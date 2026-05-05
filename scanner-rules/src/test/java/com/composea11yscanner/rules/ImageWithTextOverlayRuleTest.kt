package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageWithTextOverlayRuleTest {

    private val rule = ImageWithTextOverlayRule()

    // --- passing cases ---

    @Test
    fun `no image nodes returns no issues`() {
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 50))
        assertTrue(rule.evaluateAll(listOf(text)).isEmpty())
    }

    @Test
    fun `no text nodes returns no issues`() {
        val image = createNode(composableName = "Image", bounds = Rect(0, 0, 200, 200))
        assertTrue(rule.evaluateAll(listOf(image)).isEmpty())
    }

    @Test
    fun `non-overlapping text and image pass`() {
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 50))
        val image = createNode(composableName = "Image", bounds = Rect(150, 0, 300, 200))
        assertTrue(rule.evaluateAll(listOf(text, image)).isEmpty())
    }

    @Test
    fun `overlap of exactly 50 percent is not flagged`() {
        // text area=10000; intersection=50*100=5000; ratio=0.5; threshold is strictly greater-than
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image = createNode(composableName = "Image", bounds = Rect(50, 0, 200, 100))
        assertTrue(rule.evaluateAll(listOf(text, image)).isEmpty())
    }

    @Test
    fun `partial overlap below threshold passes`() {
        // intersection=20*100=2000; ratio=0.2 < 0.5
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image = createNode(composableName = "Image", bounds = Rect(80, 0, 200, 100))
        assertTrue(rule.evaluateAll(listOf(text, image)).isEmpty())
    }

    // --- failing cases ---

    @Test
    fun `overlap above 50 percent is flagged`() {
        // intersection=60*100=6000; ratio=0.6 > 0.5
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image = createNode(composableName = "Image", bounds = Rect(40, 0, 200, 100))
        assertEquals(1, rule.evaluateAll(listOf(text, image)).size)
    }

    @Test
    fun `text completely inside image is flagged`() {
        val text = createNode(composableName = "Text", bounds = Rect(10, 10, 90, 90))
        val image = createNode(composableName = "Image", bounds = Rect(0, 0, 100, 100))
        assertEquals(1, rule.evaluateAll(listOf(text, image)).size)
    }

    // --- edge cases ---

    @Test
    fun `only one issue per text node even when multiple images overlap it`() {
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image1 = createNode(composableName = "Image", bounds = Rect(0, 0, 100, 100))
        val image2 = createNode(composableName = "Image", bounds = Rect(0, 0, 100, 100))
        assertEquals(1, rule.evaluateAll(listOf(text, image1, image2)).size)
    }

    @Test
    fun `zero-area text node does not produce a false positive`() {
        // textArea == 0 → overlapRatio returns 0f → not flagged
        val text = createNode(composableName = "Text", bounds = Rect(50, 50, 50, 50))
        val image = createNode(composableName = "Image", bounds = Rect(0, 0, 100, 100))
        assertTrue(rule.evaluateAll(listOf(text, image)).isEmpty())
    }

    @Test
    fun `custom threshold lowers the overlap required to flag`() {
        // intersection=25*100=2500; ratio=0.25 > 0.1 threshold
        val strictRule = ImageWithTextOverlayRule(overlapThreshold = 0.1f)
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image = createNode(composableName = "Image", bounds = Rect(75, 0, 200, 100))
        assertEquals(1, strictRule.evaluateAll(listOf(text, image)).size)
    }

    @Test
    fun `adjacent bounds with no intersection do not overlap`() {
        // text right edge == image left edge → intRight==intLeft → no intersection
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image = createNode(composableName = "Image", bounds = Rect(100, 0, 200, 100))
        assertTrue(rule.evaluateAll(listOf(text, image)).isEmpty())
    }

    @Test
    fun `evaluate returns null for scan-level rule`() {
        assertNull(rule.evaluate(createNode()))
    }

    @Test
    fun `issue carries correct rule metadata`() {
        val text = createNode(composableName = "Text", bounds = Rect(0, 0, 100, 100))
        val image = createNode(composableName = "Image", bounds = Rect(0, 0, 100, 100))
        val issue = rule.evaluateAll(listOf(text, image)).first()
        assertEquals("image-text-overlay", issue.ruleId)
        assertEquals(A11ySeverity.Warning, issue.severity)
        assertEquals("WCAG 1.4.3 Contrast Minimum (Level AA)", issue.wcagReference)
    }
}
