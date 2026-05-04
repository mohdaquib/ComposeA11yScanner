package com.composea11yscanner.core.model

/**
 * Immutable pixel-coordinate bounding box in screen space.
 * Android screen coordinates: origin top-left, y increases downward.
 */
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun isEmpty(): Boolean = width <= 0 || height <= 0

    companion object {
        val Zero = Rect(0, 0, 0, 0)
    }
}
