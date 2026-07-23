package com.composea11yscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState

/**
 * Root scaffold that wires the full accessibility scanner UI around [content].
 *
 * Layer order (back to front):
 *   1. [content]          — the host screen being inspected
 *   2. [A11yIssueOverlay] — colored highlight boxes over flagged nodes
 *   3. [ScanSummaryBar]   — pinned at the top; slides in once scanning begins
 *   4. [IssueDetailPanel] — slides up from the bottom when an overlay box is tapped
 *
 * [ScanReportSheet] is opened by tapping the score chip inside [ScanSummaryBar].
 *
 * A new scan starts automatically when the scaffold enters composition and again
 * whenever [config] changes. The in-flight scan is stopped when the scaffold
 * leaves composition.
 *
 * @param scannerController Controller that runs scans and exposes scanner state.
 * @param config Scanner configuration applied to the controller.
 * @param modifier Modifier applied to the root scaffold.
 * @param issueOffsetY Vertical offset applied to issue highlights.
 * @param content Host UI content being scanned.
 */
@Composable
fun A11yScannerScaffold(
    scannerController: A11yScannerController,
    config: ScannerConfig,
    modifier: Modifier = Modifier,
    issueOffsetY: Int = 0,
    content: @Composable () -> Unit,
) {
    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Idle) }
    var selectedIssues by remember { mutableStateOf(emptyList<A11yIssue>()) }

    // Cancel any in-flight scan when the scaffold leaves composition.
    DisposableEffect(Unit) {
        onDispose { scannerController.stopScan() }
    }

    LaunchedEffect(Unit) {
        scannerController.stateFlow.collect { state ->
            scannerState = state
            if (state is ScannerState.Scanning) selectedIssues = emptyList()
        }
    }

    // Apply config and (re)start the scan whenever config changes.
    LaunchedEffect(config) {
        scannerController.configure(config)
        if (config.autoScan) {
            scannerController.startScan()
        } else {
            scannerController.clearState()
        }
    }

    val scanResult = (scannerState as? ScannerState.Complete)?.result

    Box(modifier = modifier.fillMaxSize()) {
        // ── 1. Host content ──────────────────────────────────────────────────
        content()

        // ── 2. Issue highlight overlay ───────────────────────────────────────
        A11yIssueOverlay(
            scanResult = scanResult,
            onIssuesSelected = { selectedIssues = it },
            modifier = Modifier.fillMaxSize(),
            issueOffsetY = issueOffsetY,
        )

        // ── 3. Summary bar — slides down from the top once scanning starts ───
        AnimatedVisibility(
            visible = scannerState !is ScannerState.Idle,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            ScanSummaryBar(
                state = scannerState,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── 4. Issue detail panel — slides up when an overlay box is tapped ──
        IssueDetailPanel(
            issues = selectedIssues,
            onDismiss = { selectedIssues = emptyList() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Scaffold – Idle (bar hidden)")
@Composable
private fun A11yScannerScaffoldIdlePreview() {
    MaterialTheme {
        A11yScannerScaffold(
            scannerController = remember {
                A11yScannerController(
                    nodeProvider = { emptyList() },
                    screenDensity = 2f,
                )
            },
            config = ScannerConfig(enabledRules = emptySet()),
        ) {
            SampleHostContent()
        }
    }
}

@Preview(showBackground = true, name = "Scaffold – Complete (bar + overlay)")
@Composable
private fun A11yScannerScaffoldCompletePreview() {
    MaterialTheme {
        // Render the inner layout directly so the preview shows a populated state
        // without needing a live coroutine.
        Box(modifier = Modifier.fillMaxSize()) {
            SampleHostContent()
            ScanSummaryBar(
                state = ScannerState.Complete(result = previewScanResult()),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SampleHostContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Screen Title", style = MaterialTheme.typography.titleLarge)
        Text("This is the host content being scanned.", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun previewScanResult() = ScanResult(
    scanId = "preview",
    timestamp = 0L,
    totalNodes = 12,
    issues = listOf(
        A11yIssue(
            issueId = "err-1",
            severity = A11ySeverity.Error,
            ruleId = "missing-content-description",
            ruleName = "Missing Content Description",
            affectedNode = A11yNode(
                nodeId = "node-1", composableName = "Button",
                bounds = Rect(0, 0, 300, 120),
                contentDescription = null, isTouchTarget = true,
                textColor = null, backgroundColors = emptyList(),
                isFocusable = true, isMergedDescendant = false, depth = 1,
            ),
            message = "Interactive element has no content description.",
            howToFix = "Add Modifier.semantics { contentDescription = … }",
            wcagReference = "WCAG 1.1.1 Non-text Content (Level A)",
        ),
    ),
    passedRules = 9,
    failedRules = 1,
)
