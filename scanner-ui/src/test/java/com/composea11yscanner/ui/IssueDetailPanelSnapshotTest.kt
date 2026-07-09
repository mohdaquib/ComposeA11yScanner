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

    @Test
    fun multipleIssues_showScrollableList() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueDetailPanel(
                    issues = listOf(
                        issueFixture(
                            severity = A11ySeverity.Error,
                            issueId = "clickable-role",
                            ruleName = "Clickable Role",
                            message = "Clickable element does not expose a button role.",
                            howToFix = "Apply Modifier.clickable(role = Role.Button) for action controls.",
                            wcagReference = "WCAG 4.1.2 Name, Role, Value (Level A)",
                        ),
                        issueFixture(
                            severity = A11ySeverity.Error,
                            issueId = "touch-target-size",
                            ruleName = "Touch Target Size",
                            message = "Touch target is 28x28dp. Minimum required is 48x48dp.",
                            howToFix = "Apply Modifier.minimumInteractiveComponentSize() or add padding.",
                            wcagReference = "WCAG 2.5.5 Target Size (Level AA)",
                        ),
                        issueFixture(
                            severity = A11ySeverity.Warning,
                            issueId = "missing-content-description",
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
    }
}
