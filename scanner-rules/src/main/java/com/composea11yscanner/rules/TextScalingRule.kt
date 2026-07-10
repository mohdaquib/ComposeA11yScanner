package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseScanRule
import kotlin.math.roundToInt

/**
 * Flags text that may clip when simulated at a larger font scale.
 *
 * @param screenDensity Display density from DisplayMetrics.density.
 * @param scaleFactor Font scale to simulate (default 1.3x).
 */
class TextScalingRule(
    private val screenDensity: Float,
    private val scaleFactor: Float = 1.3f,
) : BaseScanRule() {

    /** Stable id for the text scaling rule. */
    override val ruleId = "text-scaling"

    /** Human-readable rule name. */
    override val ruleName = "Text Scaling"

    /** Severity assigned to possible text clipping. */
    override val severity = A11ySeverity.Warning

    /** WCAG criterion associated with resized text. */
    override val wcagReference = "WCAG 1.4.4 Resize Text (Level AA)"

    /** Evaluates text nodes against their parent bounds at the configured scale factor. */
    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> =
        nodes
            .filter {
                it.composableName.contains("Text", ignoreCase = true) &&
                    !it.isMergedDescendant
            }
            .mapNotNull { textNode ->
                val parent = findParent(textNode, nodes) ?: return@mapNotNull null
                if (!parent.isTouchTarget) return@mapNotNull null

                val scaledHeight = textNode.bounds.height * scaleFactor
                val overflowPx = (textNode.bounds.top + scaledHeight) - parent.bounds.bottom
                if (overflowPx <= 0f) return@mapNotNull null

                val originalDp = (textNode.bounds.height / screenDensity).roundToInt()
                val scaledDp = (scaledHeight / screenDensity).roundToInt()
                val overflowDp = (overflowPx / screenDensity).roundToInt()

                issue(
                    node = textNode,
                    message = "'${textNode.composableName}' may clip at ${scaleFactor}x font scale: " +
                        "height grows from ${originalDp}dp to ${scaledDp}dp, " +
                        "overflowing its container by ${overflowDp}dp.",
                    howToFix = "Remove fixed heights from the parent container, use " +
                        "wrapContentHeight(), or wrap the content in a verticalScroll " +
                        "so text can reflow without clipping.",
                )
            }

    // Tightest enclosing node at depth - 1 (smallest area that still fully contains the text node).
    private fun findParent(node: A11yNode, allNodes: List<A11yNode>): A11yNode? =
        allNodes
            .filter { candidate ->
                candidate.depth == node.depth - 1 &&
                    candidate.bounds.left <= node.bounds.left &&
                    candidate.bounds.top <= node.bounds.top &&
                    candidate.bounds.right >= node.bounds.right &&
                    candidate.bounds.bottom >= node.bounds.bottom
            }
            .minByOrNull { it.bounds.width * it.bounds.height }
}
