package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseA11yRule

class TouchTargetRule(
    private val minTouchTargetDp: Int = 48,
) : BaseA11yRule() {
    override val ruleId = "touch-target-size"
    override val ruleName = "Touch Target Size"
    override val severity = A11ySeverity.Error
    override val wcagReference = "WCAG 2.5.5 Target Size (Level AA)"

    override fun check(node: A11yNode): A11yIssue? {
        if (!node.isTouchTarget) return null

        val w = node.touchTargetSize.width
        val h = node.touchTargetSize.height
        if (w >= minTouchTargetDp && h >= minTouchTargetDp) return null

        return issue(
            node = node,
            message = "Touch target is ${"%.0f".format(w)}x${"%.0f".format(h)}dp. " +
                "Minimum required is ${minTouchTargetDp}x${minTouchTargetDp}dp.",
            howToFix = "Apply Modifier.minimumInteractiveComponentSize() or add padding so the " +
                "composable reaches at least ${minTouchTargetDp}dp in both dimensions.",
        )
    }
}
