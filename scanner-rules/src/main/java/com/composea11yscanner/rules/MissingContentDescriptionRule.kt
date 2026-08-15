package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseA11yRule

/** Flags interactive or image-like nodes that do not expose a content description. */
class MissingContentDescriptionRule : BaseA11yRule() {

    /** Stable id for the missing content description rule. */
    override val ruleId = "missing-content-description"

    /** Human-readable rule name. */
    override val ruleName = "Missing Content Description"

    /** Severity assigned to missing labels. */
    override val severity = A11ySeverity.Error

    /** WCAG criterion associated with non-text content. */
    override val wcagReference = "WCAG 1.1.1 Non-text Content (Level A)"

    /** Evaluates a single node for a screen-reader label. */
    override fun check(node: A11yNode): A11yIssue? {
        // Lazy containers can retain zero-sized semantics for disposed or off-screen items.
        // They are not currently rendered or reachable, so do not report them.
        if (node.bounds.isEmpty()) return null

        val isInteractive = node.isTouchTarget
        // Covers Image, AsyncImage, SubcomposeAsyncImage, etc.
        val isImage = node.composableName.contains("Image", ignoreCase = true)

        if (!isInteractive && !isImage) return null
        // A decorative image under merging semantics is announced through its parent. An
        // interactive descendant, however, owns an action and remains an independent control;
        // suppressing it hides unlabeled IconButtons such as JetSnack's collection arrows.
        if (node.isMergedDescendant && !isInteractive) return null
        if (!node.contentDescription.isNullOrBlank()) return null

        return issue(
            node = node,
            message = "Interactive element has no content description. " +
                "Screen readers cannot announce this.",
            howToFix = "Add a meaningful contentDescription via semantics: " +
                "Modifier.semantics { contentDescription = \"Describe the action or content here\" }",
        )
    }
}
