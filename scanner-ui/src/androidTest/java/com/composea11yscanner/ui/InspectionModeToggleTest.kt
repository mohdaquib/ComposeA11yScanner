package com.composea11yscanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class InspectionModeToggleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toggle_switchesBetweenInteractionAndInspectionActions() {
        var inspectionEnabled by mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                InspectionModeToggle(
                    inspectionEnabled = inspectionEnabled,
                    onInspectionEnabledChange = { inspectionEnabled = it },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Interact with app")
            .performClick()

        composeRule
            .onNodeWithContentDescription("Resume issue inspection")
            .assertExists()
            .performClick()

        composeRule
            .onNodeWithContentDescription("Interact with app")
            .assertExists()
    }
}
