package com.composea11yscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState

/** Overlay UI installed above an activity's Compose content. */
@Composable
internal fun ScannerOverlayContent(
    controller: A11yScannerController,
    config: ScannerConfig,
) {
    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Idle) }
    var selectedIssues by remember { mutableStateOf(emptyList<A11yIssue>()) }
    var inspectionEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) { onDispose { controller.stopScan() } }

    LaunchedEffect(Unit) {
        controller.stateFlow.collect { state ->
            scannerState = state
            if (state !is ScannerState.Complete) {
                selectedIssues = emptyList()
                inspectionEnabled = true
            }
        }
    }

    LaunchedEffect(config) {
        controller.configure(config)
        if (!config.autoScan) controller.clearState()
    }

    val scanResult = (scannerState as? ScannerState.Complete)?.result
    Box(modifier = Modifier.fillMaxSize()) {
        A11yIssueOverlay(
            scanResult = scanResult.takeIf { inspectionEnabled },
            onIssuesSelected = { selectedIssues = it },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = scannerState !is ScannerState.Idle && inspectionEnabled,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth(),
        ) {
            ScanSummaryBar(state = scannerState, modifier = Modifier.fillMaxWidth())
        }

        IssueDetailPanel(
            issues = selectedIssues.takeIf { inspectionEnabled }.orEmpty(),
            onDismiss = { selectedIssues = emptyList() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(
            visible = scanResult != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            InspectionModeToggle(
                inspectionEnabled = inspectionEnabled,
                onInspectionEnabledChange = { enabled ->
                    inspectionEnabled = enabled
                    if (!enabled) selectedIssues = emptyList()
                },
            )
        }
    }
}
