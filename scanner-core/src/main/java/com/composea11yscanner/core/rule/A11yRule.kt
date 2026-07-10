package com.composea11yscanner.core.rule

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity

/** Contract implemented by every accessibility rule. */
interface A11yRule {
    /** Stable id used in configuration, reports, and issue ids. */
    val ruleId: String

    /** Human-readable rule name shown in reports. */
    val ruleName: String

    /** Severity assigned to issues produced by this rule. */
    val severity: A11ySeverity

    /** Optional WCAG criterion associated with this rule. */
    val wcagReference: String?

    /**
     * Evaluates a single node.
     *
     * @param node Node to evaluate.
     * @return Issue when the node violates the rule, otherwise null.
     */
    fun evaluate(node: A11yNode): A11yIssue?

    /**
     * Evaluates an entire node list.
     *
     * Per-node rules are handled by the default implementation.
     *
     * @param nodes Nodes to evaluate.
     * @return Issues produced by this rule.
     */
    fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> = nodes.mapNotNull { evaluate(it) }
}

/**
 * Base for rules that must inspect all nodes together (e.g. duplicate detection).
 * [evaluate] is sealed to a no-op; subclasses implement [evaluateAll] instead.
 */
abstract class BaseScanRule : A11yRule {

    /** Always returns null because scan-level rules evaluate node lists. */
    final override fun evaluate(node: A11yNode): A11yIssue? = null

    /** Evaluates the complete node list. */
    abstract override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue>

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

/** Base class for rules that evaluate each node independently. */
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
