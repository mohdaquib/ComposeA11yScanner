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

    /** Evaluates an entire node list. Per-node rules are handled by the default implementation. */
    fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> = nodes.mapNotNull { evaluate(it) }
}

/**
 * Base for rules that must inspect all nodes together (e.g. duplicate detection).
 * [evaluate] is sealed to a no-op; subclasses implement [evaluateAll] instead.
 */
abstract class BaseScanRule : A11yRule {

    final override fun evaluate(node: A11yNode): A11yIssue? = null

    abstract override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue>

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
