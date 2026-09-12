package com.composea11yscanner.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Switches between issue inspection and host-app interaction without discarding the scan result.
 *
 * In inspection mode, issue highlights consume taps to open their details. In interaction mode,
 * those hit targets are removed so taps reach the host application normally.
 */
@Composable
internal fun InspectionModeToggle(
    inspectionEnabled: Boolean,
    onInspectionEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = if (inspectionEnabled) {
        "Interact with app"
    } else {
        "Resume issue inspection"
    }

    FloatingActionButton(
        onClick = { onInspectionEnabledChange(!inspectionEnabled) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (inspectionEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = actionLabel,
        )
    }
}

@Preview
@Composable
private fun InspectionModeTogglePreview() {
    InspectionModeToggle(
        inspectionEnabled = true,
        onInspectionEnabledChange = {},
    )
}
