package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseScanRule

class DuplicateContentDescriptionRule : BaseScanRule() {

    override val ruleId = "duplicate-content-description"
    override val ruleName = "Duplicate Content Description"
    override val severity = A11ySeverity.Warning
    override val wcagReference = "WCAG 2.4.6 Headings and Labels (Level AA)"

    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> =
        nodes
            .filter { !it.contentDescription.isNullOrBlank() && !it.isMergedDescendant }
            .groupBy { it.depth to it.contentDescription }
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
}
