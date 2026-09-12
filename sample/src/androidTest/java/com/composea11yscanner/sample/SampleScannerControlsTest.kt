package com.composea11yscanner.sample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SampleScannerControlsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<SampleActivity>()

    @Test
    fun inspectionControlAppearsOnLaunchAndAfterChangingSamples() {
        awaitInspectionControl()
        composeRule.onNodeWithContentDescription("Clear scan results").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Scan selected sample").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Interact with app").performClick()
        composeRule.onNodeWithContentDescription("Resume issue inspection")
            .assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Interact with app").performClick()
        composeRule.onNodeWithText("Feed").performClick()
        awaitInspectionControl()
        composeRule.onNodeWithContentDescription("Interact with app").performClick()
        composeRule.onNodeWithText("Fixed").performClick()
        awaitInspectionControl()
    }

    private fun awaitInspectionControl() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Interact with app")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Interact with app").assertIsDisplayed()
    }
}
