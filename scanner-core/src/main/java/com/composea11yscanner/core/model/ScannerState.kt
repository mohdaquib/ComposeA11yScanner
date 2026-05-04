package com.composea11yscanner.core.model

sealed interface ScannerState {
    data object Idle : ScannerState

    data class Scanning(val progress: Float) : ScannerState

    data class Complete(val result: ScanResult) : ScannerState

    data class Error(val message: String) : ScannerState
}
