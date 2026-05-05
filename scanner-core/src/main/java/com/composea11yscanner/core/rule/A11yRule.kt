package com.composea11yscanner.core.rule

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity

interface A11yRule {
    val ruleId: String
    val ruleName: String
    val severity: A11ySeverity
    val wcagReference: String?

    fun evaluate(node: A11yNode): A11yIssue?
}

abstract class BaseA11yRule : A11yRule {

    /**
     * Entry point called by the scanner engine. Sealed final so cross-cutting
     * concerns (logging, timing, exception guarding) can be added here without
     * touching every concrete rule.
     */
    final override fun evaluate(node: A11yNode): A11yIssue? = check(node)

    /** Concrete rules implement their logic here and return null if the node passes. */
    protected abstract fun check(node: A11yNode): A11yIssue?

    /** Builds an [A11yIssue] with all rule-level fields pre-filled. */
    protected fun issue(node: A11yNode, message: String, howToFix: String): A11yIssue =
        A11yIssue(
            issueId = "${ruleId}_${node.nodeId}",
            severity = severity,
            ruleId = ruleId,
            ruleName = ruleName,
            affectedNode = node,
            message = message,
            howToFix = howToFix,
            wcagReference = wcagReference,
        )
}
