package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchTargetRuleTest {

    private val rule = TouchTargetRule()

    // --- passing cases ---

    @Test
    fun `non-interactive node is not evaluated`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = false, touchTargetSize = DpSize(40f, 40f))))
    }

    @Test
    fun `touch target at exactly minimum size passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(48f, 48f))))
    }

    @Test
    fun `floating point noise just below minimum size passes`() {
        assertNull(
            rule.evaluate(
                createNode(
                    isTouchTarget = true,
                    touchTargetSize = DpSize(47.999996f, 47.999996f),
                )
            )
        )
    }

    @Test
    fun `touch target larger than minimum passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(56f, 64f))))
    }

    @Test
    fun `merged descendant touch target is skipped`() {
        assertNull(
            rule.evaluate(
                createNode(
                    isTouchTarget = true,
                    touchTargetSize = DpSize(32f, 32f),
                    isMergedDescendant = true,
                )
            )
        )
    }

    // --- failing cases ---

    @Test
    fun `width below minimum fails`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(44f, 48f))))
    }

    @Test
    fun `meaningfully undersized target is not hidden by measurement tolerance`() {
        assertNotNull(
            rule.evaluate(
                createNode(
                    isTouchTarget = true,
                    touchTargetSize = DpSize(47.98f, 48f),
                )
            )
        )
    }

    @Test
    fun `height below minimum fails`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(48f, 44f))))
    }

    @Test
    fun `both dimensions below minimum fail`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(32f, 32f))))
    }

    // --- edge cases ---

    @Test
    fun `zero-size touch target fails with correct dimensions in message`() {
        val issue = rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(0f, 0f)))
        assertNotNull(issue)
        assertTrue(issue!!.message.contains("0x0dp"))
    }

    @Test
    fun `message contains actual dimensions`() {
        val issue = rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(32f, 40f)))
        assertNotNull(issue)
        assertTrue(issue!!.message.contains("32x40dp"))
    }

    @Test
    fun `custom threshold accepts larger node`() {
        val rule36 = TouchTargetRule(minTouchTargetDp = 36)
        assertNull(rule36.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(40f, 40f))))
    }

    @Test
    fun `custom threshold rejects node below it`() {
        val rule36 = TouchTargetRule(minTouchTargetDp = 36)
        assertNotNull(rule36.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(30f, 30f))))
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val issue = rule.evaluate(createNode(isTouchTarget = true, touchTargetSize = DpSize(40f, 40f)))!!
        assertEquals("touch-target-size", issue.ruleId)
        assertEquals(A11ySeverity.Error, issue.severity)
        assertEquals("WCAG 2.5.5 Target Size (Level AA)", issue.wcagReference)
    }

    @Test
    fun `evaluateAll aggregates per-node results`() {
        val nodes = listOf(
            createNode(isTouchTarget = false),
            createNode(isTouchTarget = true, touchTargetSize = DpSize(40f, 40f)),
            createNode(isTouchTarget = true, touchTargetSize = DpSize(48f, 48f)),
        )
        assertEquals(1, rule.evaluateAll(nodes).size)
    }
}
