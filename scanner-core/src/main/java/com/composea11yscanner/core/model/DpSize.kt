package com.composea11yscanner.core.model

/**
 * Density-independent size (dp units). Mirrors the Compose DpSize API
 * without pulling in the Compose dependency.
 */
data class DpSize(val width: Float, val height: Float) {
    companion object {
        val Zero = DpSize(0f, 0f)
        val Unspecified = DpSize(Float.NaN, Float.NaN)
    }
}
