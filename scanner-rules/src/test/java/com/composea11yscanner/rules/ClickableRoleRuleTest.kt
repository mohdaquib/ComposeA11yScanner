package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yRole
import com.composea11yscanner.core.model.A11ySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClickableRoleRuleTest {

    private val rule = ClickableRoleRule()

    // --- passing cases ---

    @Test
    fun `non-touch-target is not evaluated`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = false, role = null)))
    }

    @Test
    fun `touch target with Button role passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.Button)))
    }

    @Test
    fun `touch target with Checkbox role passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.Checkbox)))
    }

    @Test
    fun `touch target with Switch role passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.Switch)))
    }

    @Test
    fun `touch target with TextField role passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.TextField)))
    }

    @Test
    fun `touch target with Image role and non-empty description passes`() {
        assertNull(
            rule.evaluate(
                createNode(isTouchTarget = true, role = A11yRole.Image, contentDescription = "Photo")
            )
        )
    }

    // --- failing cases ---

    @Test
    fun `touch target with null role fails`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, role = null)))
    }

    @Test
    fun `touch target with Image role and null description fails`() {
        assertNotNull(
            rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.Image, contentDescription = null))
        )
    }

    @Test
    fun `touch target with Image role and blank description fails`() {
        assertNotNull(
            rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.Image, contentDescription = "  "))
        )
    }

    @Test
    fun `touch target with Image role and empty description fails`() {
        assertNotNull(
            rule.evaluate(createNode(isTouchTarget = true, role = A11yRole.Image, contentDescription = ""))
        )
    }

    // --- edge cases ---

    @Test
    fun `merged descendant that is a touch target with no role is skipped`() {
        assertNull(
            rule.evaluate(createNode(isTouchTarget = true, role = null, isMergedDescendant = true))
        )
    }

    @Test
    fun `evaluateAll aggregates per-node results`() {
        val nodes = listOf(
            createNode(isTouchTarget = false),
            createNode(isTouchTarget = true, role = A11yRole.Button),
            createNode(isTouchTarget = true, role = null),
        )
        assertEquals(1, rule.evaluateAll(nodes).size)
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val issue = rule.evaluate(createNode(isTouchTarget = true, role = null))!!
        assertEquals("clickable-role", issue.ruleId)
        assertEquals(A11ySeverity.Error, issue.severity)
        assertEquals("WCAG 4.1.2 Name, Role, Value (Level A)", issue.wcagReference)
    }
}
