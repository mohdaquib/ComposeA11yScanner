package com.composea11yscanner.core.model

/**
 * Final outcome of a completed accessibility scan.
 *
 * @property scanId Unique id for this scan run.
 * @property timestamp Wall-clock timestamp in milliseconds when the result was created.
 * @property totalNodes Number of nodes considered by the scanner.
 * @property issues Issues produced by enabled rules.
 * @property passedRules Number of enabled rules that produced no issues.
 * @property failedRules Number of enabled rules that produced at least one issue.
 */
data class ScanResult(
    val scanId: String,
    val timestamp: Long,
    val totalNodes: Int,
    val issues: List<A11yIssue>,
    val passedRules: Int,
    val failedRules: Int,
) {
    /** Number of error-severity issues in [issues]. */
    val errorCount: Int get() = issues.count { it.severity == A11ySeverity.Error }

    /** Number of warning-severity issues in [issues]. */
    val warningCount: Int get() = issues.count { it.severity == A11ySeverity.Warning }

    /** Number of info-severity issues in [issues]. */
    val infoCount: Int get() = issues.count { it.severity == A11ySeverity.Info }

    /** True when at least one error-severity issue was found. */
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
