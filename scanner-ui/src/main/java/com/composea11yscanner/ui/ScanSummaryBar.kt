package com.composea11yscanner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult
import com.composea11yscanner.core.model.ScannerState

// ─────────────────────────────────────────────────────────────────────────────
// Colour constants
// ─────────────────────────────────────────────────────────────────────────────

private val ErrorColor = Color(0xFFD32F2F)
private val WarningColor = Color(0xFFFFA000)
private val InfoColor = Color(0xFF1976D2)

private val ScoreGoodColor = Color(0xFF2E7D32)  // ≥ 90 %
private val ScoreFairColor = Color(0xFFF57C00)  // ≥ 70 %
private val ScorePoorColor = Color(0xFFC62828)  // < 70 %

private val SeverityOrder = listOf(A11ySeverity.Error, A11ySeverity.Warning, A11ySeverity.Info)

// ─────────────────────────────────────────────────────────────────────────────
// Extension helpers — file-private to avoid leaking scanner-core ↔ UI coupling
// ─────────────────────────────────────────────────────────────────────────────

private fun A11ySeverity.toColor(): Color = when (this) {
    A11ySeverity.Error -> ErrorColor
    A11ySeverity.Warning -> WarningColor
    A11ySeverity.Info -> InfoColor
}

private fun A11ySeverity.toIcon(): ImageVector = when (this) {
    A11ySeverity.Error -> Icons.Filled.Error
    A11ySeverity.Warning -> Icons.Filled.Warning
    A11ySeverity.Info -> Icons.Filled.Info
}

private fun A11ySeverity.label(): String = when (this) {
    A11ySeverity.Error -> "Error"
    A11ySeverity.Warning -> "Warning"
    A11ySeverity.Info -> "Info"
}

private fun Float.toScoreColor(): Color = when {
    this >= 90f -> ScoreGoodColor
    this >= 70f -> ScoreFairColor
    else -> ScorePoorColor
}

// ─────────────────────────────────────────────────────────────────────────────
// Public composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Top bar that shows scan progress while a scan is running and a summary of
 * findings once it completes. Tapping the score chip opens [ScanReportSheet].
 *
 * Callers are responsible for showing/hiding the bar itself; this composable
 * renders content for [ScannerState.Scanning] and [ScannerState.Complete] and
 * nothing for [ScannerState.Idle] and [ScannerState.Error].
 */
@Composable
fun ScanSummaryBar(
    state: ScannerState,
    modifier: Modifier = Modifier,
) {
    var showReport by remember { mutableStateOf(false) }

    // Retain last complete result so the sheet stays populated when state rolls
    // back to Idle while the sheet is still open.
    var lastResult by remember { mutableStateOf<ScanResult?>(null) }
    LaunchedEffect(state) {
        if (state is ScannerState.Complete) lastResult = state.result
    }

    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        // contentKey groups Scanning(0.1f) and Scanning(0.9f) under the same
        // key so the progress bar isn't cross-faded on every progress tick —
        // the fade only fires on state-category transitions (e.g. Scanning →
        // Complete).
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { s ->
                when (s) {
                    is ScannerState.Scanning -> "scanning"
                    is ScannerState.Complete -> "complete"
                    else -> "other"
                }
            },
            label = "scan-state",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) { currentState ->
            when (currentState) {
                is ScannerState.Scanning -> ScanningContent(progress = currentState.progress)
                is ScannerState.Complete -> ScanCompleteContent(
                    result = currentState.result,
                    onScoreClick = { showReport = true },
                )
                is ScannerState.Error -> ErrorContent(message = currentState.message)
                ScannerState.Idle -> Unit
            }
        }
    }

    if (showReport) {
        lastResult?.let { result ->
            ScanReportSheet(
                result = result,
                onDismiss = { showReport = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bar content slots
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanningContent(progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Scanning accessibility…",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScanCompleteContent(
    result: ScanResult,
    onScoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (result.errorCount > 0) {
            CountChip(
                count = result.errorCount,
                color = ErrorColor,
                icon = Icons.Filled.Error,
                contentDescription = "${result.errorCount} errors",
            )
        }
        if (result.warningCount > 0) {
            CountChip(
                count = result.warningCount,
                color = WarningColor,
                icon = Icons.Filled.Warning,
                contentDescription = "${result.warningCount} warnings",
            )
        }
        if (result.infoCount > 0) {
            CountChip(
                count = result.infoCount,
                color = InfoColor,
                icon = Icons.Filled.Info,
                contentDescription = "${result.infoCount} info items",
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        ScoreChip(score = result.overallScore, onClick = onScoreClick)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Scan failed: $message",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip atoms used in the bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CountChip(
    count: Int,
    color: Color,
    icon: ImageVector,
    contentDescription: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ScoreChip(score: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(score.toScoreColor())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${score.toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = "View full report",
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Report sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanReportSheet(
    result: ScanResult,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val grouped = remember(result) { result.issues.groupBy { it.severity } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                ReportScoreHeader(result = result)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (result.issues.isEmpty()) {
                item {
                    Text(
                        text = "No issues found — all checks passed.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            } else {
                SeverityOrder.forEach { severity ->
                    val issues = grouped[severity] ?: return@forEach
                    item(key = "header-${severity.label()}") {
                        SeverityGroupHeader(severity = severity, count = issues.size)
                    }
                    items(issues, key = { it.issueId }) { issue ->
                        IssueReportRow(issue = issue)
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportScoreHeader(result: ScanResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Accessibility Score",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${result.totalNodes} nodes scanned · ${result.passedRules} rules passed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${result.overallScore.toInt()}%",
                style = MaterialTheme.typography.displaySmall,
                color = result.overallScore.toScoreColor(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result.errorCount > 0) {
                CountChip(result.errorCount, ErrorColor, Icons.Filled.Error, "${result.errorCount} errors")
            }
            if (result.warningCount > 0) {
                CountChip(result.warningCount, WarningColor, Icons.Filled.Warning, "${result.warningCount} warnings")
            }
            if (result.infoCount > 0) {
                CountChip(result.infoCount, InfoColor, Icons.Filled.Info, "${result.infoCount} info items")
            }
        }
    }
}

@Composable
private fun SeverityGroupHeader(severity: A11ySeverity, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = severity.toIcon(),
                contentDescription = null,
                tint = severity.toColor(),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = severity.label(),
                style = MaterialTheme.typography.labelMedium,
                color = severity.toColor(),
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IssueReportRow(issue: A11yIssue) {
    val barColor = issue.severity.toColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // 3dp coloured left border drawn outside padding so content
                // starts cleanly 12dp from the bar edge.
                drawRect(color = barColor, size = size.copy(width = 3.dp.toPx()))
            }
            .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = issue.ruleName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = issue.affectedNode.composableName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = issue.message,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ScanSummaryBarScanningPreview() {
    MaterialTheme {
        ScanSummaryBar(state = ScannerState.Scanning(progress = 0.45f))
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanSummaryBarCompletePreview() {
    MaterialTheme {
        ScanSummaryBar(
            state = ScannerState.Complete(
                result = previewResult(
                    errors = 2, warnings = 5, info = 1, passed = 9, failed = 3,
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanSummaryBarAllClearPreview() {
    MaterialTheme {
        ScanSummaryBar(
            state = ScannerState.Complete(
                result = previewResult(errors = 0, warnings = 0, info = 0, passed = 12, failed = 0),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanSummaryBarErrorPreview() {
    MaterialTheme {
        ScanSummaryBar(state = ScannerState.Error(message = "Semantics owner unavailable"))
    }
}

private fun previewResult(
    errors: Int,
    warnings: Int,
    info: Int,
    passed: Int,
    failed: Int,
): ScanResult {
    val node = A11yNode(
        nodeId = "node-1", composableName = "Button",
        bounds = Rect(0, 0, 300, 120), contentDescription = null,
        isTouchTarget = true, touchTargetSize = DpSize(100f, 40f),
        textColor = null, backgroundColors = emptyList(),
        isFocusable = true, isMergedDescendant = false, depth = 1,
    )
    val issues = buildList {
        repeat(errors) {
            add(
                A11yIssue(
                    "err-$it", A11ySeverity.Error, "missing-content-description",
                    "Missing Content Description", node,
                    "Interactive element has no content description.",
                    "Add Modifier.semantics { contentDescription = … }", "WCAG 1.1.1 Non-text Content (Level A)",
                )
            )
        }
        repeat(warnings) {
            add(
                A11yIssue(
                    "warn-$it", A11ySeverity.Warning, "focus-order",
                    "Focus Order", node, "Focus jumps upward unexpectedly.",
                    "Reorder composables top-to-bottom.", "WCAG 2.4.3 Focus Order (Level A)",
                )
            )
        }
        repeat(info) {
            add(
                A11yIssue(
                    "info-$it", A11ySeverity.Info, "text-scaling",
                    "Text Scaling", node, "Text does not scale with system font size.",
                    "Use sp units for all text sizes.", null,
                )
            )
        }
    }
    return ScanResult(
        scanId = "preview", timestamp = 0L,
        totalNodes = 24, issues = issues,
        passedRules = passed, failedRules = failed,
    )
}
