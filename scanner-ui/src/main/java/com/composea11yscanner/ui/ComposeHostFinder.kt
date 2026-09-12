package com.composea11yscanner.ui

import android.graphics.Rect as AndroidRect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.semantics.SemanticsOwner
import com.composea11yscanner.core.model.A11yNode

/** Finds the application Compose host while excluding the scanner's own overlay. */
internal class ComposeHostFinder {
    fun findBest(
        decorView: ViewGroup,
        excludeView: View?,
        logScores: Boolean = true,
    ): ComposeHostCandidate? {
        val candidates = mutableListOf<ComposeHostCandidate>()
        decorView.collectCandidates(excludeView, depth = 0, candidates)
        if (logScores) candidates.forEach { candidate ->
            Log.d(
                LOG_TAG,
                "Candidate score: identity=${System.identityHashCode(candidate.view)}, " +
                    "visibleTextNodes=${candidate.visibleTextNodes}, " +
                    "visibleNodes=${candidate.visibleNodes}, depth=${candidate.depth}",
            )
        }
        return candidates.maxWithOrNull(
            compareBy<ComposeHostCandidate> { it.visibleTextNodes }
                .thenBy { it.visibleNodes }
                .thenBy { it.depth },
        )
    }

    fun logHosts(decorView: ViewGroup, excludeView: View?) {
        decorView.logHosts(excludeView)
    }

    fun semanticsOwner(view: AbstractComposeView): SemanticsOwner? {
        val composeOwnerView = view.getChildAt(0) ?: return null
        return runCatching {
            composeOwnerView.javaClass.getMethod("getSemanticsOwner")
                .invoke(composeOwnerView) as? SemanticsOwner
        }.getOrNull()
    }

    private fun ViewGroup.collectCandidates(
        excludeView: View?,
        depth: Int,
        candidates: MutableList<ComposeHostCandidate>,
    ) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is AbstractComposeView && child !== excludeView && child.isViable()) {
                child.toCandidate(depth + 1)?.let(candidates::add)
            }
            if (child is ViewGroup && child !== excludeView) {
                child.collectCandidates(excludeView, depth + 1, candidates)
            }
        }
    }

    private fun AbstractComposeView.isViable(): Boolean =
        visibility == View.VISIBLE && isShown && isAttachedToWindow && isLaidOut &&
            alpha > 0f && width > 0 && height > 0

    private fun AbstractComposeView.toCandidate(depth: Int): ComposeHostCandidate? {
        val owner = semanticsOwner(this) ?: return null
        val nodes = runCatching { A11yNodeExtractor().extract(owner) }.getOrNull() ?: return null
        return ComposeHostCandidate(
            view = this,
            nodes = nodes,
            depth = depth,
            visibleSemanticNodes = nodes.filter { it.bounds.intersects(width, height) },
        )
    }

    private fun ViewGroup.logHosts(excludeView: View?, path: String = javaClass.simpleName) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val childPath = "$path/$index:${child.javaClass.simpleName}"
            if (child is AbstractComposeView) {
                Log.d(LOG_TAG, "Candidate path=$childPath, ${child.description(child === excludeView)}")
            }
            if (child is ViewGroup) child.logHosts(excludeView, childPath)
        }
    }

    companion object {
        const val LOG_TAG = "ComposeA11yHosts"
    }
}

internal data class ComposeHostCandidate(
    val view: AbstractComposeView,
    val nodes: List<A11yNode>,
    val depth: Int,
    val visibleSemanticNodes: List<A11yNode>,
) {
    val visibleTextNodes: Int get() = visibleSemanticNodes.count { it.composableName == "Text" }
    val visibleNodes: Int get() = visibleSemanticNodes.size

    fun snapshot(destinationKey: String?): ScreenSnapshot = ScreenSnapshot(
        fingerprint = calculateScreenFingerprint(
            hostIdentity = System.identityHashCode(view),
            nodes = nodes,
            destinationKey = destinationKey,
        ),
        readiness = calculateReadinessFingerprint(
            hostIdentity = System.identityHashCode(view),
            visibleNodes = visibleSemanticNodes,
        ),
    )
}

private fun AbstractComposeView.description(isExcluded: Boolean): String {
    val location = IntArray(2)
    getLocationOnScreen(location)
    val visibleRect = AndroidRect()
    val hasVisibleRect = getGlobalVisibleRect(visibleRect)
    return "identity=${System.identityHashCode(this)}, excludedOverlay=$isExcluded, " +
        "visibility=${visibility.visibilityName()}, shown=$isShown, attached=$isAttachedToWindow, " +
        "laidOut=$isLaidOut, alpha=$alpha, size=${width}x$height, position=($x,$y), " +
        "translation=($translationX,$translationY), screen=(${location[0]},${location[1]}), " +
        "hasVisibleRect=$hasVisibleRect, visibleRect=$visibleRect, childCount=$childCount"
}

private fun Int.visibilityName(): String = when (this) {
    View.VISIBLE -> "VISIBLE"
    View.INVISIBLE -> "INVISIBLE"
    View.GONE -> "GONE"
    else -> toString()
}

private fun com.composea11yscanner.core.model.Rect.intersects(width: Int, height: Int): Boolean =
    !isEmpty() && right > 0 && bottom > 0 && left < width && top < height
