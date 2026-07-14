package com.composea11yscanner.ui

import com.composea11yscanner.core.A11yScanEngine
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import com.composea11yscanner.core.rule.A11yRule
import com.composea11yscanner.rules.ScannerRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Public SDK entry point for running accessibility scans.
 *
 * Typical usage:
 * ```
 * val controller = A11yScannerController(
 *     nodeProvider  = { A11yNodeExtractor(density).extract(semanticsOwner) },
 *     screenDensity = resources.displayMetrics.density,
 * )
 *     .configure(ScannerConfig(enabledRules = ScannerRules.allRuleIds().toSet()))
 *     .withRules(MyCustomRule())
 *
 * lifecycleScope.launch {
 *     controller.startScan().collect { state -> /* update UI */ }
 * }
 * // ...
 * controller.stopScan()
 * // ...
 * controller.destroy()   // cancel the internal scope when the host is destroyed
 * ```
 *
 * @param nodeProvider Called once per [startScan] invocation to produce the node list.
 *   Must be safe to call on [Dispatchers.Default].
 * @param screenDensity DisplayMetrics.density, forwarded to density-dependent rules.
 */
class A11yScannerController(
    private val nodeProvider: () -> List<A11yNode>,
    private val screenDensity: Float,
) {
    private var config: ScannerConfig = ScannerConfig(
        enabledRules = ScannerRules.allRuleIds().toSet(),
    )
    private val customRules = mutableListOf<A11yRule>()

    // SupervisorJob: a rule exception cancels only the current scan job, not the whole scope.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scanJob: Job? = null

    // replay = 1 so late subscribers immediately receive the latest state.
    // 64 extra slots is orders of magnitude more than needed (≤ ~10 states per scan),
    // DROP_OLDEST is a last-resort safety net that preserves the most recent state.
    private val _state = MutableSharedFlow<ScannerState>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Hot [Flow] backed by the internal [MutableSharedFlow].
     *
     * Observe this to receive [ScannerState] updates without triggering a new scan.
     * Use [startScan] to both initiate a scan and receive its emissions.
     */
    val stateFlow: Flow<ScannerState> = _state.asSharedFlow()

    // --- Builder methods ---

    /**
     * Replaces the active scanner configuration.
     *
     * @param config Configuration used for future scans.
     * @return This controller for chaining.
     */
    fun configure(config: ScannerConfig): A11yScannerController = apply {
        this.config = config
    }

    /**
     * Appends [rules] to the set that will run alongside the standard rule set.
     * Custom rules are automatically added to [ScannerConfig.enabledRules] so the
     * engine never filters them out. Returns this controller for chaining.
     *
     * @param rules Custom rules to append.
     * @return This controller for chaining.
     */
    fun withRules(vararg rules: A11yRule): A11yScannerController = apply {
        customRules += rules
    }

    // --- Scan control ---

    /**
     * Cancels any in-progress scan, then starts a new one.
     *
     * Returns a [Flow] backed by a [MutableSharedFlow] — multiple collectors are safe,
     * and late subscribers receive the most recent state immediately via replay.
     *
     * Emission sequence: Scanning(0f) → Scanning(k/n) … → Scanning(1f) → Complete(result)
     * or Complete(emptyResult) if there are no enabled rules or no nodes.
     */
    fun startScan(): Flow<ScannerState> {
        stopScan()

        val standardRules = ScannerRules.buildRules(config, screenDensity)

        // Custom rule IDs must be present in enabledRules; otherwise A11yScanEngine would
        // silently filter them out during its own config-gated filtering pass.
        val effectiveConfig = if (customRules.isEmpty()) config else {
            config.copy(enabledRules = config.enabledRules + customRules.map { it.ruleId })
        }

        val engine = A11yScanEngine(
            rules = standardRules + customRules,
            config = effectiveConfig,
        )

        scanJob = scope.launch {
            val nodes = withContext(Dispatchers.Main.immediate) {
                nodeProvider()
            }
            engine.scan(nodes).collect { _state.emit(it) }
        }

        return _state.asSharedFlow()
    }

    /** Stops any active scan and emits [ScannerState.Idle]. */
    fun clearState() {
        stopScan()
        _state.tryEmit(ScannerState.Idle)
    }

    /** Cancels the current scan if one is running. The [Flow] returned by [startScan] stops emitting. */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    /** Cancels the internal [CoroutineScope]. Call when the host (Activity/Fragment/ViewModel) is destroyed. */
    fun destroy() {
        scope.cancel()
    }
}
