package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11yRole
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseA11yRule

class ClickableRoleRule : BaseA11yRule() {

    override val ruleId = "clickable-role"
    override val ruleName = "Clickable Role"
    override val severity = A11ySeverity.Error
    override val wcagReference = "WCAG 4.1.2 Name, Role, Value (Level A)"

    override fun check(node: A11yNode): A11yIssue? {
        if (!node.isTouchTarget) return null

        val missingRole = node.role == null
        val imageWithoutDescription =
            node.role == A11yRole.Image && node.contentDescription.isNullOrBlank()

        if (!missingRole && !imageWithoutDescription) return null

        return issue(
            node = node,
            message = "Clickable element has no semantic role. " +
                "Add Modifier.semantics { role = Role.Button }",
            howToFix = "Apply the appropriate role: Modifier.semantics { role = Role.Button } " +
                "for buttons, Role.Checkbox for toggles, Role.Image for images. " +
                "Clickable images also require a non-empty contentDescription.",
        )
    }
}
