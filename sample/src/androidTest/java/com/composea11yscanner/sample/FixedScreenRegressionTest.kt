package com.composea11yscanner.sample

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.composea11yscanner.rules.DuplicateContentDescriptionRule
import com.composea11yscanner.rules.TextContrastRule
import com.composea11yscanner.sample.ui.theme.ScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class FixedScreenRegressionTest {
    @Test
    fun switchingBrokenFeedToFixedDoesNotCaptureInspectionButton() {
        ActivityScenario.launch(SampleActivity::class.java).use { scenario ->
            lateinit var activity: SampleActivity
            scenario.onActivity { activity = it }
            runBlocking {
                suspend fun awaitInspection() = withTimeout(10_000) {
                    while (!withContext(Dispatchers.Main) {
                        tree(activity).any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
                            ?.contains("Interact with app") == true }
                    }) delay(50)
                }
                suspend fun click(text: String) = withContext(Dispatchers.Main) {
                    val target = tree(activity).first { node ->
                        node.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick) != null &&
                            flatten(node).any { child -> child.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
                                ?.any { it.text == text } == true }
                    }
                    target.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)!!.action!!.invoke()
                }
                awaitInspection()
                click("Feed")
                delay(500)
                awaitInspection()
                click("Fixed")
                delay(500)
                awaitInspection()
                val labels = withContext(Dispatchers.Main) {
                    tree(activity).flatMap { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text).orEmpty() }
                        .map { it.text }
                }
                assertTrue("Fixed Feed scan should pass; labels=$labels", "100%" in labels)
            }
        }
    }

    private fun tree(activity: SampleActivity): List<androidx.compose.ui.semantics.SemanticsNode> {
        fun find(view: android.view.View): androidx.compose.ui.platform.AbstractComposeView? {
            if (view is androidx.compose.ui.platform.AbstractComposeView) return view
            if (view is android.view.ViewGroup) for (index in 0 until view.childCount) {
                find(view.getChildAt(index))?.let { return it }
            }
            return null
        }
        val host = find(activity.window.decorView)!!.getChildAt(0)
        val owner = host.javaClass.getMethod("getSemanticsOwner").invoke(host) as androidx.compose.ui.semantics.SemanticsOwner
        return flatten(owner.unmergedRootSemanticsNode)
    }

    private fun flatten(node: androidx.compose.ui.semantics.SemanticsNode): List<androidx.compose.ui.semantics.SemanticsNode> =
        listOf(node) + node.children.flatMap(::flatten)
    @Test fun fixedLoginSignInHasOneLabel() = checkScreen(login = true)
    @Test fun fixedFeedNegativeBadgeHasReadableContrast() = checkScreen(login = false)

    private fun checkScreen(login: Boolean) {
        ActivityScenario.launch(SampleActivity::class.java).use { scenario ->
            lateinit var activity: SampleActivity
            scenario.onActivity {
                activity = it
                it.setContent {
                    ScannerTheme {
                        Box(Modifier.height(650.dp).verticalScroll(rememberScrollState())
                            .semantics { testTag = SampleViewportTag }) {
                            Box(Modifier.semantics { testTag = BrokenSampleContentTag }) {
                                if (login) FixedLoginScreen() else FixedFeedScreen()
                            }
                        }
                    }
                }
            }
            runBlocking {
                val nodes = withTimeout(10_000) {
                    var snapshot = SampleScanNodes()
                    while (snapshot.visibleNodes.none {
                        if (login) it.contentDescription?.equals("Sign in", true) == true
                        else it.contentDescription?.contains("Open NovaPay") == true
                    }) {
                        delay(150)
                        snapshot = withContext(Dispatchers.Main) { activity.extractBrokenSampleNodes() }
                    }
                    snapshot.visibleNodes
                }
                if (login) {
                    val button = nodes.first { it.contentDescription?.equals("Sign in", true) == true }
                    assertFalse(button.hasExplicitContentDescription)
                    assertTrue(DuplicateContentDescriptionRule().evaluateAll(nodes).none {
                        it.affectedNode.nodeId == button.nodeId
                    })
                } else {
                    val badgeId = withContext(Dispatchers.Main) {
                        tree(activity).first { node ->
                            node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
                                ?.any { it.text == "-2.1%" } == true
                        }.id.toString()
                    }
                    val badge = nodes.single { it.nodeId == badgeId }
                    assertNotNull("Badge must be measured", badge.textColor)
                    assertTrue("Badge foreground=${badge.textColor?.toArgb()?.toUInt()?.toString(16)} " +
                        "background=${badge.backgroundColors} issues=${TextContrastRule().evaluateAll(listOf(badge))}",
                        TextContrastRule().evaluateAll(listOf(badge)).isEmpty())
                }
            }
        }
    }
}
