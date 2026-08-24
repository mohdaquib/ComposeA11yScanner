package com.composea11yscanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.composea11yscanner.ui.ReadinessFingerprint
import com.composea11yscanner.ui.RenderedTextContrastAnalyzer
import com.composea11yscanner.ui.ScanSummaryBar
import com.composea11yscanner.ui.ScreenFingerprint
import com.composea11yscanner.ui.calculateReadinessFingerprint
import com.composea11yscanner.ui.calculateScreenFingerprint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import android.graphics.Rect as AndroidRect

/**
 * Top-level public API for the Compose Accessibility Scanner.
 *
 * [install] attaches a transparent overlay to a [ComponentActivity] that renders the scan
 * summary bar, issue detail panel, and highlight boxes over flagged nodes. The overlay is
 * removed automatically when the activity is destroyed, so explicit [uninstall] calls are
 * only needed if the scanner should stop before destroy.
 *
 * Scanner calls throw [IllegalStateException] in non-debuggable builds unless [toggleScanner] is
 * explicitly enabled on the main thread. Debuggable builds remain enabled regardless of the toggle.
 * Checking [ApplicationInfo.FLAG_DEBUGGABLE] is correct for library code because a library module's
 * `BuildConfig.DEBUG` does not reflect the consuming app's build type.
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
    private const val SCAN_LIFECYCLE_LOG_TAG = "ComposeA11yLifecycle"

    /**
     * Active scanner entries keyed by activity. [LinkedHashMap] preserves insertion order so
     * `entries.values.last()` always refers to the most recently installed activity.
     *
     * Must only be read/written on the main thread.
     */
    private val entries = LinkedHashMap<ComponentActivity, InstallEntry>()

    /** Set during [install] so that [scan] can perform the debug-build check without a [Context]. */
    @Volatile private var cachedAppContext: Context? = null

    private val scannerLifecycle = ScannerLifecycle<ComponentActivity, ScannerConfig>(
        checkMainThread = ::checkMainThread,
        isDebuggable = { it.isDebuggable() },
        install = ::installAuto,
        removeProd = {
            entries.keys.filterNot { it.isDebuggable() }.forEach(::remove)
        },
    )

    /**
     * Enables or disables scanner installation in non-debuggable builds.
     *
     * Enabling installs the scanner on eligible resumed activities. Explicitly uninstalled
     * activities stay suppressed until their next resume. Disabling removes non-debuggable
     * overlays; debuggable builds remain enabled. This method must be called on the main thread.
     */
    @MainThread
    fun toggleScanner(enabled: Boolean) = scannerLifecycle.toggle(enabled)

    /**
     * Selected controller, preferring the latest resumed automatic activity and otherwise the
     * latest surviving manual installation.
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
     * @throws IllegalStateException in non-debuggable builds when the scanner is not enabled.
     */
    fun install(
        activity: ComponentActivity,
        config: ScannerConfig = ScannerConfig(enabledRules = ScannerRules.allRuleIds().toSet()),
    ) = installInternal(activity, config, destinationKeyProvider = null, automatic = false)

    /**
     * Installs the scanner with an explicit key for single-host Compose navigation.
     * Return the current route, pane, or other stable destination identifier from the provider.
     */
    fun install(
        activity: ComponentActivity,
        destinationKeyProvider: () -> String?,
        config: ScannerConfig = ScannerConfig(enabledRules = ScannerRules.allRuleIds().toSet()),
    ) = installInternal(activity, config, destinationKeyProvider, automatic = false)

    private fun installAuto(activity: ComponentActivity, config: ScannerConfig) =
        installInternal(activity, config, destinationKeyProvider = null, automatic = true)

    private fun installInternal(
        activity: ComponentActivity,
        config: ScannerConfig,
        destinationKeyProvider: (() -> String?)?,
        automatic: Boolean,
    ) {
        requireDebugBuild(activity)
        entries[activity]?.let { entry ->
            if (automatic) entry.automatic = true
            return
        }

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

        val observer = AutoUninstallObserver(activity)
        val entry = InstallEntry(
            controller = controller,
            overlayView = overlayView,
            automatic = automatic,
            autoScan = config.autoScan,
            screenSnapshotProvider = {
                activity.currentScreenSnapshot(destinationKeyProvider)
            },
            removeObserver = { activity.lifecycle.removeObserver(observer) },
        )
        entries[activity] = entry
        entry.attach()
        activeController.value = controller
        activity.lifecycle.addObserver(observer)
    }

    /**
     * Removes the scanner overlay from [activity] and cancels the internal coroutine scope.
     *
     * This is called automatically when the activity is destroyed. Explicit calls stop the
     * scanner while the activity remains alive and suppress reinstallation until its next resume.
     *
     * Must be called on the main thread.
     *
     * @param activity Activity whose scanner overlay should be removed.
     * @throws IllegalStateException in non-debuggable builds when the scanner is not enabled.
     */
    fun uninstall(activity: ComponentActivity) {
        requireDebugBuild(activity)
        scannerLifecycle.uninstall(activity)
        remove(activity)
    }

    private fun remove(activity: ComponentActivity) {
        entries.remove(activity)?.detach()
        routeActive()
    }

    private fun routeActive() {
        activeController.value = activeEntry()?.controller
    }

    private fun activeEntry(): InstallEntry? = selectEntry(
        resumedActivities = scannerLifecycle.resumedActivities(),
        entries = entries,
        isAutomatic = InstallEntry::automatic,
    )

    /**
     * Returns scanner state for the latest resumed automatic activity, or the latest surviving
     * manual installation when automatic routing has no active entry.
     *
     * The backing [kotlinx.coroutines.flow.SharedFlow] has `replay = 1`, so late subscribers
     * immediately receive the current state. The returned flow can be collected before automatic
     * installation; it begins forwarding state when an activity scanner becomes available.
     *
     * @throws IllegalStateException in a non-debuggable build when the scanner is not enabled, or
     * when automatic initialization is disabled and this is called before [install].
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun scan(): Flow<ScannerState> {
        requireDebugBuild()
        return activeController.flatMapLatest { controller ->
            controller?.stateFlow ?: emptyFlow()
        }
    }

    /**
     * Starts a scan for the selected automatic or manual installation and returns its state flow.
     *
     * This is useful for consumer-side triggers such as long press, shake, or debug menu actions.
     * Returns an empty flow when no scanner is installed.
     *
     * @throws IllegalStateException in a non-debuggable build when the scanner is not enabled, or
     * when automatic initialization is disabled and this is called before [install].
     */
    fun triggerScan(): Flow<ScannerState> {
        requireDebugBuild()
        return activeController.value?.startScan() ?: emptyFlow()
    }

    internal fun triggerIfEnabled(): Flow<ScannerState> {
        val context = cachedAppContext ?: return emptyFlow()
        if (!scannerLifecycle.isAllowed(context.isDebuggable())) return emptyFlow()
        return activeController.value?.startScan() ?: emptyFlow()
    }

    /** Invalidates the current result and schedules a scan for the latest destination. */
    fun notifyScreenChanged() {
        requireDebugBuild()
        activeEntry()?.notifyScreenChanged()
    }

    // ── Debug guard ─────────────────────────────────────────────────────────────

    private fun requireDebugBuild(context: Context) {
        if (!scannerLifecycle.isAllowed(context.isDebuggable())) {
            throw IllegalStateException(
                "ComposeA11yScanner requires a debuggable build or " +
                    "toggleScanner(true) on the main thread.",
            )
        }
    }

    /** Seeds the application context before activity installation when AndroidX Startup is used. */
    internal fun initialize(context: Context) {
        cachedAppContext = context.applicationContext
    }

    internal fun prepare(activity: ComponentActivity) = scannerLifecycle.prepare(activity)

    internal fun resume(activity: ComponentActivity, config: ScannerConfig) {
        scannerLifecycle.resume(activity, config)
        routeActive()
    }

    internal fun pause(activity: ComponentActivity) {
        scannerLifecycle.pause(activity)
        routeActive()
    }

    internal fun destroy(activity: ComponentActivity) {
        scannerLifecycle.destroy(activity)
        routeActive()
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "ComposeA11yScanner.toggleScanner() must be called on the main thread."
        }
    }

    private fun Context.isDebuggable(): Boolean =
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    // Overload for scan(), which has no Context parameter.
    private fun requireDebugBuild() {
        val ctx = cachedAppContext
            ?: throw IllegalStateException(
                "ComposeA11yScanner called before install(). Enable production use with " +
                    "toggleScanner(true) on the main thread.",
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
        runCatching { extractNodesUnchecked(activity) }
            .onFailure { error ->
                Log.e(COMPOSE_HOST_LOG_TAG, "Failed to extract Compose semantics", error)
            }
            .getOrDefault(emptyList())

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
        val semanticNodes = selectedHost.nodes
        return runCatching {
            RenderedTextContrastAnalyzer(selectedHost.view).analyze(semanticNodes)
        }.onFailure { error ->
            // Rendered contrast is an optional enrichment step. A bitmap capture or pixel-analysis
            // failure must not discard the semantics tree and turn the whole scan into an empty
            // 100% result; all non-visual rules can still evaluate the original nodes.
            Log.w(
                COMPOSE_HOST_LOG_TAG,
                "Rendered text contrast analysis failed; scanning semantic nodes without colors",
                error,
            )
        }.getOrDefault(semanticNodes)
    }

    private fun ComponentActivity.currentScreenSnapshot(
        destinationKeyProvider: (() -> String?)?,
    ): ScreenSnapshot? {
        val overlayView = entries[this]?.overlayView
        val decorView = window.decorView as? ViewGroup ?: return null
        val candidate = decorView
            .findBestAbstractComposeView(excludeView = overlayView, logScores = false)
            ?: return null
        // A newly attached ComposeView can expose only its root node before the destination has
        // produced semantics. Treat that state as not ready instead of reporting an empty 100% scan.
        if (candidate.nodes.none { it.depth > 0 }) return null
        val destinationKey = destinationKeyProvider?.let { provider ->
            runCatching(provider)
                .onFailure { error ->
                    Log.w(SCAN_LIFECYCLE_LOG_TAG, "Destination key provider failed", error)
                }
                .getOrNull()
        }
        return ScreenSnapshot(
            fingerprint = candidate.screenFingerprint(destinationKey),
            readiness = candidate.readinessFingerprint(),
        )
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
        logScores: Boolean = true,
    ): ComposeHostCandidate? {
        val candidates = mutableListOf<ComposeHostCandidate>()
        collectComposeHostCandidates(
            excludeView = excludeView,
            depth = 0,
            candidates = candidates,
        )
        if (logScores) {
            candidates.forEach { candidate ->
                Log.d(
                    COMPOSE_HOST_LOG_TAG,
                    "Candidate score: identity=${System.identityHashCode(candidate.view)}, " +
                        "visibleTextNodes=${candidate.visibleTextNodes}, " +
                        "visibleNodes=${candidate.visibleNodes}, depth=${candidate.depth}",
                )
            }
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
            visibleSemanticNodes = visibleNodes,
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
        var automatic: Boolean,
        private val autoScan: Boolean,
        private val screenSnapshotProvider: () -> ScreenSnapshot?,
        private val removeObserver: () -> Unit,
    ) : ViewTreeObserver.OnPreDrawListener {
        private var baselineFingerprint: ScreenFingerprint? = null
        private var completedScanId: String? = null
        private var lastCheckUptimeMillis = 0L
        private var pendingScreenFingerprint: ScreenFingerprint? = null
        private var rescanRequestedAtUptimeMillis: Long? = null
        private var pendingInitialReadiness: ReadinessFingerprint? = null
        private var initialScanRequestedAtUptimeMillis: Long? = null
        private val initialScanRunnable = object : Runnable {
            override fun run() {
                val now = android.os.SystemClock.uptimeMillis()
                val deadlineReached = initialScanRequestedAtUptimeMillis?.let { requestedAt ->
                    now - requestedAt >= MAX_INITIAL_SETTLE_MILLIS
                } ?: true
                val snapshot = screenSnapshotProvider()
                if (snapshot == null && !deadlineReached) return scheduleInitialScanCheck()

                val readiness = snapshot?.readiness
                if (readiness != null && readiness != pendingInitialReadiness && !deadlineReached) {
                    pendingInitialReadiness = readiness
                    Log.d(
                        SCAN_LIFECYCLE_LOG_TAG,
                        "Initial semantics changed; waiting for a stable sample: $readiness",
                    )
                    return scheduleInitialScanCheck()
                }

                Log.d(
                    SCAN_LIFECYCLE_LOG_TAG,
                    if (deadlineReached) {
                        "Initial settle deadline reached; starting scan"
                    } else {
                        "Initial host ready; starting scan"
                    },
                )
                pendingInitialReadiness = null
                initialScanRequestedAtUptimeMillis = null
                controller.startScan()
            }
        }
        private val rescanRunnable = object : Runnable {
            override fun run() {
                val expectedFingerprint = pendingScreenFingerprint ?: return
                val now = android.os.SystemClock.uptimeMillis()
                val deadlineReached = rescanRequestedAtUptimeMillis?.let { requestedAt ->
                    now - requestedAt >= MAX_RESCAN_SETTLE_MILLIS
                } ?: true
                val currentFingerprint = screenSnapshotProvider()?.fingerprint

                if (currentFingerprint == null && !deadlineReached) return scheduleRescan()
                if (
                    currentFingerprint != null &&
                    currentFingerprint != expectedFingerprint &&
                    !deadlineReached
                ) {
                    pendingScreenFingerprint = currentFingerprint
                    return scheduleRescan()
                }

                Log.d(
                    SCAN_LIFECYCLE_LOG_TAG,
                    if (deadlineReached) {
                        "Rescan settle deadline reached; starting scan"
                    } else {
                        "Destination stable; starting rescan"
                    },
                )
                pendingScreenFingerprint = null
                rescanRequestedAtUptimeMillis = null
                controller.startScan()
            }
        }

        fun attach() {
            overlayView.rootView.viewTreeObserver.addOnPreDrawListener(this)
            if (autoScan) requestInitialScan()
        }

        override fun onPreDraw(): Boolean {
            val now = android.os.SystemClock.uptimeMillis()
            if (now - lastCheckUptimeMillis < SCREEN_CHECK_INTERVAL_MILLIS) return true
            lastCheckUptimeMillis = now

            val complete = controller.currentState as? ScannerState.Complete
            if (complete == null) {
                completedScanId = null
                baselineFingerprint = null
                return true
            }

            val currentFingerprint = screenSnapshotProvider()?.fingerprint ?: return true
            if (completedScanId != complete.result.scanId) {
                completedScanId = complete.result.scanId
                baselineFingerprint = currentFingerprint
                Log.d(SCAN_LIFECYCLE_LOG_TAG, "Scan baseline recorded: $currentFingerprint")
                return true
            }

            if (baselineFingerprint != currentFingerprint) {
                Log.d(
                    SCAN_LIFECYCLE_LOG_TAG,
                    "Screen changed: previous=$baselineFingerprint, current=$currentFingerprint",
                )
                baselineFingerprint = null
                completedScanId = null
                controller.clearState()
                if (autoScan) {
                    pendingScreenFingerprint = currentFingerprint
                    rescanRequestedAtUptimeMillis = android.os.SystemClock.uptimeMillis()
                    scheduleRescan()
                }
            }
            return true
        }

        private fun scheduleRescan() {
            overlayView.removeCallbacks(rescanRunnable)
            overlayView.postDelayed(rescanRunnable, RESCAN_SETTLE_DELAY_MILLIS)
        }

        private fun requestInitialScan() {
            pendingInitialReadiness = screenSnapshotProvider()?.readiness
            initialScanRequestedAtUptimeMillis = android.os.SystemClock.uptimeMillis()
            scheduleInitialScanCheck()
        }

        fun notifyScreenChanged() {
            Log.d(SCAN_LIFECYCLE_LOG_TAG, "Screen change explicitly notified")
            baselineFingerprint = null
            completedScanId = null
            controller.clearState()
            if (!autoScan) return

            val currentFingerprint = screenSnapshotProvider()?.fingerprint
            if (currentFingerprint == null) {
                requestInitialScan()
            } else {
                pendingScreenFingerprint = currentFingerprint
                rescanRequestedAtUptimeMillis = android.os.SystemClock.uptimeMillis()
                scheduleRescan()
            }
        }

        private fun scheduleInitialScanCheck() {
            overlayView.removeCallbacks(initialScanRunnable)
            overlayView.postDelayed(initialScanRunnable, RESCAN_SETTLE_DELAY_MILLIS)
        }

        fun detach() {
            removeObserver()
            val observer = overlayView.rootView.viewTreeObserver
            if (observer.isAlive) observer.removeOnPreDrawListener(this)
            overlayView.removeCallbacks(initialScanRunnable)
            overlayView.removeCallbacks(rescanRunnable)
            pendingScreenFingerprint = null
            rescanRequestedAtUptimeMillis = null
            pendingInitialReadiness = null
            initialScanRequestedAtUptimeMillis = null
            overlayView.disposeComposition()
            (overlayView.parent as? ViewGroup)?.removeView(overlayView)
            controller.stopScan()
            controller.destroy()
        }

        private companion object {
            const val SCREEN_CHECK_INTERVAL_MILLIS = 500L
            const val RESCAN_SETTLE_DELAY_MILLIS = 300L
            const val MAX_RESCAN_SETTLE_MILLIS = 1_500L
            const val MAX_INITIAL_SETTLE_MILLIS = 1_500L
        }
    }

    private data class ComposeHostCandidate(
        val view: AbstractComposeView,
        val nodes: List<A11yNode>,
        val depth: Int,
        val visibleSemanticNodes: List<A11yNode>,
    ) {
        val visibleTextNodes: Int
            get() = visibleSemanticNodes.count { it.composableName == "Text" }

        val visibleNodes: Int
            get() = visibleSemanticNodes.size

        fun screenFingerprint(destinationKey: String?): ScreenFingerprint {
            return calculateScreenFingerprint(
                hostIdentity = System.identityHashCode(view),
                nodes = nodes,
                destinationKey = destinationKey,
            )
        }

        fun readinessFingerprint(): ReadinessFingerprint {
            return calculateReadinessFingerprint(
                hostIdentity = System.identityHashCode(view),
                visibleNodes = visibleSemanticNodes,
            )
        }
    }

    private data class ScreenSnapshot(
        val fingerprint: ScreenFingerprint,
        val readiness: ReadinessFingerprint,
    )

    private class AutoUninstallObserver(
        private val activity: ComponentActivity,
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            // entries[activity] may already be null if uninstall() was called manually first.
            remove(activity)
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
            if (state !is ScannerState.Complete) selectedIssues = emptyList()
        }
    }

    LaunchedEffect(config) {
        controller.configure(config)
        if (!config.autoScan) {
            controller.clearState()
        }
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
                .statusBarsPadding()
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
