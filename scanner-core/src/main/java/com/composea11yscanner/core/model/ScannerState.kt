package com.composea11yscanner.core.model

/** State emitted while a scan is idle, running, complete, or failed. */
sealed interface ScannerState {
    /** No scan is currently active and no result is displayed. */
    data object Idle : ScannerState

    /**
     * Scan progress state.
     *
     * @property progress Completion fraction from 0.0 to 1.0.
     */
    data class Scanning(val progress: Float) : ScannerState

    /**
     * Successful scan completion.
     *
     * @property result Full scan result.
     */
    data class Complete(val result: ScanResult) : ScannerState

    /**
     * Scan failure.
     *
     * @property message Human-readable error message.
     */
    data class Error(val message: String) : ScannerState
}
