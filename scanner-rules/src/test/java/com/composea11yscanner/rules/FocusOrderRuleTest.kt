package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// screenDensity=1f makes 1dp == 1px, simplifying expected values.
class FocusOrderRuleTest {

    private val rule = FocusOrderRule(screenDensity = 1f)

    // --- passing cases ---

    @Test
    fun `empty node list returns no issues`() {
        assertTrue(rule.evaluateAll(emptyList()).isEmpty())
    }

    @Test
    fun `single focusable node returns no issues`() {
        assertTrue(rule.evaluateAll(listOf(createNode(isFocusable = true))).isEmpty())
    }

    @Test
    fun `focusable nodes in correct top-to-bottom order pass`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 0, 100, 50)),
            createNode(isFocusable = true, bounds = Rect(0, 60, 100, 110)),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `upward jump of exactly the threshold is not flagged`() {
        // threshold=8px; 100-92=8 → condition is strict less-than, so NOT flagged
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = true, bounds = Rect(0, 92, 100, 142)),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `no focusable nodes returns no issues`() {
        val nodes = listOf(
            createNode(isFocusable = false, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = false, bounds = Rect(0, 0, 100, 50)),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `merged descendant focusable nodes are skipped`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(
                isFocusable = true,
                bounds = Rect(0, 0, 100, 50),
                isMergedDescendant = true,
            ),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `focusable child inside focusable parent is skipped`() {
        val nodes = listOf(
            createNode(isFocusable = true, depth = 0, bounds = Rect(0, 100, 200, 160)),
            createNode(isFocusable = true, depth = 1, bounds = Rect(0, 110, 100, 140)),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `zero-sized focusable node is skipped`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = true, bounds = Rect.Zero),
        )

        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `reverse ordered nodes inside semantic collection are skipped`() {
        val collection = createNode(
            nodeId = "collection",
            bounds = Rect(0, 0, 200, 300),
            isCollectionContainer = true,
        )
        val nodes = listOf(
            collection,
            createNode(
                isFocusable = true,
                parentNodeId = collection.nodeId,
                bounds = Rect(0, 200, 100, 250),
            ),
            createNode(
                isFocusable = true,
                parentNodeId = collection.nodeId,
                bounds = Rect(0, 100, 100, 150),
            ),
        )

        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `nodes in different semantic containers are not compared`() {
        val nodes = listOf(
            createNode(
                isFocusable = true,
                parentNodeId = "content",
                bounds = Rect(0, 700, 100, 750),
            ),
            createNode(
                isFocusable = true,
                parentNodeId = "app-bar",
                bounds = Rect(0, 20, 100, 70),
            ),
        )

        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `upward sibling traversal is still flagged`() {
        val nodes = listOf(
            createNode(
                isFocusable = true,
                parentNodeId = "form",
                bounds = Rect(0, 100, 100, 150),
            ),
            createNode(
                isFocusable = true,
                parentNodeId = "form",
                bounds = Rect(0, 50, 100, 100),
            ),
        )

        assertEquals(1, rule.evaluateAll(nodes).size)
    }

    // --- failing cases ---

    @Test
    fun `upward jump one pixel beyond threshold is flagged`() {
        // 100-91=9 > 8 → flagged
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = true, bounds = Rect(0, 91, 100, 141)),
        )
        assertEquals(1, rule.evaluateAll(nodes).size)
    }

    @Test
    fun `multiple upward jumps produce multiple issues`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 200, 100, 250)),
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = true, bounds = Rect(0, 0, 100, 50)),
        )
        assertEquals(2, rule.evaluateAll(nodes).size)
    }

    // --- edge cases ---

    @Test
    fun `non-focusable nodes between focusable ones are ignored`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = false, bounds = Rect(0, 0, 100, 50)),  // would be a jump if focusable
            createNode(isFocusable = true, bounds = Rect(0, 200, 100, 250)),
        )
        assertTrue(rule.evaluateAll(nodes).isEmpty())
    }

    @Test
    fun `issue message contains both position values in dp`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = true, bounds = Rect(0, 50, 100, 100)),
        )
        val issue = rule.evaluateAll(nodes).first()
        assertTrue(issue.message.contains("100dp"))
        assertTrue(issue.message.contains("50dp"))
    }

    @Test
    fun `custom threshold increases detection sensitivity`() {
        val strictRule = FocusOrderRule(screenDensity = 1f, jumpThresholdDp = 1f)
        // 50-48=2px > 1dp threshold → flagged
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 50, 100, 100)),
            createNode(isFocusable = true, bounds = Rect(0, 48, 100, 98)),
        )
        assertEquals(1, strictRule.evaluateAll(nodes).size)
    }

    @Test
    fun `evaluate returns null for scan-level rule`() {
        assertNull(rule.evaluate(createNode(isFocusable = true)))
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val nodes = listOf(
            createNode(isFocusable = true, bounds = Rect(0, 100, 100, 150)),
            createNode(isFocusable = true, bounds = Rect(0, 50, 100, 100)),
        )
        val issue = rule.evaluateAll(nodes).first()
        assertEquals("focus-order", issue.ruleId)
        assertEquals(A11ySeverity.Error, issue.severity)
        assertEquals("WCAG 2.4.3 Focus Order (Level A)", issue.wcagReference)
    }
}
