package com.composea11yscanner.core.model

/**
 * Runtime configuration used by the scanner engine and UI integration.
 *
 * @property enabledRules Rule ids that should be evaluated.
 * @property minTouchTargetDp Minimum touch target size in dp.
 * @property minContrastRatio Minimum text contrast ratio used by contrast-related rules.
 * @property debugOverlay Whether scanner UI should display issue overlays.
 * @property autoScan Whether scanning should start automatically when the scanner attaches.
 */
data class ScannerConfig(
    val enabledRules: Set<String>,
    val minTouchTargetDp: Int = 48,
    val minContrastRatio: Float = 4.5f,
    val debugOverlay: Boolean = true,
    val autoScan: Boolean = true,
)
