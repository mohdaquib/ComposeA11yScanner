package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseScanRule
import kotlin.math.roundToInt

/**
 * Flags focus traversal that jumps upward relative to visual top-to-bottom order.
 *
 * @param screenDensity Display density from DisplayMetrics.density.
 *   Used to convert [jumpThresholdDp] to pixels for comparison against [A11yNode.bounds].
 * @param jumpThresholdDp Upward movement that triggers a violation (default 8dp).
 */
class FocusOrderRule(
    private val screenDensity: Float,
    private val jumpThresholdDp: Float = 8f,
) : BaseScanRule() {

    /** Stable id for the focus order rule. */
    override val ruleId = "focus-order"

    /** Human-readable rule name. */
    override val ruleName = "Focus Order"

    /** Severity assigned to focus order jumps. */
    override val severity = A11ySeverity.Error

    /** WCAG criterion associated with focus order. */
    override val wcagReference = "WCAG 2.4.3 Focus Order (Level A)"

    private val jumpThresholdPx: Int = (jumpThresholdDp * screenDensity).roundToInt()

    /** Evaluates all focusable nodes in semantics order. */
    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> =
        effectiveFocusNodes(nodes)
            .zipWithNext { prev, curr ->
                val jumpedUpward = curr.bounds.top < prev.bounds.top - jumpThresholdPx
                if (!jumpedUpward) return@zipWithNext null

                val prevTopDp = (prev.bounds.top / screenDensity).roundToInt()
                val currTopDp = (curr.bounds.top / screenDensity).roundToInt()

                issue(
                    node = curr,
                    message = "Focus jumps upward from ${prevTopDp}dp to ${currTopDp}dp. " +
                        "Screen readers will announce this element out of visual reading order.",
                    howToFix = "Reorder composables in the source so focus flows top-to-bottom, " +
                        "left-to-right, or apply Modifier.semantics { traversalIndex = n } " +
                        "to explicitly control the focus traversal sequence.",
                )
            }
            .filterNotNull()

    private fun effectiveFocusNodes(nodes: List<A11yNode>): List<A11yNode> {
        val result = mutableListOf<A11yNode>()

        nodes
            .filter { it.isFocusable && !it.isMergedDescendant }
            .forEach { node ->
                val hasFocusableAncestor = result.any { ancestor ->
                    ancestor.depth < node.depth && ancestor.bounds.contains(node.bounds)
                }
                if (!hasFocusableAncestor) result += node
            }

        return result
    }

    private fun com.composea11yscanner.core.model.Rect.contains(other: com.composea11yscanner.core.model.Rect): Boolean =
        left <= other.left &&
            top <= other.top &&
            right >= other.right &&
            bottom >= other.bottom
}
