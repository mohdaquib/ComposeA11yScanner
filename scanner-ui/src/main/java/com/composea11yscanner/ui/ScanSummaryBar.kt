package com.composea11yscanner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult
import com.composea11yscanner.core.model.ScannerState

private val ErrorColor = Color(0xFFD32F2F)
private val WarningColor = Color(0xFFFFA000)
private val InfoColor = Color(0xFF1976D2)
private val ScoreColor = Color(0xFF6C63FF)
private val PrimaryGlow = Color(0x556C63FF)

private val PillShape = RoundedCornerShape(percent = 50)
private val ScanningPillWidth = 220.dp
private val CompletePillWidth = 280.dp
private val DefaultTopOffset = 72.dp

@Composable
fun ScanSummaryBar(
    state: ScannerState,
    modifier: Modifier = Modifier,
    topOffset: Dp = DefaultTopOffset,
) {
    var showReport by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<ScanResult?>(null) }

    LaunchedEffect(state) {
        if (state is ScannerState.Complete) lastResult = state.result
    }

    val targetWidth = when (state) {
        is ScannerState.Complete -> CompletePillWidth
        is ScannerState.Scanning -> ScanningPillWidth
        is ScannerState.Error -> CompletePillWidth
        ScannerState.Idle -> 120.dp
    }
    val pillWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 360),
        label = "scan-summary-width",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            // topOffset clears the host toolbar; the inset additionally clears the system bar.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = topOffset, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = PillShape,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(pillWidth)
                .shadow(
                    elevation = 16.dp,
                    shape = PillShape,
                    ambientColor = PrimaryGlow,
                    spotColor = PrimaryGlow,
                ),
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { current ->
                    when (current) {
                        is ScannerState.Scanning -> "scanning"
                        is ScannerState.Complete -> "complete"
                        is ScannerState.Error -> "error"
                        ScannerState.Idle -> "idle"
                    }
                },
                label = "scan-state",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

@Composable
private fun ScanningContent(progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadarSweepIcon(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Scanning...",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(PillShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
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
        CountChip(
            count = result.errorCount,
            color = ErrorColor,
            icon = Icons.Filled.Error,
            contentDescription = "${result.errorCount} errors",
        )
        CountChip(
            count = result.warningCount,
            color = WarningColor,
            icon = Icons.Filled.Warning,
            contentDescription = "${result.warningCount} warnings",
        )
        CountChip(
            count = result.infoCount,
            color = InfoColor,
            icon = Icons.Filled.Info,
            contentDescription = "${result.infoCount} info items",
        )
        ScoreChip(score = result.overallScore, onClick = onScoreClick)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            maxLines = 1,
        )
    }
}

@Composable
private fun RadarSweepIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val isInspecting = LocalInspectionMode.current
    val infiniteTransition = rememberInfiniteTransition(label = "radar-sweep")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar-sweep-rotation",
    )
    val rotation = if (isInspecting) 35f else sweepRotation

    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = color.copy(alpha = 0.14f),
            radius = radius * 0.55f,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        rotate(degrees = rotation, pivot = center) {
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = color, radius = 2.2.dp.toPx(), center = center)
    }
}

@Composable
private fun CountChip(
    count: Int,
    color: Color,
    icon: ImageVector,
    contentDescription: String,
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(color)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
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
            .clip(PillShape)
            .background(ScoreColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${score.toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = "View full report",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

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
        nodeId = "node-1",
        composableName = "Button",
        bounds = Rect(0, 0, 300, 120),
        contentDescription = null,
        isTouchTarget = true,
        touchTargetSize = DpSize(100f, 40f),
        textColor = null,
        backgroundColors = emptyList(),
        isFocusable = true,
        isMergedDescendant = false,
        depth = 1,
    )
    val issues = buildList {
        repeat(errors) {
            add(
                A11yIssue(
                    issueId = "err-$it",
                    severity = A11ySeverity.Error,
                    ruleId = "missing-content-description",
                    ruleName = "Missing Content Description",
                    affectedNode = node,
                    message = "Interactive element has no content description.",
                    howToFix = "Add Modifier.semantics { contentDescription = \"Label\" }",
                    wcagReference = "WCAG 1.1.1 Non-text Content (Level A)",
                )
            )
        }
        repeat(warnings) {
            add(
                A11yIssue(
                    issueId = "warn-$it",
                    severity = A11ySeverity.Warning,
                    ruleId = "focus-order",
                    ruleName = "Focus Order",
                    affectedNode = node,
                    message = "Focus jumps upward unexpectedly.",
                    howToFix = "Reorder composables top-to-bottom.",
                    wcagReference = "WCAG 2.4.3 Focus Order (Level A)",
                )
            )
        }
        repeat(info) {
            add(
                A11yIssue(
                    issueId = "info-$it",
                    severity = A11ySeverity.Info,
                    ruleId = "text-scaling",
                    ruleName = "Text Scaling",
                    affectedNode = node,
                    message = "Text does not scale with system font size.",
                    howToFix = "Use sp units for all text sizes.",
                    wcagReference = null,
                )
            )
        }
    }
    return ScanResult(
        scanId = "preview",
        timestamp = 0L,
        totalNodes = 24,
        issues = issues,
        passedRules = passed,
        failedRules = failed,
    )
}
