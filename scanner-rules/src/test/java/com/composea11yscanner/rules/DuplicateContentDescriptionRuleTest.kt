package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            createNode(parentNodeId = "toolbar", depth = 1, contentDescription = "Submit"),
            createNode(parentNodeId = "dialog", depth = 2, contentDescription = "Submit"),
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
    fun `two siblings with same description produce two issues`() {
        val nodes = listOf(
            createNode(depth = 1, bounds = Rect(0, 0, 100, 100), contentDescription = "Submit"),
            createNode(depth = 1, bounds = Rect(100, 0, 200, 100), contentDescription = "Submit"),
        )
        assertEquals(2, rule.evaluateAll(nodes).size)
    }

    @Test
    fun `three siblings with same description produce three issues`() {
        val nodes = listOf(
            createNode(depth = 1, bounds = Rect(0, 0, 100, 100), contentDescription = "Delete"),
            createNode(depth = 1, bounds = Rect(100, 0, 200, 100), contentDescription = "Delete"),
            createNode(depth = 1, bounds = Rect(200, 0, 300, 100), contentDescription = "Delete"),
        )
        assertEquals(3, rule.evaluateAll(nodes).size)
    }

    // --- edge cases ---

    @Test
    fun `only the duplicate group is flagged, non-duplicates are clean`() {
        val nodes = listOf(
            createNode(depth = 1, bounds = Rect(0, 0, 100, 100), contentDescription = "Submit"),
            createNode(depth = 1, bounds = Rect(100, 0, 200, 100), contentDescription = "Submit"),
            createNode(depth = 1, bounds = Rect(200, 0, 300, 100), contentDescription = "Cancel"),
        )
        val issues = rule.evaluateAll(nodes)
        assertEquals(2, issues.size)
        assertTrue(issues.all { it.message.contains("'Submit'") })
    }

    @Test
    fun `issue message contains the duplicated text`() {
        val nodes = listOf(
            createNode(depth = 1, bounds = Rect(0, 0, 100, 100), contentDescription = "Close dialog"),
            createNode(depth = 1, bounds = Rect(100, 0, 200, 100), contentDescription = "Close dialog"),
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
            createNode(depth = 1, bounds = Rect(0, 0, 100, 100), contentDescription = "X"),
            createNode(depth = 1, bounds = Rect(100, 0, 200, 100), contentDescription = "X"),
        )
        val issue = rule.evaluateAll(nodes).first()
        assertEquals("duplicate-content-description", issue.ruleId)
        assertEquals(A11ySeverity.Warning, issue.severity)
        assertEquals("WCAG 2.4.6 Headings and Labels (Level AA)", issue.wcagReference)
    }

    @Test
    fun `separate controls with the same label remain duplicates`() {
        val first = createNode(
            parentNodeId = "toolbar",
            depth = 1,
            bounds = Rect(0, 0, 100, 100),
            contentDescription = "Open item",
        )
        val second = first.copy(
            nodeId = "second",
            bounds = Rect(110, 0, 210, 100),
        )

        assertEquals(2, rule.evaluateAll(listOf(first, second)).size)
    }

    @Test
    fun `same product label in different collection parents is not a duplicate`() {
        val androidPicks = createNode(
            nodeId = "android-picks-cupcake",
            parentNodeId = "android-picks-row",
            depth = 8,
            bounds = Rect(66, 528, 534, 1216),
            contentDescription = "Cupcake A tag line",
            isTouchTarget = true,
        )
        val wfhFavourites = androidPicks.copy(
            nodeId = "wfh-favourites-cupcake",
            parentNodeId = "wfh-favourites-row",
            bounds = Rect(66, 2052, 534, 2060),
        )

        assertTrue(rule.evaluateAll(listOf(androidPicks, wfhFavourites)).isEmpty())
    }
}
