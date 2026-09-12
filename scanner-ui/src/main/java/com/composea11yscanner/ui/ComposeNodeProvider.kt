package com.composea11yscanner.ui

import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.InternalComposeUiApi

/** Provides merged TalkBack nodes and unmerged rendered-text nodes for their respective rules. */
internal class ComposeNodeProvider(
    private val activity: ComponentActivity,
    private val overlayViewProvider: () -> androidx.compose.ui.platform.ComposeView?,
    private val hostFinder: ComposeHostFinder,
) {
    suspend fun mergedNodes(): List<com.composea11yscanner.core.model.A11yNode> = runCatching {
        val decor = activity.window.decorView as? ViewGroup ?: return emptyList()
        hostFinder.logHosts(decor, overlayViewProvider())
        val host = hostFinder.findBest(decor, overlayViewProvider()) ?: return emptyList()
        Log.d(
            ComposeHostFinder.LOG_TAG,
            "Selected host: identity=${System.identityHashCode(host.view)}, " +
                "visibleTextNodes=${host.visibleTextNodes}, visibleNodes=${host.visibleNodes}, " +
                "depth=${host.depth}",
        )
        host.nodes
    }.onFailure {
        Log.e(ComposeHostFinder.LOG_TAG, "Failed to extract Compose semantics", it)
    }.getOrDefault(emptyList())

    @OptIn(InternalComposeUiApi::class)
    suspend fun contrastNodes(): List<com.composea11yscanner.core.model.A11yNode> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.i(
                ComposeHostFinder.LOG_TAG,
                "Rendered text contrast requires API 26 or newer; semantic rules continue without it",
            )
            return emptyList()
        }
        val decor = activity.window.decorView as? ViewGroup ?: return emptyList()
        val host = hostFinder.findBest(decor, overlayViewProvider(), logScores = false)
            ?: return emptyList()
        val owner = hostFinder.semanticsOwner(host.view) ?: return emptyList()
        val nodes = A11yNodeExtractor().extract(owner.unmergedRootSemanticsNode)
        val bitmap = runCatching { captureRenderedView(activity.window, host.view) }
            .onFailure {
                Log.w(ComposeHostFinder.LOG_TAG, "Rendered pixel capture failed; contrast unavailable", it)
            }.getOrNull() ?: return emptyList()
        return try {
            runCatching { RenderedTextContrastAnalyzer(host.view).analyze(nodes, bitmap) }
                .onFailure {
                    Log.w(ComposeHostFinder.LOG_TAG, "Rendered text contrast analysis failed", it)
                }.getOrDefault(emptyList())
        } finally {
            bitmap.recycle()
        }
    }
}

internal class ScreenSnapshotProvider(
    private val activity: ComponentActivity,
    private val overlayViewProvider: () -> androidx.compose.ui.platform.ComposeView?,
    private val destinationKeyProvider: (() -> String?)?,
    private val hostFinder: ComposeHostFinder,
) {
    fun current(): ScreenSnapshot? {
        val decor = activity.window.decorView as? ViewGroup ?: return null
        val candidate = hostFinder.findBest(decor, overlayViewProvider(), logScores = false)
            ?: return null
        if (candidate.nodes.none { it.depth > 0 }) return null
        val destinationKey = destinationKeyProvider?.let { provider ->
            runCatching(provider).onFailure {
                Log.w("ComposeA11yLifecycle", "Destination key provider failed", it)
            }.getOrNull()
        }
        return candidate.snapshot(destinationKey)
    }
}
