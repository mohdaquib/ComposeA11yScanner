package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateContentDescriptionRuleTest {

    private val rule = DuplicateContentDescriptionRule()

    // --- passing cases ---

    @Test
    fun `empty node list returns no issues`() {
        assertTrue(rule.evaluateAll(emptyList()).isEmpty())
    }

    @Test
    fun `unique descriptions at same depth return no issues`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Button A"),
            createNode(depth = 1, contentDescription = "Button B"),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `same description at different depths is not a duplicate`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Submit"),
            createNode(depth = 2, contentDescription = "Submit"),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `null descriptions are not compared`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = null),
            createNode(depth = 1, contentDescription = null),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `blank descriptions are not compared`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "   "),
            createNode(depth = 1, contentDescription = "   "),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `merged descendants are excluded from the check`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Submit", isMergedDescendant = true),
            createNode(depth = 1, contentDescription = "Submit", isMergedDescendant = true),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    // --- failing cases ---

    @Test
    fun `two nodes with same description at same depth produce two issues`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Submit"),
            createNode(depth = 1, contentDescription = "Submit"),
        )
        assertEquals(2, rule.evaluateAll(nodes).size)
    }

    @Test
    fun `three nodes with same description at same depth produce three issues`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Delete"),
            createNode(depth = 1, contentDescription = "Delete"),
            createNode(depth = 1, contentDescription = "Delete"),
        )
        assertEquals(3, rule.evaluateAll(nodes).size)
    }

    // --- edge cases ---

    @Test
    fun `only the duplicate group is flagged, non-duplicates are clean`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Submit"),
            createNode(depth = 1, contentDescription = "Submit"),
            createNode(depth = 1, contentDescription = "Cancel"),
        )
        val issues = rule.evaluateAll(nodes)
        assertEquals(2, issues.size)
        assertTrue(issues.all { it.message.contains("'Submit'") })
    }

    @Test
    fun `issue message contains the duplicated text`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "Close dialog"),
            createNode(depth = 1, contentDescription = "Close dialog"),
        )
        val issue = rule.evaluateAll(nodes).first()
        assertTrue(issue.message.contains("'Close dialog'"))
    }

    @Test
    fun `evaluate returns null for scan-level rule`() {
        assertNull(rule.evaluate(createNode(depth = 1, contentDescription = "Anything")))
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val nodes = listOf(
            createNode(depth = 1, contentDescription = "X"),
            createNode(depth = 1, contentDescription = "X"),
        )
        val issue = rule.evaluateAll(nodes).first()
        assertEquals("duplicate-content-description", issue.ruleId)
        assertEquals(A11ySeverity.Warning, issue.severity)
        assertEquals("WCAG 2.4.6 Headings and Labels (Level AA)", issue.wcagReference)
    }
}
