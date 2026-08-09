package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseScanRule

/** Flags sibling-level nodes that reuse the same non-empty content description. */
class DuplicateContentDescriptionRule : BaseScanRule() {

    /** Stable id for the duplicate content description rule. */
    override val ruleId = "duplicate-content-description"

    /** Human-readable rule name. */
    override val ruleName = "Duplicate Content Description"

    /** Severity assigned to duplicate labels. */
    override val severity = A11ySeverity.Warning

    /** WCAG criterion associated with distinguishable labels. */
    override val wcagReference = "WCAG 2.4.6 Headings and Labels (Level AA)"

    /** Evaluates all nodes together to find repeated labels among semantic siblings. */
    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> =
        nodes
            .asSequence()
            .filter { !it.contentDescription.isNullOrBlank() && !it.isMergedDescendant }
            .groupBy { it.parentNodeId to it.contentDescription }
            .filter { (_, group) -> group.size > 1 }
            .flatMap { (key, group) ->
                val text = key.second
                group.map { node ->
                    issue(
                        node = node,
                        message = "Two elements share the same content description: '$text'. " +
                            "Screen readers may confuse users.",
                        howToFix = "Give each element a unique content description that " +
                            "identifies its specific action or content.",
                    )
                }
            }
            .toList()
}
