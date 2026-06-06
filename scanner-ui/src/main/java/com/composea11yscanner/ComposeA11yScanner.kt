package com.composea11yscanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.unit.Density
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import com.composea11yscanner.rules.ScannerRules
import com.composea11yscanner.ui.A11yIssueOverlay
import com.composea11yscanner.ui.A11yNodeExtractor
import com.composea11yscanner.ui.A11yScannerController
import com.composea11yscanner.ui.IssueDetailPanel
import com.composea11yscanner.ui.ScanSummaryBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Top-level public API for the Compose Accessibility Scanner.
 *
 * [install] attaches a transparent overlay to a [ComponentActivity] that renders the scan
 * summary bar, issue detail panel, and highlight boxes over flagged nodes. The overlay is
 * removed automatically when the activity is destroyed, so explicit [uninstall] calls are
 * only needed if the scanner should stop before destroy.
 *
 * **All three methods throw [IllegalStateException] in non-debug builds** (i.e., when
 * [ApplicationInfo.FLAG_DEBUGGABLE] is absent from the running APK). This is the correct
 * runtime check for library code; `BuildConfig.DEBUG` in a library module does not reflect
 * the consuming app's build type.
 *
 * Usage:
 * ```kotlin
 * // Activity.onCreate — after setContent { … }
 * ComposeA11yScanner.install(this)
 *
 * // Anywhere:
 * lifecycleScope.launch {
 *     ComposeA11yScanner.scan().collect { state -> /* react to ScannerState */ }
 * }
 * ```
 */
object ComposeA11yScanner {

    /**
     * Active scanner entries keyed by activity. [LinkedHashMap] preserves insertion order so
     * `entries.values.last()` always refers to the most recently installed activity.
     *
     * Must only be read/written on the main thread.
     */
    private val entries = LinkedHashMap<ComponentActivity, InstallEntry>()

    /** Set during [install] so that [scan] can perform the debug-build check without a [Context]. */
    @Volatile private var cachedAppContext: Context? = null

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Attaches the accessibility scanner overlay to [activity].
     *
     * A default [ScannerConfig] that enables all built-in rules is used when [config] is
     * omitted. Calling [install] for an activity that is already installed is a no-op.
     *
     * Must be called on the main thread, typically in `Activity.onCreate` after `setContent`.
     *
     * @throws IllegalStateException in non-debug builds.
     */
    fun install(
        activity: ComponentActivity,
        config: ScannerConfig = ScannerConfig(enabledRules = ScannerRules.allRuleIds().toSet()),
    ) {
        requireDebugBuild(activity)
        if (entries.containsKey(activity)) return

        cachedAppContext = activity.applicationContext

        val controller = A11yScannerController(
            nodeProvider = { extractNodes(activity) },
            screenDensity = activity.resources.displayMetrics.density,
        ).configure(config)

        val overlayView = ComposeView(activity).also { view ->
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity),
            )
            view.setContent {
                MaterialTheme {
                    ScannerOverlayContent(controller = controller, config = config)
                }
            }
        }
        activity.addContentView(overlayView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        entries[activity] = InstallEntry(controller, overlayView)
        activity.lifecycle.addObserver(AutoUninstallObserver(activity))
    }

    /**
     * Removes the scanner overlay from [activity] and cancels the internal coroutine scope.
     *
     * This is called automatically when the activity is destroyed. Explicit calls are only
     * needed to stop the scanner while the activity is still alive.
     *
     * Must be called on the main thread.
     *
     * @throws IllegalStateException in non-debug builds.
     */
    fun uninstall(activity: ComponentActivity) {
        requireDebugBuild(activity)
        entries.remove(activity)?.detach()
    }

    /**
     * Returns a [Flow] of [ScannerState] for the most recently installed activity.
     *
     * The backing [kotlinx.coroutines.flow.SharedFlow] has `replay = 1`, so late subscribers
     * immediately receive the current state. Returns an empty flow when no scanner is installed.
     *
     * @throws IllegalStateException in non-debug builds or if called before [install].
     */
    fun scan(): Flow<ScannerState> {
        requireDebugBuild()
        return entries.values.lastOrNull()?.controller?.stateFlow ?: emptyFlow()
    }

    // ── Debug guard ─────────────────────────────────────────────────────────────

    private fun requireDebugBuild(context: Context) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            throw IllegalStateException(
                "ComposeA11yScanner must only be used in debug builds. " +
                    "Remove all ComposeA11yScanner calls before shipping to production.",
            )
        }
    }

    // Overload for scan(), which has no Context parameter.
    private fun requireDebugBuild() {
        val ctx = cachedAppContext
            ?: throw IllegalStateException(
                "ComposeA11yScanner.scan() called before install(). " +
                    "ComposeA11yScanner may only be used in debug builds.",
            )
        requireDebugBuild(ctx)
    }

    // ── Node extraction ──────────────────────────────────────────────────────────

    // nodeProvider is invoked from Dispatchers.Default (inside A11yScannerController).
    // Reading the decor-view hierarchy and SemanticsOwner from a background thread is safe for
    // this debug tool: view-hierarchy reads do not trigger layout/draw callbacks, and the
    // Compose semantics snapshot is immutable once produced on the main thread.
    // runCatching provides a last-resort safety net in case of unexpected threading issues.
    private fun extractNodes(activity: ComponentActivity): List<A11yNode> =
        runCatching { extractNodesUnchecked(activity) }.getOrDefault(emptyList())

    private fun extractNodesUnchecked(activity: ComponentActivity): List<A11yNode> {
        val overlayView = entries[activity]?.overlayView
        val hostView = (activity.window.decorView as? ViewGroup)
            ?.findFirstAbstractComposeView(excludeView = overlayView)
            ?: return emptyList()
        val semanticsOwner = hostView.findSemanticsOwner() ?: return emptyList()
        return A11yNodeExtractor(Density(activity)).extract(semanticsOwner)
    }

    private fun AbstractComposeView.findSemanticsOwner(): SemanticsOwner? {
        val composeOwnerView = getChildAt(0) ?: return null
        return runCatching {
            composeOwnerView.javaClass
                .getMethod("getSemanticsOwner")
                .invoke(composeOwnerView) as? SemanticsOwner
        }.getOrNull()
    }

    private fun ViewGroup.findFirstAbstractComposeView(
        excludeView: View?,
    ): AbstractComposeView? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child === excludeView) continue
            if (child is AbstractComposeView) return child
            if (child is ViewGroup) {
                child.findFirstAbstractComposeView(excludeView)?.let { return it }
            }
        }
        return null
    }

    // ── Inner types ──────────────────────────────────────────────────────────────

    private class InstallEntry(
        val controller: A11yScannerController,
        val overlayView: ComposeView,
    ) {
        fun detach() {
            overlayView.disposeComposition()
            (overlayView.parent as? ViewGroup)?.removeView(overlayView)
            controller.stopScan()
            controller.destroy()
        }
    }

    private class AutoUninstallObserver(
        private val activity: ComponentActivity,
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            // entries[activity] may already be null if uninstall() was called manually first.
            entries.remove(activity)?.detach()
        }
    }
}

// ── Overlay composable ──────────────────────────────────────────────────────────

/**
 * Internal composable rendered inside the overlay [ComposeView] that [ComposeA11yScanner.install]
 * adds on top of the activity's content. Mirrors the layer structure of [A11yScannerScaffold]
 * without re-wrapping the host content.
 */
@Composable
private fun ScannerOverlayContent(
    controller: A11yScannerController,
    config: ScannerConfig,
) {
    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Idle) }
    var selectedIssue by remember { mutableStateOf<A11yIssue?>(null) }

    DisposableEffect(Unit) { onDispose { controller.stopScan() } }

    LaunchedEffect(config) {
        if (!config.autoScan) {
            controller.stopScan()
            return@LaunchedEffect
        }

        controller.configure(config).startScan().collect { state ->
            scannerState = state
            if (state is ScannerState.Scanning) selectedIssue = null
        }
    }

    val scanResult = (scannerState as? ScannerState.Complete)?.result

    Box(modifier = Modifier.fillMaxSize()) {
        A11yIssueOverlay(
            scanResult = scanResult,
            onIssueSelected = { selectedIssue = it },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = scannerState !is ScannerState.Idle,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            ScanSummaryBar(
                state = scannerState,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IssueDetailPanel(
            issue = selectedIssue,
            onDismiss = { selectedIssue = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
