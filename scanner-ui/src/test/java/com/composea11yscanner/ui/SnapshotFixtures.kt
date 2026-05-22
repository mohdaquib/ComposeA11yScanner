package com.composea11yscanner.ui

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult

internal fun issueFixture(
    severity: A11ySeverity,
    issueId: String = "snapshot-1",
    ruleName: String = "Missing Content Description",
    message: String = "Interactive element has no content description.",
    howToFix: String = "Add Modifier.semantics { contentDescription = \"Describe action\" }",
    wcagReference: String? = "WCAG 1.1.1 Non-text Content (Level A)",
): A11yIssue = A11yIssue(
    issueId = issueId,
    severity = severity,
    ruleId = "snapshot-rule",
    ruleName = ruleName,
    affectedNode = nodeFixture(),
    message = message,
    howToFix = howToFix,
    wcagReference = wcagReference,
)

internal fun nodeFixture() = A11yNode(
    nodeId = "node-1",
    composableName = "Button",
    bounds = Rect(left = 0, top = 0, right = 300, bottom = 120),
    contentDescription = null,
    isTouchTarget = true,
    touchTargetSize = DpSize(width = 100f, height = 40f),
    textColor = null,
    backgroundColors = emptyList(),
    isFocusable = true,
    isMergedDescendant = false,
    depth = 1,
)

internal fun scanResultFixture(errors: Int, warnings: Int, info: Int): ScanResult {
    val issues = buildList {
        repeat(errors) { i ->
            add(
                issueFixture(
                    severity = A11ySeverity.Error,
                    issueId = "err-$i",
                    ruleName = "Missing Content Description",
                    wcagReference = "WCAG 1.1.1 Non-text Content (Level A)",
                )
            )
        }
        repeat(warnings) { i ->
            add(
                issueFixture(
                    severity = A11ySeverity.Warning,
                    issueId = "warn-$i",
                    ruleName = "Focus Order",
                    message = "Focus jumps upward unexpectedly.",
                    howToFix = "Reorder composables so focus flows top-to-bottom.",
                    wcagReference = "WCAG 2.4.3 Focus Order (Level A)",
                )
            )
        }
        repeat(info) { i ->
            add(
                issueFixture(
                    severity = A11ySeverity.Info,
                    issueId = "info-$i",
                    ruleName = "Text Scaling",
                    message = "Text does not scale with system font size.",
                    howToFix = "Use sp units for all text sizes.",
                    wcagReference = null,
                )
            )
        }
    }
    val totalRules = errors + warnings + info + 12
    return ScanResult(
        scanId = "snapshot",
        timestamp = 0L,
        totalNodes = 24,
        issues = issues,
        passedRules = totalRules - (errors + warnings + info),
        failedRules = errors + warnings + info,
    )
}
