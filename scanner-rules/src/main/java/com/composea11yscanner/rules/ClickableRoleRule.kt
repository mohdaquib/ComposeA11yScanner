package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11yRole
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseA11yRule

/** Flags clickable images that do not expose an accessible description. */
class ClickableRoleRule : BaseA11yRule() {

    /** Stable id for the clickable role rule. */
    override val ruleId = "clickable-role"

    /** Human-readable rule name. */
    override val ruleName = "Clickable Role"

    /** Severity assigned to invalid clickable-role semantics. */
    override val severity = A11ySeverity.Error

    /** WCAG criterion associated with name, role, and value. */
    override val wcagReference = "WCAG 4.1.2 Name, Role, Value (Level A)"

    /** Evaluates role-specific requirements for a clickable node. */
    override fun check(node: A11yNode): A11yIssue? {
        if (node.isMergedDescendant) return null
        if (!node.isTouchTarget) return null

        // Compose intentionally permits a null role when none of its predefined roles accurately
        // describes a custom click target (for example, a clickable list row). Requiring a role for
        // every OnClick node encourages misleading Role.Button semantics and creates false positives.
        if (node.role != A11yRole.Image || !node.contentDescription.isNullOrBlank()) return null

        return issue(
            node = node,
            message = "Clickable image has no accessible description.",
            howToFix = "Provide a meaningful, non-empty contentDescription for the clickable image.",
        )
    }
}
