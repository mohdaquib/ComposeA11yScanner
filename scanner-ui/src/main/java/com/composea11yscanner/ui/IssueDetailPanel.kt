package com.composea11yscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.DpSize
import com.composea11yscanner.core.model.Rect
import java.util.Locale

private val ErrorChipColor = Color(0xFFD32F2F)
private val WarningChipColor = Color(0xFFFFA000)
private val InfoChipColor = Color(0xFF1976D2)
private val IssueListMaxHeight = 480.dp

private fun A11ySeverity.toChipColor(): Color = when (this) {
    A11ySeverity.Error -> ErrorChipColor
    A11ySeverity.Warning -> WarningChipColor
    A11ySeverity.Info -> InfoChipColor
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

// "WCAG 1.1.1 Non-text Content (Level A)" → "https://www.w3.org/WAI/WCAG22/Understanding/non-text-content"
// "WCAG 4.1.2 Name, Role, Value (Level A)" → "https://www.w3.org/WAI/WCAG22/Understanding/name-role-value"
private fun String.toWcagUrl(): String {
    val title = Regex("""WCAG \d+\.\d+\.\d+ (.+?) \(Level""").find(this)
        ?.groupValues?.getOrNull(1)
        ?: return "https://www.w3.org/TR/WCAG22/"
    val slug = title.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "https://www.w3.org/WAI/WCAG22/Understanding/$slug"
}

/**
 * Slides up from the bottom when [issue] becomes non-null; slides back down on dismissal.
 * Content is retained during the exit transition so the panel doesn't flash empty.
 *
 * @param issue Primary issue to display, or null to hide the panel.
 * @param issues Issues associated with the selected node.
 * @param onDismiss Callback invoked when the panel close action is tapped.
 * @param modifier Modifier applied to the panel container.
 */
/**
 * Convenience overload for showing one or more issues.
 *
 * @param issues Issues associated with the selected node.
 * @param onDismiss Callback invoked when the panel close action is tapped.
 * @param modifier Modifier applied to the panel container.
 */
@Composable
fun IssueDetailPanel(
    issue: A11yIssue?,
    issues: List<A11yIssue> = issue?.let(::listOf).orEmpty(),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeIssues = issues.ifEmpty { issue?.let(::listOf).orEmpty() }
    val isVisible = activeIssues.isNotEmpty()
    // Keep the last non-null issue so the content stays visible during the exit slide.
    var panelIssues by remember { mutableStateOf(emptyList<A11yIssue>()) }
    LaunchedEffect(activeIssues) {
        if (activeIssues.isNotEmpty()) panelIssues = activeIssues
    }
    val displayIssues = activeIssues.ifEmpty { panelIssues }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        if (displayIssues.isNotEmpty()) {
            PanelContent(issues = displayIssues, onDismiss = onDismiss)
        }
    }
}

@Composable
fun IssueDetailPanel(
    issues: List<A11yIssue>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IssueDetailPanel(
        issue = issues.firstOrNull(),
        issues = issues,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun PanelContent(
    issues: List<A11yIssue>,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val primaryIssue = issues.first()
    val panelTitle = if (issues.size == 1) {
        primaryIssue.ruleName
    } else {
        "${issues.size} issues on ${primaryIssue.affectedNode.composableName}"
    }

    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            DragHandle()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = panelTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss panel",
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = IssueListMaxHeight),
            ) {
                itemsIndexed(
                    items = issues,
                    key = { index, issue -> "${issue.issueId}-$index" },
                ) { index, issue ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    IssueBlock(
                        issue = issue,
                        onWcagClick = { ref -> uriHandler.openUri(ref.toWcagUrl()) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueBlock(
    issue: A11yIssue,
    onWcagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = issue.ruleName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            SeverityChip(severity = issue.severity)
        }

        SectionBlock(
            label = "Issue",
            body = issue.message,
        )

        SectionBlock(
            label = "How to fix",
            body = issue.howToFix,
        )

        issue.wcagReference?.let { ref ->
            WcagLink(
                reference = ref,
                onClick = { onWcagClick(ref) },
            )
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun SeverityChip(severity: A11ySeverity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(severity.toChipColor())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = severity.toIcon(),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = severity.label(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SectionBlock(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WcagLink(
    reference: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = reference,
        style = MaterialTheme.typography.bodySmall.copy(
            textDecoration = TextDecoration.Underline,
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.clickable(onClick = onClick),
    )
}

// — Previews ——————————————————————————————————————————————————————————————————

@Preview(showBackground = true)
@Composable
private fun IssueDetailPanelErrorPreview() {
    MaterialTheme {
        IssueDetailPanel(
            issue = previewIssue(
                severity = A11ySeverity.Error,
                ruleName = "Missing Content Description",
                message = "Interactive element has no content description. " +
                    "Screen readers cannot announce this.",
                howToFix = "Add a meaningful contentDescription via semantics: " +
                    "Modifier.semantics { contentDescription = \"Describe action here\" }",
                wcagReference = "WCAG 1.1.1 Non-text Content (Level A)",
            ),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IssueDetailPanelWarningPreview() {
    MaterialTheme {
        IssueDetailPanel(
            issue = previewIssue(
                severity = A11ySeverity.Warning,
                ruleName = "Focus Order",
                message = "Focus jumps upward from 200dp to 80dp.",
                howToFix = "Reorder composables so focus flows top-to-bottom.",
                wcagReference = "WCAG 2.4.3 Focus Order (Level A)",
            ),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IssueDetailPanelNoWcagPreview() {
    MaterialTheme {
        IssueDetailPanel(
            issue = previewIssue(
                severity = A11ySeverity.Info,
                ruleName = "Text Scaling",
                message = "Text does not scale with system font size.",
                howToFix = "Use sp units for all text sizes.",
                wcagReference = null,
            ),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IssueDetailPanelMultipleIssuesPreview() {
    MaterialTheme {
        IssueDetailPanel(
            issues = listOf(
                previewIssue(
                    severity = A11ySeverity.Error,
                    ruleName = "Clickable Role",
                    message = "Clickable element does not expose a button role.",
                    howToFix = "Apply Modifier.clickable(role = Role.Button) for action controls.",
                    wcagReference = "WCAG 4.1.2 Name, Role, Value (Level A)",
                ),
                previewIssue(
                    severity = A11ySeverity.Error,
                    ruleName = "Touch Target Size",
                    message = "Touch target is 28x28dp. Minimum required is 48x48dp.",
                    howToFix = "Apply Modifier.minimumInteractiveComponentSize() or add padding.",
                    wcagReference = "WCAG 2.5.5 Target Size (Level AA)",
                ),
                previewIssue(
                    severity = A11ySeverity.Warning,
                    ruleName = "Missing Content Description",
                    message = "Interactive element has no content description.",
                    howToFix = "Add a meaningful contentDescription via semantics.",
                    wcagReference = "WCAG 1.1.1 Non-text Content (Level A)",
                ),
            ),
            onDismiss = {},
        )
    }
}

private fun previewIssue(
    severity: A11ySeverity,
    ruleName: String,
    message: String,
    howToFix: String,
    wcagReference: String?,
) = A11yIssue(
    issueId = "preview-1",
    severity = severity,
    ruleId = "preview-rule",
    ruleName = ruleName,
    affectedNode = A11yNode(
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
    ),
    message = message,
    howToFix = howToFix,
    wcagReference = wcagReference,
)
