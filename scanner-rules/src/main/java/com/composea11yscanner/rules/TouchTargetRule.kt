package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseA11yRule

/**
 * Flags clickable nodes whose measured touch target is smaller than the configured minimum.
 *
 * @param minTouchTargetDp Minimum accepted width and height in dp.
 */
class TouchTargetRule(
    private val minTouchTargetDp: Int = 48,
) : BaseA11yRule() {
    /** Stable id for the touch target size rule. */
    override val ruleId = "touch-target-size"

    /** Human-readable rule name. */
    override val ruleName = "Touch Target Size"

    /** Severity assigned to undersized touch targets. */
    override val severity = A11ySeverity.Error

    /** WCAG criterion associated with target size. */
    override val wcagReference = "WCAG 2.5.5 Target Size (Level AA)"

    /** Evaluates a single node for touch target dimensions. */
    override fun check(node: A11yNode): A11yIssue? {
        if (node.isMergedDescendant) return null
        if (!node.isTouchTarget) return null

        val w = node.touchTargetSize.width
        val h = node.touchTargetSize.height
        // Pixel bounds converted back to dp can land infinitesimally below an exact dp value
        // (for example, a 48 dp target may be reported as 47.999996 dp).
        if (
            w + MeasurementToleranceDp >= minTouchTargetDp &&
            h + MeasurementToleranceDp >= minTouchTargetDp
        ) return null

        return issue(
            node = node,
            message = "Touch target is ${"%.0f".format(w)}x${"%.0f".format(h)}dp. " +
                "Minimum required is ${minTouchTargetDp}x${minTouchTargetDp}dp.",
            howToFix = "Apply Modifier.minimumInteractiveComponentSize() or add padding so the " +
                "composable reaches at least ${minTouchTargetDp}dp in both dimensions.",
        )
    }

    private companion object {
        const val MeasurementToleranceDp = 0.01f
    }
}
