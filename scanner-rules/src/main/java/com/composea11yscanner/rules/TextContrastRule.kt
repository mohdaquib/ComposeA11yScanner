package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Color
import com.composea11yscanner.core.rule.BaseA11yRule
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Reports measured semantic text whose rendered contrast is below [minimumRatio].
 * Nodes without a confident rendered-pixel measurement are intentionally skipped.
 */
class TextContrastRule(
    private val minimumRatio: Float = 4.5f,
) : BaseA11yRule() {

    override val ruleId = "text-contrast"
    override val ruleName = "Text Contrast"
    override val severity = A11ySeverity.Warning
    override val wcagReference = "WCAG 1.4.3 Contrast Minimum (Level AA)"

    override fun check(node: A11yNode): A11yIssue? {
        if (node.composableName != "Text") return null
        if (!node.isEnabled) return null
        val foreground = node.textColor ?: return null
        if (foreground == Color.Unspecified || node.backgroundColors.isEmpty()) return null

        val measuredRatio = node.backgroundColors
            .asSequence()
            .filter { it != Color.Unspecified }
            .map { background -> contrastRatio(foreground, background) }
            .minOrNull()
            ?: return null
        if (measuredRatio >= minimumRatio) return null

        val measured = String.format(Locale.US, "%.2f", measuredRatio)
        val required = String.format(Locale.US, "%.1f", minimumRatio)
        return issue(
            node = node,
            message = "Rendered text contrast is $measured:1; at least $required:1 is required.",
            howToFix = "Use a darker foreground on a light background or a lighter foreground " +
                "on a dark background. Recheck every enabled theme and state.",
        )
    }
}

/** Calculates the WCAG contrast ratio for two opaque, rendered sRGB colors. */
internal fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = relativeLuminance(first)
    val secondLuminance = relativeLuminance(second)
    return ((max(firstLuminance, secondLuminance) + 0.05) /
        (min(firstLuminance, secondLuminance) + 0.05)).toFloat()
}

private fun relativeLuminance(color: Color): Double {
    val argb = color.toArgb()
    val red = ((argb ushr 16) and 0xFF) / 255.0
    val green = ((argb ushr 8) and 0xFF) / 255.0
    val blue = (argb and 0xFF) / 255.0
    return 0.2126 * red.linearize() + 0.7152 * green.linearize() + 0.0722 * blue.linearize()
}

private fun Double.linearize(): Double =
    if (this <= 0.04045) this / 12.92 else ((this + 0.055) / 1.055).pow(2.4)
