package com.composea11yscanner.core

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.ScanResult
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import com.composea11yscanner.core.rule.A11yRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

/**
 * Core orchestrator: runs a fixed set of accessibility rules over a list of nodes and
 * streams scan progress as a [Flow] of [ScannerState].
 *
 * Emission sequence:
 *   Scanning(0f) → Scanning(1/n) → … → Scanning(1f) → Complete(result)
 *   or Complete(emptyResult) when no enabled rules / no nodes.
 *   or Error(message) if a rule throws an unexpected exception.
 *
 * The entire flow body runs on [Dispatchers.Default] via [flowOn]; collectors receive
 * emissions on their own dispatcher.
 *
 * @param rules All candidate rules. Only those whose [A11yRule.ruleId] appears in
 *   [config.enabledRules] will be evaluated — defensive against callers that pass the
 *   full rule set regardless of config.
 */
class A11yScanEngine(
    rules: List<A11yRule>,
    private val config: ScannerConfig,
) {
    private val enabledRules: List<A11yRule> = rules.filter { it.ruleId in config.enabledRules }

    fun scan(nodes: List<A11yNode>): Flow<ScannerState> = flow {
        // Fast path: nothing to evaluate.
        if (enabledRules.isEmpty() || nodes.isEmpty()) {
            emit(ScannerState.Complete(buildResult(nodes.size, emptyList(), emptySet())))
            return@flow
        }

        emit(ScannerState.Scanning(0f))

        val allIssues = mutableListOf<A11yIssue>()
        val failedRuleIds = mutableSetOf<String>()

        try {
            enabledRules.forEachIndexed { index, rule ->
                val issues = rule.evaluateAll(nodes)
                allIssues += issues
                if (issues.isNotEmpty()) failedRuleIds += rule.ruleId
                // Progress advances to 1f after the last rule.
                emit(ScannerState.Scanning((index + 1f) / enabledRules.size))
            }
            emit(ScannerState.Complete(buildResult(nodes.size, allIssues, failedRuleIds)))
        } catch (e: CancellationException) {
            throw e // never swallow cancellation
        } catch (e: Exception) {
            emit(ScannerState.Error(e.message ?: "Scan failed"))
        }
    }.flowOn(Dispatchers.Default)

    // --- helpers ---

    private fun buildResult(
        totalNodes: Int,
        issues: List<A11yIssue>,
        failedRuleIds: Set<String>,
    ): ScanResult = ScanResult(
        scanId = UUID.randomUUID().toString(),
        timestamp = System.currentTimeMillis(),
        totalNodes = totalNodes,
        issues = issues.sortedWith(compareBy({ it.severity.sortOrder }, { it.ruleId })),
        passedRules = enabledRules.size - failedRuleIds.size,
        failedRules = failedRuleIds.size,
    )
}
