package com.composea11yscanner.ui

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11yRole
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect
import kotlin.math.roundToInt

/**
 * Walks a Compose semantics tree and converts each [SemanticsNode] to an [A11yNode].
 *
 * Use the unmerged tree to see all rendered nodes including merged descendants:
 *   - Test: `composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()`
 *   - Production: `SemanticsOwner.rootSemanticsNode` via [extract(SemanticsOwner)]
 */
class A11yNodeExtractor(private val density: Density) {

    /**
     * Recursively extracts all nodes from the tree rooted at [rootNode].
     * Returns a flat list in depth-first order.
     */
    fun extract(rootNode: SemanticsNode): List<A11yNode> {
        val result = mutableListOf<A11yNode>()
        visit(node = rootNode, depth = 0, isParentMerging = false, result = result)
        return result
    }

    /**
     * Entry point for production use. Requires opting in to the internal Compose UI API
     * needed to access [SemanticsOwner].
     */
    @OptIn(InternalComposeUiApi::class)
    fun extract(owner: SemanticsOwner): List<A11yNode> = extract(owner.rootSemanticsNode)

    // --- recursion ---

    private fun visit(
        node: SemanticsNode,
        depth: Int,
        isParentMerging: Boolean,
        result: MutableList<A11yNode>,
    ) {
        result.add(node.toA11yNode(depth = depth, isMergedDescendant = isParentMerging))
        // Children of a merging node are merged descendants; propagate the flag downward.
        val mergingForChildren = isParentMerging || node.config.isMergingSemanticsOfDescendants
        node.children.forEach { child ->
            visit(node = child, depth = depth + 1, isParentMerging = mergingForChildren, result = result)
        }
    }

    // --- mapping ---

    private fun SemanticsNode.toA11yNode(depth: Int, isMergedDescendant: Boolean): A11yNode {
        val composeRole = config.getOrNull(SemanticsProperties.Role)
        val isTextInput = config.contains(SemanticsActions.SetText)
        val isTouchTarget = config.contains(SemanticsActions.OnClick)
        val textLabel = if (isTouchTarget || composeRole != null || isTextInput) {
            collectTextLabel()
        } else {
            null
        }
        val visualBounds = boundsInRoot
        val touchTargetBounds = layoutBoundsInRoot()
        val bounds = visualBounds.toCoreRect()

        return A11yNode(
            nodeId = id.toString(),
            composableName = resolveComposableName(
                composeRole = composeRole,
                isTouchTarget = isTouchTarget,
                textLabel = textLabel,
            ),
            bounds = bounds,
            contentDescription = config
                .getOrNull(SemanticsProperties.ContentDescription)
                ?.joinToString(separator = ", ")
                ?: textLabel,
            isTouchTarget = isTouchTarget,
            touchTargetSize = touchTargetBounds.toDpSize(),
            textColor = null,               // not available via semantics
            backgroundColors = emptyList(), // not available via semantics
            isFocusable = config.contains(SemanticsActions.OnClick)
                || config.contains(SemanticsActions.RequestFocus)
                || composeRole != null
                || isTextInput,
            isMergedDescendant = isMergedDescendant,
            depth = depth,
            role = if (isTextInput) A11yRole.TextField else composeRole?.toA11yRole(),
        )
    }

    /**
     * Resolves a human-readable composable name.
     * Priority: explicit TestTag -> inferred from Role -> inferred from Text/click semantics -> "Unknown".
     */
    private fun SemanticsNode.resolveComposableName(
        composeRole: Role?,
        isTouchTarget: Boolean,
        textLabel: String?,
    ): String =
        config.getOrNull(SemanticsProperties.TestTag)
            ?: if (config.contains(SemanticsActions.SetText)) {
                "TextField"
            } else when (composeRole) {
                Role.Button -> "Button"
                Role.Checkbox -> "Checkbox"
                Role.Switch -> "Switch"
                Role.RadioButton -> "RadioButton"
                Role.Tab -> "Tab"
                Role.Image -> "Image"
                Role.DropdownList -> "DropdownList"
                else -> {
                    val text = config.getOrNull(SemanticsProperties.Text)
                    when {
                        !text.isNullOrEmpty() -> "Text"
                        isTouchTarget && !textLabel.isNullOrBlank() -> "ClickableText"
                        isTouchTarget -> "Clickable"
                        else -> "Unknown"
                    }
                }
            }

    private fun SemanticsNode.collectTextLabel(): String? {
        val labels = mutableListOf<String>()
        collectTextLabelsInto(labels)
        return labels
            .joinToString(separator = " ")
            .takeIf { it.isNotBlank() }
    }

    private fun SemanticsNode.collectTextLabelsInto(labels: MutableList<String>) {
        config.getOrNull(SemanticsProperties.Text)
            ?.mapNotNull { it.text.takeIf(String::isNotBlank) }
            ?.let(labels::addAll)

        children.forEach { child ->
            child.collectTextLabelsInto(labels)
        }
    }

    private fun SemanticsNode.layoutBoundsInRoot(): androidx.compose.ui.geometry.Rect {
        val position = positionInRoot
        return androidx.compose.ui.geometry.Rect(
            left = position.x,
            top = position.y,
            right = position.x + layoutInfo.width,
            bottom = position.y + layoutInfo.height,
        )
    }

    private fun androidx.compose.ui.geometry.Rect.toCoreRect(): Rect = Rect(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt(),
    )

    private fun androidx.compose.ui.geometry.Rect.toDpSize(): DpSize = with(density) {
        DpSize(
            width = width.toDp().value,
            height = height.toDp().value,
        )
    }
}

// --- Role mapping ---

private fun Role.toA11yRole(): A11yRole? = when (this) {
    Role.Button -> A11yRole.Button
    Role.Checkbox -> A11yRole.Checkbox
    Role.Switch -> A11yRole.Switch
    Role.RadioButton -> A11yRole.RadioButton
    Role.Tab -> A11yRole.Tab
    Role.Image -> A11yRole.Image
    Role.DropdownList -> A11yRole.DropdownList
    else -> null // forward-compatibility: unknown future roles map to null
}
