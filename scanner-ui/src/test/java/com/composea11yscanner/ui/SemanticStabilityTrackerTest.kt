package com.composea11yscanner.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticStabilityTrackerTest {

    @Test
    fun `requires consecutive samples and minimum stable duration`() {
        val tracker = SemanticStabilityTracker(
            requiredStableSamples = 4,
            minimumStableDurationMillis = 750L,
        )
        val readiness = readiness(visibleNodeCount = 10)

        assertFalse(tracker.observe(readiness, nowMillis = 0L))
        assertFalse(tracker.observe(readiness, nowMillis = 250L))
        assertFalse(tracker.observe(readiness, nowMillis = 500L))
        assertTrue(tracker.observe(readiness, nowMillis = 750L))
    }

    @Test
    fun `semantic change restarts the stable window`() {
        val tracker = SemanticStabilityTracker(
            requiredStableSamples = 3,
            minimumStableDurationMillis = 500L,
        )
        val loading = readiness(visibleNodeCount = 2)
        val content = readiness(visibleNodeCount = 10)

        assertFalse(tracker.observe(loading, nowMillis = 0L))
        assertFalse(tracker.observe(loading, nowMillis = 250L))
        assertFalse(tracker.observe(content, nowMillis = 500L))
        assertFalse(tracker.observe(content, nowMillis = 750L))
        assertTrue(tracker.observe(content, nowMillis = 1_000L))
    }

    @Test
    fun `reset discards previously stable observations`() {
        val tracker = SemanticStabilityTracker(
            requiredStableSamples = 2,
            minimumStableDurationMillis = 100L,
        )
        val readiness = readiness(visibleNodeCount = 10)

        assertFalse(tracker.observe(readiness, nowMillis = 0L))
        assertTrue(tracker.observe(readiness, nowMillis = 100L))
        tracker.reset()
        assertFalse(tracker.observe(readiness, nowMillis = 200L))
    }

    private fun readiness(visibleNodeCount: Int): ReadinessFingerprint = ReadinessFingerprint(
        hostIdentity = 1,
        visibleNodeCount = visibleNodeCount,
        visibleTextNodeCount = visibleNodeCount,
        visibleInteractiveNodeCount = 0,
        visibleFocusableNodeCount = 0,
        visibleNodeShapes = emptyList(),
    )
}
