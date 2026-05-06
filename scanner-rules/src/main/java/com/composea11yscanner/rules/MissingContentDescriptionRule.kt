package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseA11yRule

class MissingContentDescriptionRule : BaseA11yRule() {

    override val ruleId = "missing-content-description"
    override val ruleName = "Missing Content Description"
    override val severity = A11ySeverity.Error
    override val wcagReference = "WCAG 1.1.1 Non-text Content (Level A)"

    override fun check(node: A11yNode): A11yIssue? {
        // Merged descendants are announced via their parent — skip them.
        if (node.isMergedDescendant) return null

        val isInteractive = node.isTouchTarget
        // Covers Image, AsyncImage, SubcomposeAsyncImage, etc.
        val isImage = node.composableName.contains("Image", ignoreCase = true)

        if (!isInteractive && !isImage) return null
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
