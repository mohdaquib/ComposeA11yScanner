package com.composea11yscanner.ui

import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class RenderedTextContrastAnalyzerTest {

    @Test
    fun `merged text container is skipped when tighter text descendant exists`() {
        val navigationItem = nodeFixture().copy(
            nodeId = "home-item",
            composableName = "Text",
            bounds = Rect(76, 2049, 349, 2181),
            parentNodeId = "navigation-bar",
        )
        val homeLabel = nodeFixture().copy(
            nodeId = "home-label",
            composableName = "Text",
            bounds = Rect(177, 2084, 283, 2130),
            parentNodeId = navigationItem.nodeId,
            isTouchTarget = false,
            isFocusable = false,
            isMergedDescendant = true,
        )

        assertEquals(
            setOf(homeLabel.nodeId),
            measurableTextNodeIds(listOf(navigationItem, homeLabel)),
        )
    }

    @Test
    fun `standalone clickable text remains measurable`() {
        val clickableText = nodeFixture().copy(
            nodeId = "terms-link",
            composableName = "Text",
            isTouchTarget = true,
        )

        assertEquals(
            setOf(clickableText.nodeId),
            measurableTextNodeIds(listOf(clickableText)),
        )
    }

    @Test
    fun `disabled text descendants do not suppress enabled parent measurement`() {
        val parent = nodeFixture().copy(
            nodeId = "parent",
            composableName = "Text",
        )
        val disabledChild = nodeFixture().copy(
            nodeId = "disabled-child",
            composableName = "Text",
            parentNodeId = parent.nodeId,
            isEnabled = false,
        )

        assertEquals(
            setOf(parent.nodeId),
            measurableTextNodeIds(listOf(parent, disabledChild)),
        )
    }

    @Test
    fun `merged described control is skipped when leaf text bounds are unavailable`() {
        val mergedBookCard = nodeFixture().copy(
            nodeId = "book-card",
            composableName = "Text",
            contentDescription = "Book cover image",
            hasExplicitContentDescription = true,
            textLabel = "Moby Dick Herman Melville English",
            isTouchTarget = true,
            isFocusable = true,
            bounds = Rect(33, 283, 1047, 779),
        )

        assertEquals(
            emptySet<String>(),
            measurableTextNodeIds(listOf(mergedBookCard)),
        )
    }
}
