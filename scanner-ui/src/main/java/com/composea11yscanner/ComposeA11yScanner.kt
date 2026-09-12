package com.composea11yscanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import com.composea11yscanner.rules.ScannerRules
import com.composea11yscanner.ui.A11yScannerController
import com.composea11yscanner.ui.AutoScanCoordinator
import com.composea11yscanner.ui.ComposeHostFinder
import com.composea11yscanner.ui.ComposeNodeProvider
import com.composea11yscanner.ui.ScannerOverlayContent
import com.composea11yscanner.ui.ScreenSnapshotProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import android.os.Looper
import androidx.annotation.MainThread

/**
 * Top-level public API for the Compose Accessibility Scanner.
 *
 * [install] attaches a transparent overlay to a [ComponentActivity] that renders the scan
 * summary bar, issue detail panel, and highlight boxes over flagged nodes. The overlay is
 * removed automatically when the activity is destroyed, so explicit [uninstall] calls are
 * only needed if the scanner should stop before destroy.
 *
 * Scanner calls are allowed by default in debuggable builds and denied by default in
 * non-debuggable builds. [toggleScanner] explicitly overrides either default: `false` disables the
 * scanner in every build, while `true` enables it in every build.
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

    private const val TEXT_CONTRAST_RULE_ID = "text-contrast"


    /**
     * Active scanner entries keyed by activity. [LinkedHashMap] preserves insertion order so
     * `entries.values.last()` always refers to the most recently installed activity.
     *
     * Must only be read/written on the main thread.
     */
    private val entries = LinkedHashMap<ComponentActivity, AutoScanCoordinator>()

    /** Set during [install] so that [scan] can perform the permission check without a [Context]. */
    @Volatile private var cachedAppContext: Context? = null

    private val scannerLifecycle = ScannerLifecycle<ComponentActivity, ScannerConfig>(
        checkMainThread = ::checkMainThread,
        isDebuggable = { it.isDebuggable() },
        install = ::installAuto,
        removeProd = {
            entries.keys.toList().forEach(::remove)
        },
    )

    /**
     * Overrides the default scanner availability for every build.
     *
     * Before the first call, debuggable builds are enabled and non-debuggable builds are denied.
     * Enabling installs the scanner on eligible resumed activities. Explicitly uninstalled
     * activities stay suppressed until their next resume. Disabling removes every scanner overlay
     * and prevents reinstallation until enabled again. AndroidX Startup callbacks remain registered
     * so resumed activities can be tracked for immediate re-enabling; remove the initializer from
     * the app manifest when no scanner startup or lifecycle work is allowed. This method must be
     * called on the main thread.
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
     * @throws IllegalStateException when the scanner is denied by the current build/toggle policy.
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

        var overlayView: ComposeView? = null
        val hostFinder = ComposeHostFinder()
        val nodes = ComposeNodeProvider(activity, { overlayView }, hostFinder)
        val snapshots = ScreenSnapshotProvider(
            activity = activity,
            overlayViewProvider = { overlayView },
            destinationKeyProvider = destinationKeyProvider,
            hostFinder = hostFinder,
        )
        val controller = A11yScannerController(
            nodeProvider = nodes::mergedNodes,
            screenDensity = activity.resources.displayMetrics.density,
            ruleNodeOverridesProvider = {
                if (TEXT_CONTRAST_RULE_ID in config.enabledRules) {
                    mapOf(TEXT_CONTRAST_RULE_ID to nodes.contrastNodes())
                } else {
                    emptyMap()
                }
            },
        ).configure(config)

        overlayView = ComposeView(activity).also { view ->
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity),
            )
            view.setContent {
                MaterialTheme { ScannerOverlayContent(controller, config) }
            }
        }
        activity.addContentView(overlayView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val observer = AutoUninstallObserver(activity)
        val coordinator = AutoScanCoordinator(
            controller = controller,
            overlayView = overlayView,
            automatic = automatic,
            autoScan = config.autoScan,
            screenSnapshotProvider = snapshots::current,
            removeObserver = { activity.lifecycle.removeObserver(observer) },
        )
        entries[activity] = coordinator
        coordinator.attach()
        routeActive()
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
     * This remains safe and idempotent after [toggleScanner] disables the scanner.
     *
     * @param activity Activity whose scanner overlay should be removed.
     */
    fun uninstall(activity: ComponentActivity) {
        checkMainThread()
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

    private fun activeEntry(): AutoScanCoordinator? = selectEntry(
        resumedActivities = scannerLifecycle.resumedActivities(),
        entries = entries,
        isAutomatic = AutoScanCoordinator::automatic,
    )

    /**
     * Returns scanner state for the latest resumed automatic activity, or the latest surviving
     * manual installation when automatic routing has no active entry.
     *
     * The backing [kotlinx.coroutines.flow.SharedFlow] has `replay = 1`, so late subscribers
     * immediately receive the current state. The returned flow can be collected before automatic
     * installation; it begins forwarding state when an activity scanner becomes available.
     *
     * @throws IllegalStateException when the scanner is denied by the current build/toggle policy,
     * or when automatic initialization is disabled and this is called before [install].
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
     * @throws IllegalStateException when the scanner is denied by the current build/toggle policy,
     * or when automatic initialization is disabled and this is called before [install].
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
                "ComposeA11yScanner is disabled by the current build/toggle policy. " +
                    "Call toggleScanner(true) on the main thread to enable it.",
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

    internal fun resetForTests() {
        entries.keys.toList().forEach(::remove)
        scannerLifecycle.reset()
        cachedAppContext = null
        activeController.value = null
    }

    internal fun installedActivitiesForTests(): List<ComponentActivity> = entries.keys.toList()

    internal fun activeActivityForTests(): ComponentActivity? {
        val active = activeEntry() ?: return null
        return entries.entries.lastOrNull { it.value === active }?.key
    }

    internal fun controllerForTests(activity: ComponentActivity): A11yScannerController? =
        entries[activity]?.controller

    internal fun overlayForTests(activity: ComponentActivity): ComposeView? =
        entries[activity]?.overlayView

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

    private class AutoUninstallObserver(
        private val activity: ComponentActivity,
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            remove(activity)
        }
    }
}