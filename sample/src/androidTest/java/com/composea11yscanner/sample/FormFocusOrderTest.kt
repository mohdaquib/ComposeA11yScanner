package com.composea11yscanner.sample

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.composea11yscanner.core.A11yScanEngine
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import com.composea11yscanner.rules.FocusOrderRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the production extraction path with Send Payment below a scrolling viewport. */
class FormFocusOrderTest {
    @Test
    fun brokenFormReportsAmountWithSubmitOffscreen() = checkForm(fixed = false)

    @Test
    fun fixedFormHasNoFocusJumpWithSubmitOffscreen() = checkForm(fixed = true)

    private fun checkForm(fixed: Boolean) {
        ActivityScenario.launch(SampleActivity::class.java).use { scenario ->
            lateinit var activity: SampleActivity
            scenario.onActivity {
                activity = it
                it.setContent {
                    MaterialTheme {
                        Box(Modifier.height(340.dp).verticalScroll(rememberScrollState())
                            .semantics { testTag = SampleViewportTag }) {
                            Box(Modifier.semantics { testTag = BrokenSampleContentTag }) {
                                if (fixed) FixedFormScreen() else BrokenFormScreen()
                            }
                        }
                    }
                }
            }
            runBlocking {
                val snapshot = withTimeout(10_000) {
                    var nodes = SampleScanNodes()
                    while (nodes.visibleNodes.none { it.contentDescription?.contains("Amount") == true }) {
                        delay(100)
                        nodes = withContext(Dispatchers.Main) { activity.extractBrokenSampleNodes() }
                    }
                    nodes
                }
                val submit = snapshot.focusOrderNodes.first { it.contentDescription == "Send Payment" }
                assertFalse("Submit must be offscreen to reproduce the regression", submit.isVisibleToUser)
                assertFalse("Offscreen submit must retain its layout bounds", submit.bounds.isEmpty())
                assertTrue(snapshot.visibleNodes.none { it.nodeId == submit.nodeId })
                val engine = A11yScanEngine(
                    listOf(FocusOrderRule(activity.resources.displayMetrics.density)),
                    ScannerConfig(enabledRules = setOf("focus-order")),
                )
                val state = engine.scan(snapshot.visibleNodes,
                    mapOf("focus-order" to snapshot.focusOrderNodes)).last() as ScannerState.Complete
                val issues = state.result.issues
                if (fixed) {
                    assertTrue("Fixed form should pass: $issues", issues.isEmpty())
                } else {
                    assertTrue("Expected Amount focus jump: $issues", issues.any {
                        it.affectedNode.contentDescription?.contains("Amount") == true
                    })
                }
                assertTrue(issues.all { it.affectedNode.isVisibleToUser })
            }
        }
    }
}
