package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseScanRule

/** Flags independently exposed nodes that reuse a description in the same semantic scope. */
class DuplicateContentDescriptionRule : BaseScanRule() {

    /** Stable id for the duplicate content description rule. */
    override val ruleId = "duplicate-content-description"

    /** Human-readable rule name. */
    override val ruleName = "Duplicate Content Description"

    /** Severity assigned to duplicate labels. */
    override val severity = A11ySeverity.Warning

    /** WCAG criterion associated with distinguishable labels. */
    override val wcagReference = "WCAG 2.4.6 Headings and Labels (Level AA)"

    /**
     * Compares siblings outside collections and all items within the same semantic collection.
     * Parent/child copies of one merged accessibility target are collapsed before grouping.
     */
    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> {
        val nodesById = nodes.associateBy(A11yNode::nodeId)
        val candidates = nodes
            .asSequence()
            .filter { node ->
                !node.contentDescription.isNullOrBlank() &&
                    (!node.isMergedDescendant || node.isTouchTarget) &&
                    node.isEnabled &&
                    !node.bounds.isEmpty()
            }
            .toList()
        val candidateIds = candidates.mapTo(mutableSetOf(), A11yNode::nodeId)
        val withinTargetDuplicates = candidates.mapNotNull { node ->
            node.withinTargetDuplicateLabel()?.let { duplicatedLabel ->
                issue(
                    node = node,
                    message = "This element announces '$duplicatedLabel' more than once because " +
                        "its content description duplicates its visible text.",
                    howToFix = "Remove the redundant content description. When visible text already " +
                        "labels a control, set its decorative icon's contentDescription to null.",
                )
            }
        }
        val withinTargetDuplicateIds = withinTargetDuplicates
            .mapTo(mutableSetOf()) { it.affectedNode.nodeId }

        val crossTargetDuplicates = candidates
            .asSequence()
            .filterNot { it.nodeId in withinTargetDuplicateIds }
            .filterNot { node -> node.isCopyOfLabeledAncestor(candidateIds, nodesById) }
            .groupBy { node ->
                val scopeId = node.nearestCollectionAncestorId(nodesById) ?: node.parentNodeId
                scopeId to node.normalizedSpokenLabel()
            }
            .filter { (_, group) -> group.size > 1 }
            .flatMap { (_, group) ->
                val text = group.first().spokenLabel()
                group.map { node ->
                    issue(
                        node = node,
                        message = "Multiple elements share the same content description: '$text'. " +
                            "Screen readers may confuse users.",
                        howToFix = "Give each element a unique content description that " +
                            "identifies its specific action or content.",
                    )
                }
            }
            .toList()

        return withinTargetDuplicates + crossTargetDuplicates
    }

    private fun A11yNode.nearestCollectionAncestorId(
        nodesById: Map<String, A11yNode>,
    ): String? {
        var current = parentNodeId?.let(nodesById::get)
        val visited = mutableSetOf<String>()
        while (current != null && visited.add(current.nodeId)) {
            if (current.isCollectionContainer) return current.nodeId
            current = current.parentNodeId?.let(nodesById::get)
        }
        return null
    }

    private fun A11yNode.isCopyOfLabeledAncestor(
        candidateIds: Set<String>,
        nodesById: Map<String, A11yNode>,
    ): Boolean {
        val description = normalizedDescription()
        var current = parentNodeId?.let(nodesById::get)
        val visited = mutableSetOf<String>()
        while (current != null && visited.add(current.nodeId)) {
            if (
                current.nodeId in candidateIds &&
                current.normalizedDescription() == description &&
                current.bounds.contains(bounds)
            ) {
                return true
            }
            current = current.parentNodeId?.let(nodesById::get)
        }
        return false
    }

    private fun A11yNode.normalizedDescription(): String =
        contentDescription.orEmpty().trim().lowercase()

    private fun A11yNode.withinTargetDuplicateLabel(): String? {
        if (!hasExplicitContentDescription) return null
        val description = contentDescription?.trim()?.takeIf(String::isNotBlank) ?: return null
        val text = textLabel?.trim()?.takeIf(String::isNotBlank) ?: return null
        return description.takeIf { it.equals(text, ignoreCase = true) }
    }

    /**
     * Approximates the label TalkBack speaks for a merged Compose node. Compose can expose both
     * ContentDescription and Text, and TalkBack announces both. Comparing only the former makes
     * distinct labels such as "Book cover image, Moby Dick" and "Book cover image, Frankenstein"
     * look identical.
     */
    private fun A11yNode.spokenLabel(): String =
        listOfNotNull(contentDescription?.trim(), textLabel?.trim())
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .joinToString(separator = ", ")

    private fun A11yNode.normalizedSpokenLabel(): String = spokenLabel().lowercase()

    private fun com.composea11yscanner.core.model.Rect.contains(
        other: com.composea11yscanner.core.model.Rect,
    ): Boolean =
        left <= other.left &&
            top <= other.top &&
            right >= other.right &&
            bottom >= other.bottom
}
