package com.composea11yscanner.ui

import androidx.compose.material3.MaterialTheme
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.composea11yscanner.core.model.ScannerState
import org.junit.Rule
import org.junit.Test

class ScanSummaryBarSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun scanningAt45Percent() {
        paparazzi.snapshot {
            MaterialTheme {
                ScanSummaryBar(state = ScannerState.Scanning(progress = 0.45f))
            }
        }
    }

    @Test
    fun completeWithIssues() {
        paparazzi.snapshot {
            MaterialTheme {
                ScanSummaryBar(
                    state = ScannerState.Complete(
                        result = scanResultFixture(errors = 2, warnings = 5, info = 1),
                    ),
                )
            }
        }
    }

    @Test
    fun completeAllClear() {
        paparazzi.snapshot {
            MaterialTheme {
                ScanSummaryBar(
                    state = ScannerState.Complete(
                        result = scanResultFixture(errors = 0, warnings = 0, info = 0),
                    ),
                )
            }
        }
    }

    @Test
    fun errorState() {
        paparazzi.snapshot {
            MaterialTheme {
                ScanSummaryBar(state = ScannerState.Error(message = "Semantics owner unavailable"))
            }
        }
    }
}
