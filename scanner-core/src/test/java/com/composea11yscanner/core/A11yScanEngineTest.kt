package com.composea11yscanner.core

import app.cash.turbine.test
import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import com.composea11yscanner.core.rule.A11yRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yScanEngineTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun stubNode(id: String = "node-1") = A11yNode(
        nodeId = id,
        composableName = "Box",
        bounds = Rect(0, 0, 100, 100),
        contentDescription = null,
        isTouchTarget = false,
        textColor = null,
        backgroundColors = emptyList(),
        isFocusable = false,
        isMergedDescendant = false,
        depth = 0,
    )

    private fun stubIssue(
        ruleId: String,
        nodeId: String = "node-1",
        severity: A11ySeverity = A11ySeverity.Error,
    ) = A11yIssue(
        issueId = "${ruleId}_$nodeId",
        severity = severity,
        ruleId = ruleId,
        ruleName = "Stub $ruleId",
        affectedNode = stubNode(nodeId),
        message = "stub message",
        howToFix = "stub fix",
        wcagReference = null,
    )

    private fun mockRule(
        id: String,
        issues: List<A11yIssue> = emptyList(),
    ): A11yRule = mockk {
        every { ruleId } returns id
        every { evaluateAll(any()) } returns issues
    }

    /** Builds a [ScannerConfig] with only [ruleIds] enabled; all other fields use defaults. */
    private fun configOf(vararg ruleIds: String) =
        ScannerConfig(enabledRules = ruleIds.toSet())

    // ── fast path (empty rules / nodes) ──────────────────────────────────────

    @Test
    fun `empty node list emits Complete with zero totalNodes immediately`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a")),
            config = configOf("rule-a"),
        )
        engine.scan(emptyList()).test {
            val state = awaitItem() as ScannerState.Complete
            assertEquals(0, state.result.totalNodes)
            assertEquals(0, state.result.issues.size)
            awaitComplete()
        }
    }

    @Test
    fun `no enabled rules emits Complete immediately without any Scanning emission`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a")),
            config = configOf(),   // empty — rule-a is not enabled
        )
        engine.scan(listOf(stubNode())).test {
            assertTrue(awaitItem() is ScannerState.Complete)
            awaitComplete()
        }
    }

    @Test
    fun `rules not present in enabledRules are filtered and never evaluated`() = runTest {
        val rule = mockRule("rule-a")
        val engine = A11yScanEngine(
            rules = listOf(rule),
            config = configOf("rule-b"),   // rule-a is absent
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem()   // Complete (fast path because enabledRules is empty after filter)
            awaitComplete()
        }
        verify(exactly = 0) { rule.evaluateAll(any()) }
    }

    // ── progress emissions ────────────────────────────────────────────────────

    @Test
    fun `single rule emits Scanning 0f then Scanning 1f before Complete`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a")),
            config = configOf("rule-a"),
        )
        engine.scan(listOf(stubNode())).test {
            assertEquals(ScannerState.Scanning(0f), awaitItem())
            assertEquals(ScannerState.Scanning(1f), awaitItem())
            assertTrue(awaitItem() is ScannerState.Complete)
            awaitComplete()
        }
    }

    @Test
    fun `three rules emit progress fractions matching batch count`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf("rule-a", "rule-b", "rule-c").map { mockRule(it) },
            config = configOf("rule-a", "rule-b", "rule-c"),
        )
        engine.scan(listOf(stubNode())).test {
            assertEquals(ScannerState.Scanning(0f),      awaitItem())
            assertEquals(ScannerState.Scanning(1f / 3f), awaitItem())
            assertEquals(ScannerState.Scanning(2f / 3f), awaitItem())
            assertEquals(ScannerState.Scanning(3f / 3f), awaitItem())
            assertTrue(awaitItem() is ScannerState.Complete)
            awaitComplete()
        }
    }

    @Test
    fun `last progress emission is always Scanning 1f regardless of rule count`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf("rule-a", "rule-b").map { mockRule(it) },
            config = configOf("rule-a", "rule-b"),
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem()   // Scanning(0f)
            awaitItem()   // Scanning(0.5f)
            assertEquals(ScannerState.Scanning(1f), awaitItem())
            awaitItem()   // Complete
            awaitComplete()
        }
    }

    // ── issue aggregation ─────────────────────────────────────────────────────

    @Test
    fun `single rule failure emits correct issue inside Complete`() = runTest {
        val issue = stubIssue("rule-a")
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a", listOf(issue))),
            config = configOf("rule-a"),
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem(); awaitItem()   // Scanning(0f), Scanning(1f)
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(1, result.issues.size)
            assertEquals(issue, result.issues[0])
            awaitComplete()
        }
    }

    @Test
    fun `passing rule contributes to passedRules with zero failedRules`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a")),   // no issues
            config = configOf("rule-a"),
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem(); awaitItem()
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(1, result.passedRules)
            assertEquals(0, result.failedRules)
            awaitComplete()
        }
    }

    @Test
    fun `failing rule contributes to failedRules with zero passedRules`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a", listOf(stubIssue("rule-a")))),
            config = configOf("rule-a"),
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem(); awaitItem()
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(0, result.passedRules)
            assertEquals(1, result.failedRules)
            awaitComplete()
        }
    }

    @Test
    fun `multiple rules aggregate issues from all failing rules`() = runTest {
        val issueA = stubIssue("rule-a")
        val issueB = stubIssue("rule-b", nodeId = "node-2")
        val engine = A11yScanEngine(
            rules = listOf(
                mockRule("rule-a", listOf(issueA)),
                mockRule("rule-b", listOf(issueB)),
                mockRule("rule-c"),              // passing — no issues
            ),
            config = configOf("rule-a", "rule-b", "rule-c"),
        )
        engine.scan(listOf(stubNode())).test {
            repeat(4) { awaitItem() }   // Scanning(0f), (1/3), (2/3), (1f)
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(2, result.issues.size)
            assertTrue(result.issues.contains(issueA))
            assertTrue(result.issues.contains(issueB))
            assertEquals(1, result.passedRules)   // rule-c
            assertEquals(2, result.failedRules)   // rule-a, rule-b
            awaitComplete()
        }
    }

    @Test
    fun `issues are sorted by severity order before ruleId`() = runTest {
        val warningIssue = stubIssue("rule-z", severity = A11ySeverity.Warning)
        val errorIssue   = stubIssue("rule-a", severity = A11ySeverity.Error)
        val engine = A11yScanEngine(
            rules = listOf(
                mockRule("rule-z", listOf(warningIssue)),   // Warning emitted first by order of rules
                mockRule("rule-a", listOf(errorIssue)),
            ),
            config = configOf("rule-a", "rule-z"),
        )
        engine.scan(listOf(stubNode())).test {
            repeat(3) { awaitItem() }   // Scanning x3
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(errorIssue,   result.issues[0])   // Error (sortOrder 0) before Warning (sortOrder 1)
            assertEquals(warningIssue, result.issues[1])
            awaitComplete()
        }
    }

    @Test
    fun `issues with equal severity are sorted by ruleId alphabetically`() = runTest {
        val issueB = stubIssue("rule-b")
        val issueA = stubIssue("rule-a")
        val engine = A11yScanEngine(
            rules = listOf(
                mockRule("rule-b", listOf(issueB)),   // rule-b encountered first
                mockRule("rule-a", listOf(issueA)),
            ),
            config = configOf("rule-a", "rule-b"),
        )
        engine.scan(listOf(stubNode())).test {
            repeat(3) { awaitItem() }   // Scanning x3
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(issueA, result.issues[0])   // "rule-a" < "rule-b"
            assertEquals(issueB, result.issues[1])
            awaitComplete()
        }
    }

    // ── ScanResult metadata ───────────────────────────────────────────────────

    @Test
    fun `totalNodes in ScanResult reflects the actual input list size`() = runTest {
        val nodes = List(7) { stubNode("node-$it") }
        val engine = A11yScanEngine(
            rules = listOf(mockRule("rule-a")),
            config = configOf("rule-a"),
        )
        engine.scan(nodes).test {
            repeat(2) { awaitItem() }   // Scanning(0f), Scanning(1f)
            val result = (awaitItem() as ScannerState.Complete).result
            assertEquals(7, result.totalNodes)
            awaitComplete()
        }
    }

    @Test
    fun `evaluateAll is called exactly once per rule with the provided nodes`() = runTest {
        val nodes = listOf(stubNode("n1"), stubNode("n2"))
        val rule  = mockRule("rule-a")
        val engine = A11yScanEngine(rules = listOf(rule), config = configOf("rule-a"))

        engine.scan(nodes).test {
            repeat(3) { awaitItem() }   // Scanning(0f), Scanning(1f), Complete
            awaitComplete()
        }

        verify(exactly = 1) { rule.evaluateAll(nodes) }
    }

    @Test
    fun `scanId is non-blank in every Complete result`() = runTest {
        val engine = A11yScanEngine(rules = emptyList(), config = configOf())
        engine.scan(emptyList()).test {
            val result = (awaitItem() as ScannerState.Complete).result
            assertTrue(result.scanId.isNotBlank())
            awaitComplete()
        }
    }

    // ── error handling ────────────────────────────────────────────────────────

    @Test
    fun `rule that throws RuntimeException emits Error state with the exception message`() = runTest {
        val throwingRule = mockk<A11yRule> {
            every { ruleId } returns "throws-rule"
            every { evaluateAll(any()) } throws RuntimeException("boom")
        }
        val engine = A11yScanEngine(
            rules = listOf(throwingRule),
            config = configOf("throws-rule"),
        )
        engine.scan(listOf(stubNode())).test {
            assertEquals(ScannerState.Scanning(0f), awaitItem())
            val error = awaitItem() as ScannerState.Error
            assertEquals("boom", error.message)
            awaitComplete()
        }
    }

    @Test
    fun `exception with null message emits fallback error text`() = runTest {
        val throwingRule = mockk<A11yRule> {
            every { ruleId } returns "throws-rule"
            every { evaluateAll(any()) } throws RuntimeException(null as String?)
        }
        val engine = A11yScanEngine(
            rules = listOf(throwingRule),
            config = configOf("throws-rule"),
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem()   // Scanning(0f)
            val error = awaitItem() as ScannerState.Error
            assertEquals("Scan failed", error.message)
            awaitComplete()
        }
    }

    // ── cancellation ──────────────────────────────────────────────────────────

    @Test
    fun `scan cancellation via flow cancellation terminates without emitting Error`() = runTest {
        val engine = A11yScanEngine(
            rules = listOf("rule-a", "rule-b", "rule-c", "rule-d", "rule-e").map { mockRule(it) },
            config = configOf("rule-a", "rule-b", "rule-c", "rule-d", "rule-e"),
        )
        engine.scan(listOf(stubNode())).test {
            awaitItem()   // Scanning(0f)
            cancelAndIgnoreRemainingEvents()
        }
        // Reaching here without exception confirms clean cancellation
    }

    @Test
    fun `CancellationException from rule propagates and is not mapped to Error state`() = runTest {
        val throwingRule = mockk<A11yRule> {
            every { ruleId } returns "throws-rule"
            every { evaluateAll(any()) } throws CancellationException("from rule")
        }
        val engine = A11yScanEngine(
            rules = listOf(throwingRule),
            config = configOf("throws-rule"),
        )

        val collected = mutableListOf<ScannerState>()
        // runCatching captures the propagated CancellationException without cancelling
        // the test coroutine's own job (the CE originates from the flow, not job cancellation).
        val outcome = runCatching {
            engine.scan(listOf(stubNode())).collect { collected.add(it) }
        }

        assertFalse(
            "Error state must not be emitted for CancellationException",
            collected.any { it is ScannerState.Error },
        )
        assertTrue(
            "CancellationException should propagate to the collector",
            outcome.exceptionOrNull() is CancellationException,
        )
    }
}
