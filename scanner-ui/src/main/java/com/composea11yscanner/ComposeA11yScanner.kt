package com.composea11yscanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Rect as AndroidRect
import android.util.Log
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
import com.composea11yscanner.ui.RenderedTextContrastAnalyzer
import com.composea11yscanner.ui.ScanSummaryBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

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

    private const val COMPOSE_HOST_LOG_TAG = "ComposeA11yHosts"

    /**
     * Active scanner entries keyed by activity. [LinkedHashMap] preserves insertion order so
     * `entries.values.last()` always refers to the most recently installed activity.
     *
     * Must only be read/written on the main thread.
     */
    private val entries = LinkedHashMap<ComponentActivity, InstallEntry>()

    /** Set during [install] so that [scan] can perform the debug-build check without a [Context]. */
    @Volatile private var cachedAppContext: Context? = null

    /**
     * Controller for the most recently installed activity. Keeping this as state allows callers
     * to subscribe before the automatic activity-resume installation has completed.
     */
    private val activeController = MutableStateFlow<A11yScannerController?>(null)

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Attaches the accessibility scanner overlay to [activity].
     *
     * A default [ScannerConfig] that enables all built-in rules is used when [config] is
     * omitted. Calling [install] for an activity that is already installed is a no-op.
     *
     * Must be called on the main thread, typically in `Activity.onCreate` after `setContent`.
     *
     * @param activity Activity that should receive the scanner overlay.
     * @param config Scanner configuration applied to this install.
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
        activeController.value = controller
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
     * @param activity Activity whose scanner overlay should be removed.
     * @throws IllegalStateException in non-debug builds.
     */
    fun uninstall(activity: ComponentActivity) {
        requireDebugBuild(activity)
        entries.remove(activity)?.detach()
        activeController.value = entries.values.lastOrNull()?.controller
    }

    /**
     * Returns a [Flow] of [ScannerState] for the most recently installed activity.
     *
     * The backing [kotlinx.coroutines.flow.SharedFlow] has `replay = 1`, so late subscribers
     * immediately receive the current state. The returned flow can be collected before automatic
     * installation; it begins forwarding state when an activity scanner becomes available.
     *
     * @throws IllegalStateException in non-debug builds, or when automatic initialization is
     * disabled and this is called before [install].
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun scan(): Flow<ScannerState> {
        requireDebugBuild()
        return activeController.flatMapLatest { controller ->
            controller?.stateFlow ?: emptyFlow()
        }
    }

    /**
     * Starts a scan for the most recently installed activity and returns the shared state flow.
     *
     * This is useful for consumer-side triggers such as long press, shake, or debug menu actions.
     * Returns an empty flow when no scanner is installed.
     *
     * @throws IllegalStateException in non-debug builds, or when automatic initialization is
     * disabled and this is called before [install].
     */
    fun triggerScan(): Flow<ScannerState> {
        requireDebugBuild()
        return entries.values.lastOrNull()?.controller?.startScan() ?: emptyFlow()
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

    /** Seeds the application context before activity installation when AndroidX Startup is used. */
    internal fun initialize(context: Context) {
        cachedAppContext = context.applicationContext
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
        val decorView = activity.window.decorView as? ViewGroup ?: return emptyList()
        decorView.logAbstractComposeViews(excludeView = overlayView)
        val selectedHost = decorView
            .findBestAbstractComposeView(excludeView = overlayView)
            ?: return emptyList()
        Log.d(
            COMPOSE_HOST_LOG_TAG,
            "Selected host: ${selectedHost.view.composeHostDescription(isExcluded = false)}, " +
                "visibleTextNodes=${selectedHost.visibleTextNodes}, " +
                "visibleNodes=${selectedHost.visibleNodes}, depth=${selectedHost.depth}",
        )
        return RenderedTextContrastAnalyzer(selectedHost.view).analyze(selectedHost.nodes)
    }

    private fun AbstractComposeView.findSemanticsOwner(): SemanticsOwner? {
        val composeOwnerView = getChildAt(0) ?: return null
        return runCatching {
            composeOwnerView.javaClass
                .getMethod("getSemanticsOwner")
                .invoke(composeOwnerView) as? SemanticsOwner
        }.getOrNull()
    }

    private fun ViewGroup.findBestAbstractComposeView(
        excludeView: View?,
    ): ComposeHostCandidate? {
        val candidates = mutableListOf<ComposeHostCandidate>()
        collectComposeHostCandidates(
            excludeView = excludeView,
            depth = 0,
            candidates = candidates,
        )
        candidates.forEach { candidate ->
            Log.d(
                COMPOSE_HOST_LOG_TAG,
                "Candidate score: identity=${System.identityHashCode(candidate.view)}, " +
                    "visibleTextNodes=${candidate.visibleTextNodes}, " +
                    "visibleNodes=${candidate.visibleNodes}, depth=${candidate.depth}",
            )
        }
        return candidates.maxWithOrNull(
            compareBy<ComposeHostCandidate> { it.visibleTextNodes }
                .thenBy { it.visibleNodes }
                .thenBy { it.depth },
        )
    }

    private fun ViewGroup.collectComposeHostCandidates(
        excludeView: View?,
        depth: Int,
        candidates: MutableList<ComposeHostCandidate>,
    ) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is AbstractComposeView && child !== excludeView && child.isViableComposeHost()) {
                child.toComposeHostCandidate(depth + 1)?.let(candidates::add)
            }
            if (child is ViewGroup && child !== excludeView) {
                child.collectComposeHostCandidates(
                    excludeView = excludeView,
                    depth = depth + 1,
                    candidates = candidates,
                )
            }
        }
    }

    private fun AbstractComposeView.isViableComposeHost(): Boolean =
        visibility == View.VISIBLE &&
            isShown &&
            isAttachedToWindow &&
            isLaidOut &&
            alpha > 0f &&
            width > 0 &&
            height > 0

    private fun AbstractComposeView.toComposeHostCandidate(depth: Int): ComposeHostCandidate? {
        val owner = findSemanticsOwner() ?: return null
        val nodes = runCatching { A11yNodeExtractor().extract(owner) }.getOrNull() ?: return null
        val visibleNodes = nodes.filter { node -> node.bounds.intersectsViewport(width, height) }
        return ComposeHostCandidate(
            view = this,
            nodes = nodes,
            depth = depth,
            visibleTextNodes = visibleNodes.count { it.composableName == "Text" },
            visibleNodes = visibleNodes.size,
        )
    }

    private fun ViewGroup.logAbstractComposeViews(
        excludeView: View?,
        path: String = javaClass.simpleName,
    ) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val childPath = "$path/$index:${child.javaClass.simpleName}"
            if (child is AbstractComposeView) {
                Log.d(
                    COMPOSE_HOST_LOG_TAG,
                    "Candidate path=$childPath, " +
                        child.composeHostDescription(isExcluded = child === excludeView),
                )
            }
            if (child is ViewGroup) {
                child.logAbstractComposeViews(
                    excludeView = excludeView,
                    path = childPath,
                )
            }
        }
    }

    private fun AbstractComposeView.composeHostDescription(isExcluded: Boolean): String {
        val screenLocation = IntArray(2)
        getLocationOnScreen(screenLocation)
        val visibleRect = AndroidRect()
        val hasVisibleRect = getGlobalVisibleRect(visibleRect)
        return "identity=${System.identityHashCode(this)}, " +
            "excludedOverlay=$isExcluded, " +
            "visibility=${visibility.asVisibilityName()}, " +
            "shown=$isShown, attached=$isAttachedToWindow, laidOut=$isLaidOut, " +
            "alpha=$alpha, size=${width}x$height, " +
            "position=($x,$y), translation=($translationX,$translationY), " +
            "screen=(${screenLocation[0]},${screenLocation[1]}), " +
            "hasVisibleRect=$hasVisibleRect, visibleRect=$visibleRect, " +
            "childCount=$childCount"
    }

    private fun Int.asVisibilityName(): String = when (this) {
        View.VISIBLE -> "VISIBLE"
        View.INVISIBLE -> "INVISIBLE"
        View.GONE -> "GONE"
        else -> toString()
    }

    private fun com.composea11yscanner.core.model.Rect.intersectsViewport(
        viewportWidth: Int,
        viewportHeight: Int,
    ): Boolean =
        !isEmpty() &&
            right > 0 &&
            bottom > 0 &&
            left < viewportWidth &&
            top < viewportHeight

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

    private data class ComposeHostCandidate(
        val view: AbstractComposeView,
        val nodes: List<A11yNode>,
        val depth: Int,
        val visibleTextNodes: Int,
        val visibleNodes: Int,
    )

    private class AutoUninstallObserver(
        private val activity: ComponentActivity,
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            // entries[activity] may already be null if uninstall() was called manually first.
            entries.remove(activity)?.detach()
            activeController.value = entries.values.lastOrNull()?.controller
        }
    }
}

// ── Overlay composable ──────────────────────────────────────────────────────────

/**
 * Internal composable rendered inside the overlay [ComposeView] that [ComposeA11yScanner.install]
 * adds on top of the activity's content. Mirrors the layer structure of
 * [com.composea11yscanner.ui.A11yScannerScaffold]
 * without re-wrapping the host content.
 */
@Composable
private fun ScannerOverlayContent(
    controller: A11yScannerController,
    config: ScannerConfig,
) {
    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Idle) }
    var selectedIssues by remember { mutableStateOf(emptyList<A11yIssue>()) }

    DisposableEffect(Unit) { onDispose { controller.stopScan() } }

    LaunchedEffect(Unit) {
        controller.stateFlow.collect { state ->
            scannerState = state
            if (state is ScannerState.Scanning) selectedIssues = emptyList()
        }
    }

    LaunchedEffect(config) {
        controller.configure(config)
        if (!config.autoScan) {
            controller.stopScan()
            return@LaunchedEffect
        }
        controller.startScan()
    }

    val scanResult = (scannerState as? ScannerState.Complete)?.result

    Box(modifier = Modifier.fillMaxSize()) {
        A11yIssueOverlay(
            scanResult = scanResult,
            onIssuesSelected = { selectedIssues = it },
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
            issues = selectedIssues,
            onDismiss = { selectedIssues = emptyList() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
