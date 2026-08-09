package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchTargetOverlapRuleTest {

    private val rule = TouchTargetOverlapRule()

    @Test
    fun `overlapping effective targets report each affected node once`() {
        val first = target("first", Rect(0, 0, 48, 48), Rect(0, 0, 48, 48))
        val second = target("second", Rect(40, 0, 88, 48), Rect(40, 0, 88, 48))

        val issues = rule.evaluateAll(listOf(first, second))

        assertEquals(2, issues.size)
        assertEquals(setOf("first", "second"), issues.map { it.affectedNode.nodeId }.toSet())
    }

    @Test
    fun `adjacent targets that only share an edge pass`() {
        val first = target("first", Rect(0, 0, 48, 48), Rect(0, 0, 48, 48))
        val second = target("second", Rect(48, 0, 96, 48), Rect(48, 0, 96, 48))

        assertTrue(rule.evaluateAll(listOf(first, second)).isEmpty())
    }

    @Test
    fun `non-interactive merged and missing bounds nodes are ignored`() {
        val valid = target("valid", Rect(0, 0, 48, 48), Rect(0, 0, 48, 48))
        val nonInteractive = createNode(
            nodeId = "non-interactive",
            isTouchTarget = false,
            effectiveTouchBounds = Rect(0, 0, 48, 48),
        )
        val merged = target(
            "merged",
            Rect(0, 0, 48, 48),
            Rect(0, 0, 48, 48),
            isMergedDescendant = true,
        )
        val missingBounds = createNode(nodeId = "missing", isTouchTarget = true)

        assertTrue(rule.evaluateAll(listOf(valid, nonInteractive, merged, missingBounds)).isEmpty())
    }

    @Test
    fun `one node overlapping multiple targets produces one aggregated issue`() {
        val center = target("center", Rect(20, 0, 68, 48), Rect(20, 0, 68, 48))
        val left = target("left", Rect(0, 0, 40, 48), Rect(0, 0, 40, 48))
        val right = target("right", Rect(60, 0, 108, 48), Rect(60, 0, 108, 48))

        val centerIssue = rule.evaluateAll(listOf(center, left, right))
            .single { it.affectedNode.nodeId == "center" }

        assertTrue(centerIssue.message.contains("2 other targets"))
        assertEquals(A11ySeverity.Warning, centerIssue.severity)
        assertEquals(null, centerIssue.wcagReference)
    }

    @Test
    fun `expanded touch bounds do not flag visually separate controls`() {
        val first = target(
            "first",
            visualBounds = Rect(0, 0, 40, 40),
            effectiveBounds = Rect(0, 0, 48, 48),
        )
        val second = target(
            "second",
            visualBounds = Rect(40, 0, 80, 40),
            effectiveBounds = Rect(32, 0, 80, 48),
        )

        assertTrue(rule.evaluateAll(listOf(first, second)).isEmpty())
    }

    @Test
    fun `identical shared-transition targets are treated as one logical control`() {
        val original = target(
            "original",
            visualBounds = Rect(0, 0, 170, 250),
            effectiveBounds = Rect(0, 0, 170, 250),
        ).copy(contentDescription = "Cupcake A tag line")
        val transitionCopy = original.copy(nodeId = "transition-copy")

        assertTrue(rule.evaluateAll(listOf(original, transitionCopy)).isEmpty())
    }

    private fun target(
        id: String,
        visualBounds: Rect,
        effectiveBounds: Rect,
        isMergedDescendant: Boolean = false,
    ) = createNode(
        nodeId = id,
        bounds = visualBounds,
        isTouchTarget = true,
        effectiveTouchBounds = effectiveBounds,
        isMergedDescendant = isMergedDescendant,
    )
}
