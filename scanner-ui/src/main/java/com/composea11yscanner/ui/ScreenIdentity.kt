package com.composea11yscanner.ui

import com.composea11yscanner.core.model.A11yNode

private const val SCREEN_IDENTITY_MAX_DEPTH = 3

/** Readable, collision-resistant identity for the currently displayed destination. */
internal data class ScreenFingerprint(
    val hostIdentity: Int,
    val destinationKey: String?,
    val shallowNodeShapes: List<SemanticNodeShape>,
)

/** Stable semantics characteristics that survive recomposition and sibling reordering. */
internal data class SemanticNodeShape(
    val depth: Int,
    val composableName: String,
    val role: String?,
    val isFocusable: Boolean,
    val isTouchTarget: Boolean,
    val isCollectionContainer: Boolean,
) : Comparable<SemanticNodeShape> {
    override fun compareTo(other: SemanticNodeShape): Int =
        compareValuesBy(
            this,
            other,
            SemanticNodeShape::depth,
            SemanticNodeShape::composableName,
            { it.role.orEmpty() },
            SemanticNodeShape::isFocusable,
            SemanticNodeShape::isTouchTarget,
            SemanticNodeShape::isCollectionContainer,
        )
}

/** Lightweight sample used to decide when initial visible semantics have settled. */
internal data class ReadinessFingerprint(
    val hostIdentity: Int,
    val visibleNodeCount: Int,
    val visibleTextNodeCount: Int,
    val visibleInteractiveNodeCount: Int,
    val visibleFocusableNodeCount: Int,
)

/**
 * Creates a stable destination identity while ignoring volatile deep content.
 *
 * An explicit navigation key is authoritative when supplied. Otherwise Fragment transitions are
 * detected by [hostIdentity] and single-host Compose transitions are inferred from the
 * order-independent shape of the shallow semantics tree. Generated semantics IDs, text, bounds,
 * and lazy descendants are deliberately excluded.
 */
internal fun calculateScreenFingerprint(
    hostIdentity: Int,
    nodes: List<A11yNode>,
    destinationKey: String? = null,
): ScreenFingerprint = ScreenFingerprint(
    hostIdentity = hostIdentity,
    destinationKey = destinationKey,
    shallowNodeShapes = if (destinationKey != null) {
        emptyList()
    } else {
        nodes.asSequence()
            .filter { it.depth <= SCREEN_IDENTITY_MAX_DEPTH }
            .map { node ->
                SemanticNodeShape(
                    depth = node.depth,
                    composableName = node.composableName,
                    role = node.role?.name,
                    isFocusable = node.isFocusable,
                    isTouchTarget = node.isTouchTarget,
                    isCollectionContainer = node.isCollectionContainer,
                )
            }
            .sorted()
            .toList()
    },
)

/** Creates an animation-resistant readiness sample from already-visible semantic nodes. */
internal fun calculateReadinessFingerprint(
    hostIdentity: Int,
    visibleNodes: List<A11yNode>,
): ReadinessFingerprint = ReadinessFingerprint(
    hostIdentity = hostIdentity,
    visibleNodeCount = visibleNodes.size,
    visibleTextNodeCount = visibleNodes.count { it.composableName == "Text" },
    visibleInteractiveNodeCount = visibleNodes.count(A11yNode::isTouchTarget),
    visibleFocusableNodeCount = visibleNodes.count(A11yNode::isFocusable),
)
