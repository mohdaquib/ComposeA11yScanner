package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MissingContentDescriptionRuleTest {

    private val rule = MissingContentDescriptionRule()

    // --- passing cases ---

    @Test
    fun `non-interactive non-image node passes`() {
        assertNull(rule.evaluate(createNode(composableName = "Box", isTouchTarget = false)))
    }

    @Test
    fun `interactive node with description passes`() {
        assertNull(rule.evaluate(createNode(isTouchTarget = true, contentDescription = "Submit")))
    }

    @Test
    fun `Image composable with description passes`() {
        assertNull(rule.evaluate(createNode(composableName = "Image", contentDescription = "Profile photo")))
    }

    @Test
    fun `merged decorative image with no description is skipped`() {
        assertNull(
            rule.evaluate(
                createNode(
                    composableName = "Image",
                    isTouchTarget = false,
                    contentDescription = null,
                    isMergedDescendant = true,
                ),
            )
        )
    }

    @Test
    fun `merged interactive node with no description fails`() {
        assertNotNull(
            rule.evaluate(
                createNode(
                    composableName = "Button",
                    isTouchTarget = true,
                    contentDescription = null,
                    isMergedDescendant = true,
                ),
            ),
        )
    }

    @Test
    fun `zero-sized interactive node with no description is skipped`() {
        assertNull(
            rule.evaluate(
                createNode(
                    bounds = Rect.Zero,
                    isTouchTarget = true,
                    effectiveTouchBounds = Rect.Zero,
                    contentDescription = null,
                ),
            ),
        )
    }

    @Test
    fun `partially visible interactive node with no description still fails`() {
        assertNotNull(
            rule.evaluate(
                createNode(
                    bounds = Rect(-10, 10, 20, 40),
                    isTouchTarget = true,
                    contentDescription = null,
                ),
            ),
        )
    }

    // --- failing cases ---

    @Test
    fun `interactive node with null description fails`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, contentDescription = null)))
    }

    @Test
    fun `interactive node with blank description fails`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, contentDescription = "   ")))
    }

    @Test
    fun `interactive node with empty description fails`() {
        assertNotNull(rule.evaluate(createNode(isTouchTarget = true, contentDescription = "")))
    }

    @Test
    fun `Image composable with no description fails`() {
        assertNotNull(rule.evaluate(createNode(composableName = "Image", contentDescription = null)))
    }

    // --- edge cases ---

    @Test
    fun `AsyncImage is treated as an image composable`() {
        assertNotNull(rule.evaluate(createNode(composableName = "AsyncImage", contentDescription = null)))
    }

    @Test
    fun `SubcomposeAsyncImage is treated as an image composable`() {
        assertNotNull(
            rule.evaluate(createNode(composableName = "SubcomposeAsyncImage", contentDescription = null))
        )
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val issue = rule.evaluate(createNode(isTouchTarget = true))!!
        assertEquals("missing-content-description", issue.ruleId)
        assertEquals(A11ySeverity.Error, issue.severity)
        assertEquals("WCAG 1.1.1 Non-text Content (Level A)", issue.wcagReference)
    }

    @Test
    fun `evaluateAll aggregates per-node results`() {
        val nodes = listOf(
            createNode(composableName = "Box"),
            createNode(isTouchTarget = true, contentDescription = null),
            createNode(isTouchTarget = true, contentDescription = "OK"),
        )
        assertEquals(1, rule.evaluateAll(nodes).size)
    }
}
