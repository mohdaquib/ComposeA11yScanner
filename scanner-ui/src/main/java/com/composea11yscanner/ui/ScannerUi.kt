package com.composea11yscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.tooling.preview.Preview
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult

@Composable
fun A11yIssueOverlay(
    scanResult: ScanResult?,
    onIssueSelected: (A11yIssue) -> Unit = {},
    onIssuesSelected: (List<A11yIssue>) -> Unit = { issues ->
        issues.firstOrNull()?.let(onIssueSelected)
    },
    modifier: Modifier = Modifier,
    issueOffsetY: Int = 0,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = scanResult != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                scanResult?.issues
                    .orEmpty()
                    .groupBy { it.affectedNode.nodeId }
                    .values
                    .forEach { issues ->
                        val primaryIssue = issues.minWith(
                            compareBy<A11yIssue> { it.severity.sortOrder }
                                .thenBy { it.ruleId },
                        )
                        IssueHighlightBox(
                            issue = primaryIssue,
                            onIssueSelected = { onIssuesSelected(issues) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        x = primaryIssue.affectedNode.bounds.left,
                                        y = primaryIssue.affectedNode.bounds.top + issueOffsetY,
                                    )
                                },
                        )
                    }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun A11yIssueOverlayPreview() {
    val node = A11yNode(
        nodeId = "node-1",
        composableName = "Button",
        bounds = Rect(0, 0, 120, 48),
        contentDescription = null,
        isTouchTarget = true,
        touchTargetSize = DpSize(120f, 48f),
        textColor = null,
        backgroundColors = emptyList(),
        isFocusable = true,
        isMergedDescendant = false,
        depth = 1,
    )
    val result = ScanResult(
        scanId = "preview",
        timestamp = 0L,
        totalNodes = 1,
        issues = listOf(
            A11yIssue(
                issueId = "issue-1",
                severity = A11ySeverity.Warning,
                ruleId = "missing-content-description",
                ruleName = "Missing Content Description",
                affectedNode = node,
                message = "Interactive element has no content description.",
                howToFix = "Add contentDescription to the Modifier.",
                wcagReference = "WCAG 1.1.1",
            )
        ),
        passedRules = 9,
        failedRules = 1,
    )
    A11yIssueOverlay(scanResult = result)
}
