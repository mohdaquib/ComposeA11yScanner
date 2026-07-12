package com.composea11yscanner.sample.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val ScannerBackground = Color(0xFF0A0A0F)
val ScannerSurface = Color(0xFF12121A)
val ScannerSurfaceVariant = Color(0xFF1C1C28)
val ScannerPrimary = Color(0xFF6C63FF)
val ScannerSecondary = Color(0xFF00D4AA)
val ScannerError = Color(0xFFFF4D6A)
val ScannerWarning = Color(0xFFFFB547)
val ScannerInfo = Color(0xFF4DA6FF)
val ScannerSuccess = ScannerSecondary

@Immutable
data class ScannerPalette(
    val warning: Color,
    val info: Color,
    val success: Color,
)
