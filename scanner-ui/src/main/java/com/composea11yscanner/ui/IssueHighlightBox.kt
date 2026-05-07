package com.composea11yscanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect

private val ErrorBorderColor = Color(0xFFD32F2F)
private val WarningBorderColor = Color(0xFFFFA000)
private val InfoBorderColor = Color(0xFF1976D2)

private val BorderStrokeWidth = 2.dp
private val BorderCornerRadius = 4.dp

private fun A11ySeverity.toBorderColor(): Color = when (this) {
    A11ySeverity.Error -> ErrorBorderColor
    A11ySeverity.Warning -> WarningBorderColor
    A11ySeverity.Info -> InfoBorderColor
}

@Composable
fun IssueHighlightBox(
    issue: A11yIssue,
    onIssueSelected: (A11yIssue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bounds = issue.affectedNode.bounds
    val width = with(density) { bounds.width.toDp() }
    val height = with(density) { bounds.height.toDp() }
    val borderColor = issue.severity.toBorderColor()

    Spacer(
        modifier = modifier
            .size(width, height)
            .clickable { onIssueSelected(issue) }
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = BorderStrokeWidth.toPx()),
                    cornerRadius = CornerRadius(BorderCornerRadius.toPx()),
                )
            },
    )
}

@Preview(showBackground = true)
@Composable
private fun IssueHighlightBoxErrorPreview() {
    IssueHighlightBox(
        issue = previewIssue(severity = A11ySeverity.Error),
        onIssueSelected = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun IssueHighlightBoxWarningPreview() {
    IssueHighlightBox(
        issue = previewIssue(severity = A11ySeverity.Warning),
        onIssueSelected = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun IssueHighlightBoxInfoPreview() {
    IssueHighlightBox(
        issue = previewIssue(severity = A11ySeverity.Info),
        onIssueSelected = {},
    )
}

private fun previewIssue(severity: A11ySeverity) = A11yIssue(
    issueId = "preview-1",
    severity = severity,
    ruleId = "preview-rule",
    ruleName = "Preview Rule",
    affectedNode = A11yNode(
        nodeId = "node-1",
        composableName = "Button",
        bounds = Rect(0, 0, 300, 150),
        contentDescription = null,
        isTouchTarget = true,
        touchTargetSize = DpSize(100f, 50f),
        textColor = null,
        backgroundColors = emptyList(),
        isFocusable = true,
        isMergedDescendant = false,
        depth = 1,
    ),
    message = "Missing content description",
    howToFix = "Add contentDescription to the Modifier.",
    wcagReference = "WCAG 1.1.1",
)
