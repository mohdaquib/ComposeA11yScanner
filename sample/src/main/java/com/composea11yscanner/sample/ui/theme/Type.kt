package com.composea11yscanner.sample.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

private val InterFont = GoogleFont("Inter")

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = 0,
)

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)

private fun TextStyle.withInter(): TextStyle = copy(fontFamily = InterFontFamily)

val ScannerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.withInter(),
        displayMedium = displayMedium.withInter(),
        displaySmall = displaySmall.withInter(),
        headlineLarge = headlineLarge.withInter(),
        headlineMedium = headlineMedium.withInter(),
        headlineSmall = headlineSmall.withInter(),
        titleLarge = titleLarge.withInter(),
        titleMedium = titleMedium.withInter(),
        titleSmall = titleSmall.withInter(),
        bodyLarge = bodyLarge.withInter(),
        bodyMedium = bodyMedium.withInter(),
        bodySmall = bodySmall.withInter(),
        labelLarge = labelLarge.withInter(),
        labelMedium = labelMedium.withInter(),
        labelSmall = labelSmall.withInter(),
    )
}
