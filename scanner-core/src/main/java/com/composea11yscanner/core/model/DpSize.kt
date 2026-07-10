package com.composea11yscanner.core.model

/**
 * Density-independent size in dp units. Mirrors the Compose DpSize API
 * without pulling in the Compose dependency.
 *
 * @property width Width in dp.
 * @property height Height in dp.
 */
data class DpSize(val width: Float, val height: Float) {
    companion object {
        /** A zero-width and zero-height size. */
        val Zero = DpSize(0f, 0f)

        /** Sentinel used when a size could not be measured. */
        val Unspecified = DpSize(Float.NaN, Float.NaN)
    }
}
