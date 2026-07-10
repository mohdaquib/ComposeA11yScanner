package com.composea11yscanner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11ySeverity

private val ErrorBadgeColor = Color(0xFFD32F2F)
private val WarningBadgeColor = Color(0xFFFFA000)
private val InfoBadgeColor = Color(0xFF1976D2)

private fun A11ySeverity.toBadgeColor(): Color = when (this) {
    A11ySeverity.Error -> ErrorBadgeColor
    A11ySeverity.Warning -> WarningBadgeColor
    A11ySeverity.Info -> InfoBadgeColor
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

/**
 * Compact severity badge used in scan summaries and overlays.
 *
 * @param severity Severity represented by the badge color and icon.
 * @param count Number of issues represented by the badge.
 * @param modifier Modifier applied to the badge row.
 */
@Composable
fun IssueSeverityBadge(
    severity: A11ySeverity,
    count: Int = 1,
    modifier: Modifier = Modifier,
) {
    // Skip the entrance animation in the layout inspector / preview tool so
    // the badge renders at full scale rather than being invisible.
    val isInspecting = LocalInspectionMode.current
    var appeared by remember { mutableStateOf(isInspecting) }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "badge-scale",
    )

    LaunchedEffect(Unit) { appeared = true }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Grow from the top-left corner so the badge pops out
                // in place rather than expanding from its own centre.
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .clip(RoundedCornerShape(4.dp))
            .background(severity.toBadgeColor())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = severity.toIcon(),
            contentDescription = severity.label(),
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        if (count > 1) {
            Text(
                text = count.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IssueSeverityBadgeErrorPreview() {
    IssueSeverityBadge(severity = A11ySeverity.Error)
}

@Preview(showBackground = true)
@Composable
private fun IssueSeverityBadgeWarningPreview() {
    IssueSeverityBadge(severity = A11ySeverity.Warning)
}

@Preview(showBackground = true)
@Composable
private fun IssueSeverityBadgeInfoPreview() {
    IssueSeverityBadge(severity = A11ySeverity.Info)
}

@Preview(showBackground = true)
@Composable
private fun IssueSeverityBadgeCountPreview() {
    IssueSeverityBadge(severity = A11ySeverity.Warning, count = 3)
}
