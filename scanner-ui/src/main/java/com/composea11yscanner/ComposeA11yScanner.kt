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
import android.graphics.Rect as AndroidRect

/** Public facade for installing and controlling the Compose accessibility scanner. */
object ComposeA11yScanner {
    private const val TEXT_CONTRAST_RULE_ID = "text-contrast"

    /** Active installations in insertion order; accessed only from the main thread. */
    private val entries = LinkedHashMap<ComponentActivity, AutoScanCoordinator>()

    @Volatile
    private var cachedAppContext: Context? = null

    private val activeController = MutableStateFlow<A11yScannerController?>(null)

    /** Installs the scanner using automatic structural destination detection. */
    fun install(
        activity: ComponentActivity,
        config: ScannerConfig = defaultConfig(),
    ) = installInternal(activity, config, destinationKeyProvider = null)

    /** Installs the scanner using an application-provided destination identity. */
    fun install(
        activity: ComponentActivity,
        destinationKeyProvider: () -> String?,
        config: ScannerConfig = defaultConfig(),
    ) = installInternal(activity, config, destinationKeyProvider)

    private fun installInternal(
        activity: ComponentActivity,
        config: ScannerConfig,
        destinationKeyProvider: (() -> String?)?,
        automatic: Boolean,
    ) {
        requireDebugBuild(activity)
        if (entries.containsKey(activity)) return
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
        val entry = InstallEntry(
        val coordinator = AutoScanCoordinator(
            controller = controller,
            overlayView = overlayView,
            automatic = automatic,
            autoScan = config.autoScan,
            screenSnapshotProvider = snapshots::current,
        )
        entries[activity] = coordinator
        coordinator.attach()
        activeController.value = controller
        activity.lifecycle.addObserver(observer)
    }

    /** Removes the scanner and releases its resources for [activity]. */
    fun uninstall(activity: ComponentActivity) {
        requireDebugBuild(activity)
        removeEntry(activity)
    }

    /** Observes scanner state for the most recently installed activity. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun scan(): Flow<ScannerState> {
        requireDebugBuild()
        return activeController.flatMapLatest { it?.stateFlow ?: emptyFlow() }
    }

    /** Starts a scan for the most recently installed activity. */
    fun triggerScan(): Flow<ScannerState> {
        requireDebugBuild()
        return activeController.value?.startScan() ?: emptyFlow()
    }

    internal fun triggerIfEnabled(): Flow<ScannerState> {
        val context = cachedAppContext ?: return emptyFlow()
        if (!scannerLifecycle.isAllowed(context.isDebuggable())) return emptyFlow()
        return activeController.value?.startScan() ?: emptyFlow()
    }

    /** Clears the current result and schedules a stable rescan of the latest destination. */
    fun notifyScreenChanged() {
        requireDebugBuild()
        activeEntry()?.notifyScreenChanged()
    }

    /** Seeds application context when AndroidX Startup performs automatic installation. */
    internal fun initialize(context: Context) {
        cachedAppContext = context.applicationContext
    }

    private fun removeEntry(activity: ComponentActivity) {
        entries.remove(activity)?.detach()
        activeController.value = entries.values.lastOrNull()?.controller
    }

    private fun requireDebugBuild(context: Context) {
        if (!scannerLifecycle.isAllowed(context.isDebuggable())) {
            throw IllegalStateException(
                "ComposeA11yScanner is disabled by the current build/toggle policy. " +
                    "Call toggleScanner(true) on the main thread to enable it.",
            )
        }
    }

    private fun requireDebugBuild() {
        val context = cachedAppContext ?: throw IllegalStateException(
            "ComposeA11yScanner.scan() called before install(). " +
                "ComposeA11yScanner may only be used in debug builds.",
        )
        requireDebugBuild(context)
    }

    private class AutoUninstallObserver(
        private val activity: ComponentActivity,
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            removeEntry(activity)
        }
    }

    private fun defaultConfig() = ScannerConfig(
        enabledRules = ScannerRules.allRuleIds().toSet(),
    )
}
