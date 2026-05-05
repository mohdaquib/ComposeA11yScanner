package com.composea11yscanner.core.model

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
