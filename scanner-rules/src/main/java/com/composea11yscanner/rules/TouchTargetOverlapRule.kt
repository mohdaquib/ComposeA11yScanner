package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.rule.BaseScanRule

/** Reports interactive nodes whose effective pointer target overlaps another target. */
class TouchTargetOverlapRule : BaseScanRule() {

    override val ruleId = "touch-target-overlap"
    override val ruleName = "Touch Target Overlap"
    override val severity = A11ySeverity.Warning
    override val wcagReference: String? = null

    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> {
        val targets = nodes.filter { node ->
            node.isTouchTarget &&
                !node.isMergedDescendant &&
                node.effectiveTouchBounds?.isEmpty() == false
        }.distinctBy { it.logicalIdentity() }
        val overlapsByNodeId = mutableMapOf<String, MutableSet<String>>()

        targets.forEachIndexed { index, first ->
            for (secondIndex in index + 1 until targets.size) {
                val second = targets[secondIndex]
                if (!first.effectiveTouchBounds!!.overlaps(second.effectiveTouchBounds!!)) continue
                // Compose may expand a small control's touch bounds beyond its visual bounds to
                // meet the minimum target size. Adjacent controls can therefore have intersecting
                // effective rectangles even though they remain distinct hit targets. Only report
                // overlap when the actual layout bounds intersect too.
                if (!first.bounds.overlaps(second.bounds)) continue

                overlapsByNodeId.getOrPut(first.nodeId) { mutableSetOf() }.add(second.nodeId)
                overlapsByNodeId.getOrPut(second.nodeId) { mutableSetOf() }.add(first.nodeId)
            }
        }

        return targets.mapNotNull { node ->
            val overlappingIds = overlapsByNodeId[node.nodeId] ?: return@mapNotNull null
            val targetWord = if (overlappingIds.size == 1) "target" else "targets"
            issue(
                node = node,
                message = "Effective touch target overlaps ${overlappingIds.size} other $targetWord. " +
                    "Overlapping hit regions can make the intended control ambiguous.",
                howToFix = "Increase spacing between controls, enlarge their layout bounds, or " +
                    "restructure the layout so effective touch regions do not overlap.",
            )
        }
    }
}

private fun A11yNode.logicalIdentity(): List<Any?> = listOf(
    composableName,
    bounds,
    effectiveTouchBounds,
    contentDescription,
    role,
)

private fun Rect.overlaps(other: Rect): Boolean =
    left < other.right &&
        right > other.left &&
        top < other.bottom &&
        bottom > other.top
