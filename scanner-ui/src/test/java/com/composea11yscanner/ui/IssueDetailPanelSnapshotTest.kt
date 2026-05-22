package com.composea11yscanner.ui

import androidx.compose.material3.MaterialTheme
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.composea11yscanner.core.model.A11ySeverity
import org.junit.Rule
import org.junit.Test

class IssueDetailPanelSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun errorIssue_withWcagReference() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueDetailPanel(
                    issue = issueFixture(
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
    }

    @Test
    fun warningIssue_withWcagReference() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueDetailPanel(
                    issue = issueFixture(
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
    }

    @Test
    fun infoIssue_noWcagReference() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueDetailPanel(
                    issue = issueFixture(
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
    }
}
