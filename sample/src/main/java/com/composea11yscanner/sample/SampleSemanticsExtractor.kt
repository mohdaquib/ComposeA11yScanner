package com.composea11yscanner.sample

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.ui.A11yNodeExtractor
import com.composea11yscanner.ui.RenderedTextContrastAnalyzer
import kotlin.math.roundToInt

internal const val BrokenSampleContentTag = "broken-sample-content"
internal const val SampleViewportTag = "sample-viewport"

internal fun ComponentActivity.extractBrokenSampleNodes(): List<A11yNode> =
    runCatching {
        val hostView = (window.decorView as? ViewGroup)
            ?.findFirstAbstractComposeView()
            ?: return emptyList()
        val semanticsOwner = hostView.findSemanticsOwner() ?: return emptyList()
        val sampleRoot = semanticsOwner.unmergedRootSemanticsNode
            .findNodeByTestTag(BrokenSampleContentTag)
            ?: return emptyList()
        val viewport = semanticsOwner.unmergedRootSemanticsNode
            .findNodeByTestTag(SampleViewportTag)
            ?.boundsInRoot
            ?.let { Rect(it.left.roundToInt(), it.top.roundToInt(), it.right.roundToInt(), it.bottom.roundToInt()) }
            ?: sampleRoot.boundsInRoot.let {
                Rect(it.left.roundToInt(), it.top.roundToInt(), it.right.roundToInt(), it.bottom.roundToInt())
            }
        RenderedTextContrastAnalyzer(hostView)
            .analyze(A11yNodeExtractor().extract(sampleRoot))
            .filterVisibleIn(viewport)
    }.getOrDefault(emptyList())

private fun List<A11yNode>.filterVisibleIn(viewport: Rect): List<A11yNode> =
    filter { node ->
        if (node.isFocusable) {
            node.bounds.centerInside(viewport)
        } else {
            node.bounds.intersects(viewport)
        }
    }

private fun Rect.centerInside(other: Rect): Boolean {
    if (isEmpty()) return false
    val centerX = (left + right) / 2
    val centerY = (top + bottom) / 2
    return centerX in other.left..other.right && centerY in other.top..other.bottom
}

private fun Rect.intersects(other: Rect): Boolean =
    !isEmpty() &&
        right > other.left &&
        left < other.right &&
        bottom > other.top &&
        top < other.bottom

private fun SemanticsNode.findNodeByTestTag(tag: String): SemanticsNode? {
    if (config.getOrNull(SemanticsProperties.TestTag) == tag) return this
    children.forEach { child ->
        child.findNodeByTestTag(tag)?.let { return it }
    }
    return null
}

private fun ViewGroup.findFirstAbstractComposeView(): AbstractComposeView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is AbstractComposeView) return child
        if (child is ViewGroup) {
            child.findFirstAbstractComposeView()?.let { return it }
        }
    }
    return null
}

private fun AbstractComposeView.findSemanticsOwner(): SemanticsOwner? {
    val composeOwnerView: View = getChildAt(0) ?: return null
    return runCatching {
        composeOwnerView.javaClass
            .getMethod("getSemanticsOwner")
            .invoke(composeOwnerView) as? SemanticsOwner
    }.getOrNull()
}
