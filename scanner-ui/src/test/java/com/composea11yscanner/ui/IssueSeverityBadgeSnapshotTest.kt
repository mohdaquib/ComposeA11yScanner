package com.composea11yscanner.ui

import androidx.compose.material3.MaterialTheme
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.composea11yscanner.core.model.A11ySeverity
import org.junit.Rule
import org.junit.Test

class IssueSeverityBadgeSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun errorBadge_count1() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueSeverityBadge(severity = A11ySeverity.Error, count = 1)
            }
        }
    }

    @Test
    fun warningBadge_count1() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueSeverityBadge(severity = A11ySeverity.Warning, count = 1)
            }
        }
    }

    @Test
    fun infoBadge_count1() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueSeverityBadge(severity = A11ySeverity.Info, count = 1)
            }
        }
    }

    @Test
    fun errorBadge_count3() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueSeverityBadge(severity = A11ySeverity.Error, count = 3)
            }
        }
    }

    @Test
    fun warningBadge_count9plus() {
        paparazzi.snapshot {
            MaterialTheme {
                IssueSeverityBadge(severity = A11ySeverity.Warning, count = 9)
            }
        }
    }
}
