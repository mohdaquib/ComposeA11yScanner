package com.composea11yscanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class A11yNodeExtractorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickableContainerWithDescendantText_resolvesAsClickableText() {
        composeRule.setContent {
            Box(modifier = Modifier.clickable { }) {
                Text("Go")
            }
        }

        composeRule.waitForIdle()

        val nodes = A11yNodeExtractor(Density(1f))
            .extract(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        val clickableNode = nodes.single { it.isTouchTarget && it.contentDescription == "Go" }

        assertEquals("ClickableText", clickableNode.composableName)
    }

    @Test
    fun unlabeledClickable_resolvesAsClickable() {
        composeRule.setContent {
            Box(modifier = Modifier.clickable { })
        }

        composeRule.waitForIdle()

        val nodes = A11yNodeExtractor(Density(1f))
            .extract(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        val clickableNode = nodes.single { it.isTouchTarget }

        assertEquals("Clickable", clickableNode.composableName)
        assertTrue(clickableNode.contentDescription.isNullOrBlank())
    }

    @Test
    fun compactClickable_usesExpandedTouchBounds() {
        lateinit var density: Density
        composeRule.setContent {
            density = LocalDensity.current
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 48.dp)
                    .clickable { },
            )
        }

        composeRule.waitForIdle()

        val nodes = A11yNodeExtractor(density)
            .extract(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        val clickableNode = nodes.single { it.isTouchTarget }

        assertTrue(clickableNode.touchTargetSize.width >= 48f)
        assertTrue(clickableNode.touchTargetSize.height >= 48f)
    }
}
