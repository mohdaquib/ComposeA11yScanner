package com.composea11yscanner.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val ScannerDarkColorScheme = darkColorScheme(
    primary = ScannerPrimary,
    onPrimary = Color.White,
    primaryContainer = ScannerPrimary.copy(alpha = 0.22f),
    onPrimaryContainer = Color.White,
    secondary = ScannerSecondary,
    onSecondary = ScannerBackground,
    secondaryContainer = ScannerSecondary.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    tertiary = ScannerInfo,
    onTertiary = ScannerBackground,
    error = ScannerError,
    onError = Color.White,
    errorContainer = ScannerError.copy(alpha = 0.2f),
    onErrorContainer = Color.White,
    background = ScannerBackground,
    onBackground = Color(0xFFF4F4FA),
    surface = ScannerSurface,
    onSurface = Color(0xFFF4F4FA),
    surfaceVariant = ScannerSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7C7D6),
    outline = Color(0xFF6D6D80),
    outlineVariant = Color(0xFF343447),
    surfaceContainerLowest = ScannerBackground,
    surfaceContainerLow = ScannerSurface,
    surfaceContainer = ScannerSurfaceVariant,
    surfaceContainerHigh = Color(0xFF242433),
    surfaceContainerHighest = Color(0xFF2D2D3D),
)

private val ScannerSemanticPalette = ScannerPalette(
    warning = ScannerWarning,
    info = ScannerInfo,
    success = ScannerSuccess,
)

private val LocalScannerPalette = staticCompositionLocalOf { ScannerSemanticPalette }

object ScannerTheme {
    val palette: ScannerPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalScannerPalette.current
}

@Composable
fun ScannerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScannerPalette provides ScannerSemanticPalette) {
        MaterialTheme(
            colorScheme = ScannerDarkColorScheme,
            typography = ScannerTypography,
            shapes = ScannerShapes,
            content = content,
        )
    }
}
