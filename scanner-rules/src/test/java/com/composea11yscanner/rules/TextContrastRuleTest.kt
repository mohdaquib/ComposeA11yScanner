package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextContrastRuleTest {

    private val rule = TextContrastRule(minimumRatio = 4.5f)

    @Test
    fun `black on white has maximum contrast`() {
        assertEquals(21f, contrastRatio(color(0xFF000000), color(0xFFFFFFFF)), 0.01f)
    }

    @Test
    fun `low contrast semantic text produces warning`() {
        val issue = rule.evaluate(
            createNode(
                composableName = "Text",
                textColor = color(0xFF9E9E9E),
                backgroundColors = listOf(color(0xFFFFFFFF)),
            ),
        )

        requireNotNull(issue)
        assertEquals(A11ySeverity.Warning, issue.severity)
        assertTrue(issue.message.contains("2.68:1"))
    }

    @Test
    fun `high contrast semantic text passes`() {
        val issue = rule.evaluate(
            createNode(
                composableName = "Text",
                textColor = color(0xFF212121),
                backgroundColors = listOf(color(0xFFFFFFFF)),
            ),
        )

        assertNull(issue)
    }

    @Test
    fun `unmeasured and non-text nodes are skipped`() {
        assertNull(rule.evaluate(createNode(composableName = "Text")))
        assertNull(
            rule.evaluate(
                createNode(
                    composableName = "Button",
                    textColor = color(0xFF9E9E9E),
                    backgroundColors = listOf(color(0xFFFFFFFF)),
                ),
            ),
        )
    }

    @Test
    fun `disabled text is skipped`() {
        assertNull(
            rule.evaluate(
                createNode(
                    composableName = "Text",
                    textColor = color(0xFFB0B0B0),
                    backgroundColors = listOf(color(0xFFFFFFFF)),
                    isEnabled = false,
                ),
            ),
        )
    }

    private fun color(argb: Long): Color = Color.fromArgb(argb.toInt())
}
