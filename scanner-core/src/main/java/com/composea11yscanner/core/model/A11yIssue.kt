package com.composea11yscanner.core.model

/**
 * Accessibility violation produced by an [com.composea11yscanner.core.rule.A11yRule].
 *
 * @property issueId Stable issue identifier, usually derived from the rule id and affected node id.
 * @property severity User-facing priority used for sorting and visual treatment.
 * @property ruleId Stable identifier of the rule that produced this issue.
 * @property ruleName Human-readable rule name.
 * @property affectedNode Node that caused the rule to fail.
 * @property message Short explanation of the problem.
 * @property howToFix Suggested remediation text for developers.
 * @property wcagReference Optional WCAG criterion related to the issue.
 */
data class A11yIssue(
    val issueId: String,
    val severity: A11ySeverity,
    val ruleId: String,
    val ruleName: String,
    val affectedNode: A11yNode,
    val message: String,
    val howToFix: String,
    val wcagReference: String?,
)
