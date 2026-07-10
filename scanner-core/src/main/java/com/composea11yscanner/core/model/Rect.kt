package com.composea11yscanner.core.model

/**
 * Immutable pixel-coordinate bounding box in screen space.
 * Android screen coordinates: origin top-left, y increases downward.
 *
 * @property left Left edge in pixels.
 * @property top Top edge in pixels.
 * @property right Right edge in pixels.
 * @property bottom Bottom edge in pixels.
 */
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    /** Width in pixels. */
    val width: Int get() = right - left

    /** Height in pixels. */
    val height: Int get() = bottom - top

    /** Returns true when either dimension is zero or negative. */
    fun isEmpty(): Boolean = width <= 0 || height <= 0

    companion object {
        /** Empty rectangle at the origin. */
        val Zero = Rect(0, 0, 0, 0)
    }
}
