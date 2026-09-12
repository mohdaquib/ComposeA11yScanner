package com.composea11yscanner.ui

/**
 * Requires a semantic readiness fingerprint to remain unchanged across both a minimum number of
 * observations and a minimum amount of time. A single matching pair is not sufficient because
 * asynchronous content and Compose animations can pause briefly between updates.
 */
internal class SemanticStabilityTracker(
    private val requiredStableSamples: Int,
    private val minimumStableDurationMillis: Long,
) {
    private var lastReadiness: ReadinessFingerprint? = null
    private var stableSampleCount = 0
    private var stableSinceMillis = 0L

    fun observe(readiness: ReadinessFingerprint, nowMillis: Long): Boolean {
        if (readiness != lastReadiness) {
            lastReadiness = readiness
            stableSampleCount = 1
            stableSinceMillis = nowMillis
            return false
        }

        stableSampleCount++
        return stableSampleCount >= requiredStableSamples &&
            nowMillis - stableSinceMillis >= minimumStableDurationMillis
    }

    fun reset() {
        lastReadiness = null
        stableSampleCount = 0
        stableSinceMillis = 0L
    }
}
