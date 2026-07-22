package com.composea11yscanner.ui

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.ScanResult
import com.composea11yscanner.core.rule.A11yRule
import java.util.UUID

/**
 * Orchestrates a single accessibility scan.
 *
 * @param rules The rules to run. Build from [com.composea11yscanner.rules.ScannerRules.buildRules]
 *   or supply a custom list.
 */
class A11yScanner(
    private val rules: List<A11yRule>,
) {
    /**
     * Scans from a semantics node, typically in tests or manual tree walks.
     *
     * @param rootNode Root semantics node to scan.
     * @return Completed scan result.
     */
    fun scan(rootNode: SemanticsNode): ScanResult =
        buildResult(A11yNodeExtractor().extract(rootNode))

    /**
     * Scans the live Compose semantics tree via [SemanticsOwner].
     *
     * @param owner Semantics owner for the Compose tree.
     * @return Completed scan result.
     */
    @OptIn(InternalComposeUiApi::class)
    fun scan(owner: SemanticsOwner): ScanResult =
        buildResult(A11yNodeExtractor().extract(owner))

    private fun buildResult(nodes: List<A11yNode>): ScanResult {
        val issues = rules
            .flatMap { it.evaluateAll(nodes) }
            .sortedWith(compareBy({ it.severity.sortOrder }, { it.ruleId }))
        val failedRuleIds = issues.map { it.ruleId }.toSet()

        return ScanResult(
            scanId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            totalNodes = nodes.size,
            issues = issues,
            passedRules = rules.size - failedRuleIds.size,
            failedRules = failedRuleIds.size,
        )
    }
}
