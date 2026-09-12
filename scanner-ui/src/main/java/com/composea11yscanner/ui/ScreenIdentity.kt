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
    val visibleNodeShapes: List<SemanticReadinessShape>,
)

/** Semantics that must stop changing before a scan can safely evaluate labels and geometry. */
internal data class SemanticReadinessShape(
    val depth: Int,
    val composableName: String,
    val bounds: com.composea11yscanner.core.model.Rect,
    val effectiveTouchBounds: com.composea11yscanner.core.model.Rect?,
    val contentDescription: String?,
    val textLabel: String?,
    val role: String?,
    val isFocusable: Boolean,
    val isTouchTarget: Boolean,
    val isEnabled: Boolean,
    val isMergedDescendant: Boolean,
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
    visibleNodeShapes = visibleNodes
        .map { node ->
            SemanticReadinessShape(
                depth = node.depth,
                composableName = node.composableName,
                bounds = node.bounds,
                effectiveTouchBounds = node.effectiveTouchBounds,
                contentDescription = node.contentDescription,
                textLabel = node.textLabel,
                role = node.role?.name,
                isFocusable = node.isFocusable,
                isTouchTarget = node.isTouchTarget,
                isEnabled = node.isEnabled,
                isMergedDescendant = node.isMergedDescendant,
            )
        }
        .sortedWith(
            compareBy<SemanticReadinessShape> { it.depth }
                .thenBy { it.composableName }
                .thenBy { it.role.orEmpty() }
                .thenBy { it.contentDescription.orEmpty() }
                .thenBy { it.textLabel.orEmpty() }
                .thenBy { it.bounds.left }
                .thenBy { it.bounds.top }
                .thenBy { it.bounds.right }
                .thenBy { it.bounds.bottom }
                .thenBy { it.effectiveTouchBounds?.left ?: Int.MIN_VALUE }
                .thenBy { it.effectiveTouchBounds?.top ?: Int.MIN_VALUE }
                .thenBy { it.effectiveTouchBounds?.right ?: Int.MIN_VALUE }
                .thenBy { it.effectiveTouchBounds?.bottom ?: Int.MIN_VALUE }
                .thenBy { it.isFocusable }
                .thenBy { it.isTouchTarget }
                .thenBy { it.isEnabled }
                .thenBy { it.isMergedDescendant },
        ),
)
