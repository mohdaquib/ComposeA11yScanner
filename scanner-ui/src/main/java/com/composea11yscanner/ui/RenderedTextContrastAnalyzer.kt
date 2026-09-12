package com.composea11yscanner.ui

import android.graphics.Bitmap
import android.os.Build
import android.graphics.Rect as AndroidRect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.core.view.drawToBitmap
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.Color
import com.composea11yscanner.core.model.Rect
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.core.graphics.createBitmap

/** Enriches semantic Text nodes with colors measured from one rendered view capture. */
class RenderedTextContrastAnalyzer(private val rootView: View) {
    /** Returns unchanged nodes when the rendered colors cannot be measured confidently. */
    @Deprecated(
        message = "Use analyze(nodes, bitmap) with a hardware-compatible PixelCopy capture",
    )
    fun analyze(nodes: List<A11yNode>): List<A11yNode> {
        val bitmap = rootView.drawToBitmap()
        return try {
            analyze(nodes, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /** Enriches [nodes] using a rendered bitmap whose origin matches the semantics host. */
    fun analyze(nodes: List<A11yNode>, bitmap: Bitmap): List<A11yNode> {
        nodes.forEach { node ->
            Log.d(
                "TextContrast",
                "before: id=${node.nodeId}, " +
                        "name=${node.composableName}, " +
                        "bounds=${node.bounds}, " +
                        "enabled=${node.isEnabled}",
            )
        }

        val measurableNodeIds = measurableTextNodeIds(nodes)
        if (measurableNodeIds.isEmpty()) return nodes
        if (bitmap.width <= 0 || bitmap.height <= 0) return nodes

        return nodes.map { node ->
            if (node.nodeId !in measurableNodeIds) return@map node
            val colors = SolidBackgroundTextColorEstimator.estimate(bitmap, node.bounds)
                ?: return@map node

            Log.d(
                "TextContrast",
                "measured: id=${node.nodeId}, " +
                        "bounds=${node.bounds}, " +
                        "foreground=${colors.foreground}, " +
                        "background=${colors.background}",
            )

            node.copy(
                textColor = colors.foreground,
                backgroundColors = listOf(colors.background),
            )
        }
    }
}

/** Copies the rendered Compose host from the hardware-backed application window. */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun captureRenderedView(window: Window, view: View): Bitmap {
    check(view.isAttachedToWindow && view.width > 0 && view.height > 0) {
        "Cannot capture a detached or empty Compose host"
    }

    val location = IntArray(2)
    view.getLocationInWindow(location)
    val sourceRect = AndroidRect(
        location[0],
        location[1],
        location[0] + view.width,
        location[1] + view.height,
    )
    val bitmap = createBitmap(view.width, view.height)

    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        runCatching {
            PixelCopy.request(
                window,
                sourceRect,
                bitmap,
                { result ->
                    if (!continuation.isActive) {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    } else if (result == PixelCopy.SUCCESS) {
                        continuation.resume(bitmap)
                    } else {
                        if (!bitmap.isRecycled) bitmap.recycle()
                        continuation.resumeWithException(
                            IllegalStateException("PixelCopy failed with status $result"),
                        )
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }.onFailure { error ->
            if (!bitmap.isRecycled) bitmap.recycle()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
}

/**
 * Selects the tightest enabled semantic Text nodes available for pixel measurement.
 *
 * Merging containers can inherit their descendants' Text semantics while retaining bounds for the
 * entire control. Measuring such a container can mistake a selected pill, icon, or adjacent surface
 * for the text foreground. When a Text node has a Text descendant, only the descendant is measured.
 */
internal fun measurableTextNodeIds(nodes: List<A11yNode>): Set<String> {
    val nodesByParent = nodes
        .mapNotNull { node -> node.parentNodeId?.let { parentId -> parentId to node } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    fun A11yNode.hasEnabledTextDescendant(): Boolean {
        val pending = ArrayDeque(nodesByParent[nodeId].orEmpty())
        val visited = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val descendant = pending.removeFirst()
            if (!visited.add(descendant.nodeId)) continue
            if (descendant.isEnabled && descendant.composableName == "Text") return true
            pending.addAll(nodesByParent[descendant.nodeId].orEmpty())
        }
        return false
    }

    return nodes
        .asSequence()
        .filter { it.isEnabled && it.composableName == "Text" }
        .filterNot { it.hasEnabledTextDescendant() }
        // A merged target can inherit descendant text while keeping the bounds of the whole
        // control. Those bounds may contain images, icons, and container colors, so measure the
        // unmerged Text descendants instead. If none are available, skipping is safer than using
        // a surface or image color as the text foreground.
        .filterNot {
            it.hasExplicitContentDescription && !it.textLabel.isNullOrBlank()
        }
        .mapTo(mutableSetOf(), A11yNode::nodeId)
}

/** A rendered foreground/background pair suitable for WCAG contrast calculation. */
data class RenderedTextColors(
    val foreground: Color,
    val background: Color,
)

/** Conservative histogram detector for text drawn over a solid-looking background. */
internal object SolidBackgroundTextColorEstimator {
    private const val CHANNEL_SHIFT = 3
    private const val MINIMUM_PIXELS = 16
    private const val MINIMUM_BACKGROUND_SHARE = 0.45
    private const val MINIMUM_FOREGROUND_SHARE = 0.005
    private const val MINIMUM_COLOR_DISTANCE_SQUARED = 144
    private const val MINIMUM_SURFACE_AREA_SHARE = 0.02
    private const val MINIMUM_SURFACE_FILL_RATIO = 0.70

    fun estimate(bitmap: Bitmap, bounds: Rect): RenderedTextColors? {
        val left = bounds.left.coerceIn(0, bitmap.width)
        val top = bounds.top.coerceIn(0, bitmap.height)
        val right = bounds.right.coerceIn(0, bitmap.width)
        val bottom = bounds.bottom.coerceIn(0, bitmap.height)
        if (right <= left || bottom <= top) return null

        val width = right - left
        val height = bottom - top
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, left, top, width, height)
        return estimate(pixels, width)
    }

    internal fun estimate(pixels: IntArray, width: Int): RenderedTextColors? {
        if (pixels.size < MINIMUM_PIXELS || width <= 0 || pixels.size % width != 0) return null
        val clusters = buildColorClusters(pixels, width)
        val background = clusters.values.maxByOrNull { it.count } ?: return null
        if (background.count.toDouble() / pixels.size < MINIMUM_BACKGROUND_SHARE) return null

        val minimumForegroundPixels = max(2, (pixels.size * MINIMUM_FOREGROUND_SHARE).toInt())
        val foreground = clusters.values
            .asSequence()
            .filter { it.quantizedColor != background.quantizedColor }
            .filter { it.count >= minimumForegroundPixels }
            .filter {
                colorDistanceSquared(it.quantizedColor, background.quantizedColor) >=
                    MINIMUM_COLOR_DISTANCE_SQUARED
            }
            .filterNot { it.isSurfaceLike(pixels.size) }
            .maxByOrNull { it.count }
            ?: return null

        return RenderedTextColors(
            foreground = Color.fromArgb(foreground.averageArgb()),
            background = Color.fromArgb(background.averageArgb()),
        )
    }

    private fun buildColorClusters(pixels: IntArray, width: Int): Map<Int, ColorCluster> {
        val clusters = HashMap<Int, ColorCluster>()
        pixels.forEachIndexed { index, argb ->
            val quantizedColor = quantize(argb)
            clusters.getOrPut(quantizedColor) { ColorCluster(quantizedColor) }
                .add(argb = argb, x = index % width, y = index / width)
        }
        return clusters
    }

    private fun quantize(argb: Int): Int {
        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF
        return ((red ushr CHANNEL_SHIFT) shl 10) or
            ((green ushr CHANNEL_SHIFT) shl 5) or
            (blue ushr CHANNEL_SHIFT)
    }

    private fun colorDistanceSquared(first: Int, second: Int): Int {
        val red = ((first ushr 10) and 0x1F) - ((second ushr 10) and 0x1F)
        val green = ((first ushr 5) and 0x1F) - ((second ushr 5) and 0x1F)
        val blue = (first and 0x1F) - (second and 0x1F)
        return (red * red + green * green + blue * blue) shl (CHANNEL_SHIFT * 2)
    }

    private class ColorCluster(val quantizedColor: Int) {
        var count: Int = 0
            private set
        private var redSum = 0L
        private var greenSum = 0L
        private var blueSum = 0L
        private var minX = Int.MAX_VALUE
        private var minY = Int.MAX_VALUE
        private var maxX = Int.MIN_VALUE
        private var maxY = Int.MIN_VALUE

        fun add(argb: Int, x: Int, y: Int) {
            count++
            redSum += (argb ushr 16) and 0xFF
            greenSum += (argb ushr 8) and 0xFF
            blueSum += argb and 0xFF
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
        }

        fun averageArgb(): Int =
            (0xFF shl 24) or
                ((redSum / count).toInt() shl 16) or
                ((greenSum / count).toInt() shl 8) or
                (blueSum / count).toInt()

        fun isSurfaceLike(totalPixels: Int): Boolean {
            val boundingArea = (maxX - minX + 1) * (maxY - minY + 1)
            val areaShare = boundingArea.toDouble() / totalPixels
            val fillRatio = count.toDouble() / boundingArea
            return areaShare >= MINIMUM_SURFACE_AREA_SHARE &&
                fillRatio >= MINIMUM_SURFACE_FILL_RATIO
        }
    }
}
