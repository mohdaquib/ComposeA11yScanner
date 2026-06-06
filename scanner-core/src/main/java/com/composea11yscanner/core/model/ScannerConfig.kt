package com.composea11yscanner.core.model

data class ScannerConfig(
    val enabledRules: Set<String>,
    val minTouchTargetDp: Int = 48,
    val minContrastRatio: Float = 4.5f,
    val debugOverlay: Boolean = true,
    val autoScan: Boolean = true,
)
