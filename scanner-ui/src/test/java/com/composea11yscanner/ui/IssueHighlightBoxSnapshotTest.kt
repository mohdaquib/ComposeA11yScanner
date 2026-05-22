package com.composea11yscanner.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.composea11yscanner.core.model.A11ySeverity
import org.junit.Rule
import org.junit.Test

class IssueHighlightBoxSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun errorSeverityBorderIsRed() {
        paparazzi.snapshot {
            IssueHighlightBox(
                issue = issueFixture(A11ySeverity.Error),
                onIssueSelected = {},
            )
        }
    }

    @Test
    fun warningSeverityBorderIsOrange() {
        paparazzi.snapshot {
            IssueHighlightBox(
                issue = issueFixture(A11ySeverity.Warning),
                onIssueSelected = {},
            )
        }
    }

    @Test
    fun infoSeverityBorderIsBlue() {
        paparazzi.snapshot {
            IssueHighlightBox(
                issue = issueFixture(A11ySeverity.Info),
                onIssueSelected = {},
            )
        }
    }
}
