package com.composea11yscanner.ui

import android.graphics.Bitmap
import android.view.View
import androidx.core.view.drawToBitmap
import com.composea11yscanner.core.model.Color
import com.composea11yscanner.core.model.Rect

/**
 * Samples background and foreground colors from a composable's rendered bounds.
 *
 * Strategy:
 *   - Background: 8 sample points — 4 corners + 4 edge midpoints. Nodes at the edge of a
 *     container have consistent background color there; deduplication returns only distinct
 *     candidates, so a solid background yields a single-element list.
 *   - Text/foreground: single pixel at the node's geometric center.
 *
 * [Rect] coordinates must be root-relative (matching [SemanticsNode.boundsInRoot]).
 * [rootView] must be laid out with non-zero dimensions before calling [extractColors].
 *
 * Note: [drawToBitmap] captures a software copy of the entire view on every call.
 * When sampling multiple nodes, prefer creating the bitmap once externally and calling
 * [sampleEdges]/[sampleCenter] directly with the shared [Bitmap].
 */
class ColorExtractor(private val rootView: View) {

    data class Result(
        val backgroundColors: List<Color>,
        val textColor: Color?,
    )

    fun extractColors(bounds: Rect): Result {
        if (bounds.isEmpty()) return Result(emptyList(), null)
        val bitmap = rootView.drawToBitmap()
        return try {
            Result(
                backgroundColors = sampleEdges(bitmap, bounds),
                textColor = sampleCenter(bitmap, bounds),
            )
        } finally {
            bitmap.recycle()
        }
    }

    fun sampleEdges(bitmap: Bitmap, bounds: Rect): List<Color> {
        val l = bounds.left
        val t = bounds.top
        val r = (bounds.right - 1).coerceAtLeast(l)
        val b = (bounds.bottom - 1).coerceAtLeast(t)
        val midX = (l + r) / 2
        val midY = (t + b) / 2

        return listOf(
            l    to t,    // top-left corner
            r    to t,    // top-right corner
            l    to b,    // bottom-left corner
            r    to b,    // bottom-right corner
            midX to t,    // top edge midpoint
            midX to b,    // bottom edge midpoint
            l    to midY, // left edge midpoint
            r    to midY, // right edge midpoint
        )
            .mapNotNull { (x, y) -> bitmap.safePixel(x, y) }
            .distinct()
    }

    fun sampleCenter(bitmap: Bitmap, bounds: Rect): Color? {
        val cx = (bounds.left + bounds.right) / 2
        val cy = (bounds.top + bounds.bottom) / 2
        return bitmap.safePixel(cx, cy)
    }

    // --- internal helpers ---

    private fun Bitmap.safePixel(x: Int, y: Int): Color? {
        if (x < 0 || y < 0 || x >= width || y >= height) return null
        return getPixel(x, y).toColor()
    }
}

/**
 * Converts an Android ARGB [ColorInt] to [Color] using Compose's Color.value packing:
 *   value = (argb & 0xFFFFFFFFL) shl 32
 * This keeps the sRGB color space ID (0) in bits 0-5 as zero, matching [Color.Unspecified]'s
 * convention and making round-trips with Compose's Color(Int) constructor lossless.
 */
private fun Int.toColor(): Color = Color((this.toLong() and 0xFFFFFFFFL) shl 32)
