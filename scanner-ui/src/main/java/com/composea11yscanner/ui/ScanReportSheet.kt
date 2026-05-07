package com.composea11yscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Colour constants + severity helpers (file-private)
// ─────────────────────────────────────────────────────────────────────────────

private val ErrorColor = Color(0xFFD32F2F)
private val WarningColor = Color(0xFFFFA000)
private val InfoColor = Color(0xFF1976D2)
private val ScoreGoodColor = Color(0xFF2E7D32)
private val ScoreFairColor = Color(0xFFF57C00)
private val ScorePoorColor = Color(0xFFC62828)

private val SeverityOrder = listOf(A11ySeverity.Error, A11ySeverity.Warning, A11ySeverity.Info)

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
 * Full-screen report sheet. Pressing back/dismiss while a detail panel is open
 * closes the panel first; a second press dismisses the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReportSheet(
    result: ScanResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeFilter by remember { mutableStateOf<A11ySeverity?>(null) }
    var selectedIssue by remember { mutableStateOf<A11yIssue?>(null) }

    val grouped = remember(result) { result.issues.groupBy { it.severity } }
    val displayIssues = result.issues.let { all ->
        val f = activeFilter
        if (f == null) all else all.filter { it.severity == f }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (selectedIssue != null) selectedIssue = null else onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        // Box lets IssueDetailPanel overlay the list at BottomCenter when a row is tapped.
        Box(Modifier.fillMaxWidth()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    ReportScoreHeader(result = result)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                item {
                    FilterChipRow(
                        result = result,
                        activeFilter = activeFilter,
                        onFilterChange = { activeFilter = it },
                    )
                    HorizontalDivider()
                }

                if (displayIssues.isEmpty()) {
                    item {
                        Text(
                            text = if (result.issues.isEmpty()) {
                                "No issues found — all checks passed."
                            } else {
                                // activeFilter is guaranteed non-null here: if it were null,
                                // displayIssues == result.issues which would also be empty,
                                // handled by the outer branch.
                                "No ${activeFilter!!.label().lowercase(Locale.ROOT)} issues."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else if (activeFilter == null) {
                    // Grouped view: Errors → Warnings → Info, each with a section header.
                    SeverityOrder.forEach { severity ->
                        val issues = grouped[severity] ?: return@forEach
                        if (issues.isEmpty()) return@forEach
                        item(key = "header-${severity.label()}") {
                            SeverityGroupHeader(severity = severity, count = issues.size)
                        }
                        items(issues, key = { it.issueId }) { issue ->
                            IssueListRow(
                                issue = issue,
                                onClick = { selectedIssue = issue },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                } else {
                    // Flat filtered view: no section header (filter chip signals the severity).
                    items(displayIssues, key = { it.issueId }) { issue ->
                        IssueListRow(
                            issue = issue,
                            onClick = { selectedIssue = issue },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                        )
                    }
                }
            }

            // Slides in over the list when a row chevron is tapped; slides back on dismiss.
            IssueDetailPanel(
                issue = selectedIssue,
                onDismiss = { selectedIssue = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sheet sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReportScoreHeader(result: ScanResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
    }
}

@Composable
private fun FilterChipRow(
    result: ScanResult,
    activeFilter: A11ySeverity?,
    onFilterChange: (A11ySeverity?) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = activeFilter == null,
            onClick = { onFilterChange(null) },
            label = { Text("All (${result.issues.size})") },
        )
        // Only render a filter chip for severities that have at least one issue.
        if (result.errorCount > 0) {
            FilterChip(
                selected = activeFilter == A11ySeverity.Error,
                onClick = {
                    onFilterChange(
                        if (activeFilter == A11ySeverity.Error) null else A11ySeverity.Error,
                    )
                },
                label = { Text("Errors (${result.errorCount})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        if (result.warningCount > 0) {
            FilterChip(
                selected = activeFilter == A11ySeverity.Warning,
                onClick = {
                    onFilterChange(
                        if (activeFilter == A11ySeverity.Warning) null else A11ySeverity.Warning,
                    )
                },
                label = { Text("Warnings (${result.warningCount})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = WarningColor,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        if (result.infoCount > 0) {
            FilterChip(
                selected = activeFilter == A11ySeverity.Info,
                onClick = {
                    onFilterChange(
                        if (activeFilter == A11ySeverity.Info) null else A11ySeverity.Info,
                    )
                },
                label = { Text("Info (${result.infoCount})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = InfoColor,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
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
private fun IssueListRow(issue: A11yIssue, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IssueSeverityBadge(severity = issue.severity)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.affectedNode.composableName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = issue.ruleName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "View issue detail",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ScanReportSheetPreview() {
    MaterialTheme {
        ScanReportSheet(
            result = previewResult(errors = 2, warnings = 3, info = 1),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanReportSheetAllClearPreview() {
    MaterialTheme {
        ScanReportSheet(
            result = previewResult(errors = 0, warnings = 0, info = 0),
            onDismiss = {},
        )
    }
}

private fun previewResult(errors: Int, warnings: Int, info: Int): ScanResult {
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
                    "Add Modifier.semantics { contentDescription = … }",
                    "WCAG 1.1.1 Non-text Content (Level A)",
                )
            )
        }
        repeat(warnings) {
            add(
                A11yIssue(
                    "warn-$it", A11ySeverity.Warning, "focus-order",
                    "Focus Order", node, "Focus jumps upward unexpectedly.",
                    "Reorder composables top-to-bottom.",
                    "WCAG 2.4.3 Focus Order (Level A)",
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
        totalNodes = 18, issues = issues,
        passedRules = 9, failedRules = errors + warnings + info,
    )
}
