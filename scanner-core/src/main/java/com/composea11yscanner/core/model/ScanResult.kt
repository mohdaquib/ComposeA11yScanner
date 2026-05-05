package com.composea11yscanner.core.model

data class ScanResult(
    val scanId: String,
    val timestamp: Long,
    val totalNodes: Int,
    val issues: List<A11yIssue>,
    val passedRules: Int,
    val failedRules: Int,
) {
    val errorCount: Int get() = issues.count { it.severity == A11ySeverity.Error }
    val warningCount: Int get() = issues.count { it.severity == A11ySeverity.Warning }
    val infoCount: Int get() = issues.count { it.severity == A11ySeverity.Info }

    val hasErrors: Boolean get() = errorCount > 0

    /**
     * Score from 0–100 representing the percentage of rules that passed.
     * Returns 100 when no rules have run yet.
     */
    val overallScore: Float
        get() {
            val total = passedRules + failedRules
            return if (total == 0) 100f else (passedRules.toFloat() / total) * 100f
        }
}
