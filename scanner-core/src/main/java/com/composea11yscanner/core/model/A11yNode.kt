package com.composea11yscanner.core.model

/**
 * Normalized representation of one UI semantics node scanned by the accessibility rules.
 *
 * @property nodeId Stable id for grouping issues that belong to the same semantics node.
 * @property composableName Best-effort composable or role name used in reports.
 * @property bounds Pixel bounds relative to the scanned root.
 * @property contentDescription Accessible label exposed by the node, if any.
 * @property isTouchTarget True when the node exposes a click action.
 * @property effectiveTouchBounds Effective pointer target bounds in root pixels for clickable nodes.
 * @property textColor Foreground text color when it can be extracted.
 * @property backgroundColors Candidate background colors sampled behind the node.
 * @property isFocusable True when the node can participate in focus traversal.
 * @property isMergedDescendant True when the node is inside a parent that merges semantics.
 * @property depth Depth in the semantics tree.
 * @property role Accessibility role mapped from the platform semantics role, if any.
 */
data class A11yNode(
    val nodeId: String,
    val composableName: String,
    val bounds: Rect,
    val contentDescription: String?,
    val isTouchTarget: Boolean,
    val textColor: Color?,
    val backgroundColors: List<Color>,
    val isFocusable: Boolean,
    val isMergedDescendant: Boolean,
    val depth: Int,
    val role: A11yRole? = null,
    val effectiveTouchBounds: Rect? = null,
)
