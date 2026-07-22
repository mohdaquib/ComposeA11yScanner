package com.composea11yscanner.core

import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11yRole
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Color
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult
import com.composea11yscanner.core.rule.BaseA11yRule
import com.composea11yscanner.core.rule.BaseScanRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreModelAndRuleTest {

    private val node = A11yNode(
        nodeId = "node-1",
        composableName = "Button",
        bounds = Rect(0, 0, 100, 48),
        contentDescription = "Submit",
        isTouchTarget = true,
        textColor = Color(0xFF000000),
        backgroundColors = emptyList(),
        isFocusable = true,
        isMergedDescendant = false,
        depth = 0,
        role = A11yRole.Button,
    )

    @Test
    fun `base node rule delegates evaluate to check and builds issue metadata`() {
        val rule = object : BaseA11yRule() {
            override val ruleId = "test-rule"
            override val ruleName = "Test Rule"
            override val severity = A11ySeverity.Warning
            override val wcagReference = "WCAG test"

            override fun check(node: A11yNode) = issue(
                node = node,
                message = "Test message",
                howToFix = "Test fix",
            )
        }

        val issue = rule.evaluate(node)

        requireNotNull(issue)
        assertEquals("test-rule_node-1", issue.issueId)
        assertEquals(A11ySeverity.Warning, issue.severity)
        assertEquals("test-rule", issue.ruleId)
        assertEquals("Test Rule", issue.ruleName)
        assertSame(node, issue.affectedNode)
        assertEquals("Test message", issue.message)
        assertEquals("Test fix", issue.howToFix)
        assertEquals("WCAG test", issue.wcagReference)
    }

    @Test
    fun `base scan rule evaluate is a no-op and issue helper fills metadata`() {
        val rule = object : BaseScanRule() {
            override val ruleId = "scan-rule"
            override val ruleName = "Scan Rule"
            override val severity = A11ySeverity.Error
            override val wcagReference = null

            override fun evaluateAll(nodes: List<A11yNode>) = nodes.map {
                issue(
                    node = it,
                    message = "Scan message",
                    howToFix = "Scan fix",
                )
            }
        }

        assertNull(rule.evaluate(node))

        val issue = rule.evaluateAll(listOf(node)).single()
        assertEquals("scan-rule_node-1", issue.issueId)
        assertEquals(A11ySeverity.Error, issue.severity)
        assertEquals("scan-rule", issue.ruleId)
        assertEquals("Scan Rule", issue.ruleName)
        assertSame(node, issue.affectedNode)
        assertEquals("Scan message", issue.message)
        assertEquals("Scan fix", issue.howToFix)
        assertNull(issue.wcagReference)
    }

    @Test
    fun `scan result exposes issue counts score and error state`() {
        val result = ScanResult(
            scanId = "scan-1",
            timestamp = 1L,
            totalNodes = 3,
            issues = listOf(
                issue(A11ySeverity.Error),
                issue(A11ySeverity.Warning),
                issue(A11ySeverity.Warning),
                issue(A11ySeverity.Info),
            ),
            passedRules = 3,
            failedRules = 1,
        )

        assertEquals(1, result.errorCount)
        assertEquals(2, result.warningCount)
        assertEquals(1, result.infoCount)
        assertTrue(result.hasErrors)
        assertEquals(75f, result.overallScore, 0.001f)
    }

    @Test
    fun `scan result score is perfect when no rules ran`() {
        val result = ScanResult(
            scanId = "scan-1",
            timestamp = 1L,
            totalNodes = 0,
            issues = emptyList(),
            passedRules = 0,
            failedRules = 0,
        )

        assertFalse(result.hasErrors)
        assertEquals(100f, result.overallScore, 0.001f)
    }

    @Test
    fun `severity ordering and rect helpers expose expected values`() {
        assertTrue(A11ySeverity.Error < A11ySeverity.Warning)
        assertTrue(A11ySeverity.Warning < A11ySeverity.Info)
        assertEquals(Rect(0, 0, 0, 0), Rect.Zero)
        assertFalse(Rect(0, 0, 10, 10).isEmpty())
        assertTrue(Rect(0, 0, 0, 10).isEmpty())
        assertTrue(Rect(0, 0, 10, 0).isEmpty())
    }

    @Test
    fun `roles list includes all scanner role values`() {
        assertEquals(
            listOf(
                A11yRole.Button,
                A11yRole.Checkbox,
                A11yRole.DropdownList,
                A11yRole.Image,
                A11yRole.RadioButton,
                A11yRole.Switch,
                A11yRole.Tab,
                A11yRole.TextField,
            ),
            A11yRole.entries,
        )
    }

    private fun issue(severity: A11ySeverity) = com.composea11yscanner.core.model.A11yIssue(
        issueId = "issue-${severity.sortOrder}",
        severity = severity,
        ruleId = "rule-${severity.sortOrder}",
        ruleName = "Rule",
        affectedNode = node,
        message = "Message",
        howToFix = "Fix",
        wcagReference = null,
    )
}
