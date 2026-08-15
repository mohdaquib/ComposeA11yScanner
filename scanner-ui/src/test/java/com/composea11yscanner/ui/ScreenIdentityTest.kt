package com.composea11yscanner.ui

import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScreenIdentityTest {

    @Test
    fun `deep dynamic content does not change screen fingerprint`() {
        val original = listOf(node("root", depth = 0), node("message-1", depth = 5))
        val updated = listOf(node("root", depth = 0), node("message-2", depth = 5))

        assertEquals(
            calculateScreenFingerprint(hostIdentity = 1, nodes = original),
            calculateScreenFingerprint(hostIdentity = 1, nodes = updated),
        )
    }

    @Test
    fun `semantics ids recreated by recomposition do not change screen fingerprint`() {
        val original = listOf(node("root-1", depth = 0), node("content-1", depth = 2, name = "Text"))
        val recomposed = listOf(node("root-9", depth = 0), node("content-7", depth = 2, name = "Text"))

        assertEquals(
            calculateScreenFingerprint(hostIdentity = 1, nodes = original),
            calculateScreenFingerprint(hostIdentity = 1, nodes = recomposed),
        )
    }

    @Test
    fun `shallow compose destination shape produces new screen fingerprint`() {
        val conversation = listOf(node("root", depth = 0), node("messages", depth = 2, name = "Text"))
        val profile = listOf(node("root", depth = 0), node("profile-action", depth = 2, name = "Button"))

        assertNotEquals(
            calculateScreenFingerprint(hostIdentity = 1, nodes = conversation),
            calculateScreenFingerprint(hostIdentity = 1, nodes = profile),
        )
    }

    @Test
    fun `explicit destination key detects structurally identical compose screens`() {
        val nodes = listOf(node("root", depth = 0), node("content", depth = 2, name = "Text"))

        assertNotEquals(
            calculateScreenFingerprint(hostIdentity = 1, nodes = nodes, destinationKey = "home"),
            calculateScreenFingerprint(hostIdentity = 1, nodes = nodes, destinationKey = "search"),
        )
    }

    @Test
    fun `compose host replacement produces new screen fingerprint`() {
        val nodes = listOf(node("root", depth = 0))

        assertNotEquals(
            calculateScreenFingerprint(hostIdentity = 1, nodes = nodes),
            calculateScreenFingerprint(hostIdentity = 2, nodes = nodes),
        )
    }

    @Test
    fun `shallow sibling reordering does not change screen fingerprint`() {
        val original = listOf(
            node("root", depth = 0),
            node("title", depth = 2, name = "Text"),
            node("action", depth = 2, name = "Button"),
        )
        val reordered = listOf(original[0], original[2], original[1])

        assertEquals(
            calculateScreenFingerprint(hostIdentity = 1, nodes = original),
            calculateScreenFingerprint(hostIdentity = 1, nodes = reordered),
        )
    }

    @Test
    fun `readiness ignores ids and exact bounds but observes visible node counts`() {
        val first = listOf(node("one", depth = 1), node("two", depth = 2, name = "Text"))
        val animated = listOf(
            node("nine", depth = 1, bounds = Rect(10, 10, 90, 90)),
            node("ten", depth = 2, name = "Text", bounds = Rect(20, 20, 80, 80)),
        )
        val populated = animated + node("button", depth = 4, name = "Button", isTouchTarget = true)

        assertEquals(
            calculateReadinessFingerprint(hostIdentity = 1, visibleNodes = first),
            calculateReadinessFingerprint(hostIdentity = 1, visibleNodes = animated),
        )
        assertNotEquals(
            calculateReadinessFingerprint(hostIdentity = 1, visibleNodes = animated),
            calculateReadinessFingerprint(hostIdentity = 1, visibleNodes = populated),
        )
    }

    private fun node(
        id: String,
        depth: Int,
        name: String = "Unknown",
        bounds: Rect = Rect(0, 0, 100, 100),
        isTouchTarget: Boolean = false,
    ): A11yNode = A11yNode(
        nodeId = id,
        composableName = name,
        bounds = bounds,
        contentDescription = null,
        isTouchTarget = isTouchTarget,
        textColor = null,
        backgroundColors = emptyList(),
        isFocusable = isTouchTarget,
        isMergedDescendant = false,
        depth = depth,
    )
}
