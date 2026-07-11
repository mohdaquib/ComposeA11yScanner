package com.composea11yscanner.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.rules.ScannerRules
import com.composea11yscanner.sample.ui.theme.ScannerTheme
import com.composea11yscanner.ui.A11yScanner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FixedScreenAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fixedLoginScreen_hasNoScannerIssues() {
        assertNoScannerIssues {
            FixedLoginScreen()
        }
    }

    @Test
    fun fixedFeedScreen_hasNoScannerIssues() {
        assertNoScannerIssues {
            FixedFeedScreen()
        }
    }

    @Test
    fun fixedFormScreen_hasNoScannerIssues() {
        assertNoScannerIssues {
            FixedFormScreen()
        }
    }

    private fun assertNoScannerIssues(content: @Composable () -> Unit) {
        composeRule.setContent {
            ScannerTheme {
                content()
            }
        }

        composeRule.waitForIdle()

        val config = ScannerConfig(enabledRules = ScannerRules.allRuleIds().toSet())
        val scanner = A11yScanner(
            rules = ScannerRules.buildRules(config, screenDensity = 1f),
            density = Density(1f),
        )
        val result = scanner.scan(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())

        assertEquals(
            "Expected fixed screen to have zero scanner issues. Issues: ${result.issues}",
            0,
            result.issues.size,
        )
    }
}
