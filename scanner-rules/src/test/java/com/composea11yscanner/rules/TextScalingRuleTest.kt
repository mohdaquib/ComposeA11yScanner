package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// screenDensity=1f makes 1dp == 1px, simplifying expected overflow arithmetic.
class TextScalingRuleTest {

    private val rule = TextScalingRule(screenDensity = 1f)

    // --- passing cases ---

    @Test
    fun `non-text composable is not evaluated`() {
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val button = createNode(composableName = "Button", depth = 1, bounds = Rect(0, 0, 200, 80))
        assertTrue(rule.evaluateAll(listOf(parent, button)).isEmpty())
    }

    @Test
    fun `text node at depth 0 has no parent and is skipped`() {
        val text = createNode(composableName = "Text", depth = 0, bounds = Rect(0, 0, 100, 80))
        assertTrue(rule.evaluateAll(listOf(text)).isEmpty())
    }

    @Test
    fun `text node with sufficient room in parent passes`() {
        // scaledHeight = 100 * 1.3 = 130 < parent.bottom 200 → no overflow
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 200))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 100))
        assertTrue(rule.evaluateAll(listOf(parent, text)).isEmpty())
    }

    @Test
    fun `scaled height landing exactly on parent bottom passes`() {
        // 80 * 1.3 = 104.0; scaledBottom = 0 + 104 = 104 == parent.bottom 104 → overflowPx = 0
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 104))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 80))
        assertTrue(rule.evaluateAll(listOf(parent, text)).isEmpty())
    }

    @Test
    fun `text node with zero height never overflows`() {
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 0))
        assertTrue(rule.evaluateAll(listOf(parent, text)).isEmpty())
    }

    // --- failing cases ---

    @Test
    fun `text node that overflows parent at 1_3x scale fails`() {
        // scaledHeight = 80 * 1.3 = 104; scaledBottom = 104 > parent.bottom 100 → overflow 4px
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 80))
        assertEquals(1, rule.evaluateAll(listOf(parent, text)).size)
    }

    @Test
    fun `BasicText composable name is recognised`() {
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val text = createNode(composableName = "BasicText", depth = 1, bounds = Rect(0, 0, 200, 80))
        assertEquals(1, rule.evaluateAll(listOf(parent, text)).size)
    }

    // --- edge cases ---

    @Test
    fun `tightest parent is selected when multiple candidates at depth minus 1`() {
        // tightParent area=20000 vs looseParent area=160000; tightParent is chosen
        // tightParent.bottom=100 < scaledBottom=104 → overflow flagged
        val tightParent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val looseParent = createNode(depth = 0, bounds = Rect(0, 0, 400, 400))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 80))
        assertEquals(1, rule.evaluateAll(listOf(tightParent, looseParent, text)).size)
    }

    @Test
    fun `custom scale factor changes overflow threshold`() {
        // At 2.0x: scaledHeight = 60 * 2 = 120 > parent.bottom 100 → overflow
        val rule2x = TextScalingRule(screenDensity = 1f, scaleFactor = 2.0f)
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 60))
        assertEquals(1, rule2x.evaluateAll(listOf(parent, text)).size)
    }

    @Test
    fun `issue message contains original and scaled height in dp`() {
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 80))
        val issue = rule.evaluateAll(listOf(parent, text)).first()
        assertTrue(issue.message.contains("80dp"))   // original
        assertTrue(issue.message.contains("104dp"))  // 80 * 1.3 = 104
    }

    @Test
    fun `evaluate returns null for scan-level rule`() {
        assertNull(rule.evaluate(createNode()))
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val parent = createNode(depth = 0, bounds = Rect(0, 0, 200, 100))
        val text = createNode(composableName = "Text", depth = 1, bounds = Rect(0, 0, 200, 80))
        val issue = rule.evaluateAll(listOf(parent, text)).first()
        assertEquals("text-scaling", issue.ruleId)
        assertEquals(A11ySeverity.Warning, issue.severity)
        assertEquals("WCAG 1.4.4 Resize Text (Level AA)", issue.wcagReference)
    }
}
