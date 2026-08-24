package com.composea11yscanner

import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerLifecycleTest {

    @Test
    fun `production is denied by default`() {
        val lifecycle = lifecycle()

        assertFalse(lifecycle.isAllowed(debuggable = false))
        assertTrue(lifecycle.isAllowed(debuggable = true))
    }

    @Test
    fun `toggle installs only resumed activities`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(installed = installed)
        lifecycle.resume("first", Unit)
        lifecycle.resume("second", Unit)
        lifecycle.pause("first")

        lifecycle.toggle(true)
        lifecycle.resume("third", Unit)

        assertEquals(listOf("second", "third"), installed)
        assertEquals(listOf("second", "third"), lifecycle.resumedActivities())
    }

    @Test
    fun `queued resume cannot reverse uninstall`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(
            debugActivities = setOf("debug"),
            installed = installed,
        )
        lifecycle.prepare("debug")
        lifecycle.uninstall("debug")

        lifecycle.resume("debug", Unit)

        assertTrue(installed.isEmpty())
        lifecycle.pause("debug")
        lifecycle.resume("debug", Unit)
        assertEquals(listOf("debug"), installed)
    }

    @Test
    fun `manual routing falls back after removal`() {
        val entries = linkedMapOf(
            "manual" to Entry("manual", automatic = false),
            "first" to Entry("first", automatic = true),
            "second" to Entry("second", automatic = true),
        )

        val automatic = selectEntry(
            resumedActivities = listOf("first", "second"),
            entries = entries,
            isAutomatic = Entry::automatic,
        )
        entries.remove("second")
        entries.remove("first")
        val manual = selectEntry(emptyList(), entries, Entry::automatic)

        assertEquals("second", automatic?.id)
        assertEquals("manual", manual?.id)
    }

    @Test
    fun `disable removes only production installs and is idempotent`() {
        val installed = mutableListOf<String>()
        var removals = 0
        val lifecycle = lifecycle(
            debugActivities = setOf("debug"),
            installed = installed,
            removeProd = {
                removals++
                installed.removeAll { it != "debug" }
            },
        )
        lifecycle.resume("debug", Unit)
        lifecycle.toggle(true)
        lifecycle.resume("prod", Unit)

        lifecycle.toggle(false)
        lifecycle.toggle(false)

        assertEquals(1, removals)
        assertEquals(listOf("debug", "debug"), installed)
        assertFalse(lifecycle.isAllowed(debuggable = false))
    }

    @Test
    fun `worker thread rejection happens before state change`() {
        val lifecycle = lifecycle(checkMainThread = { error("wrong thread") })

        assertThrows(IllegalStateException::class.java) {
            lifecycle.toggle(true)
        }
        assertFalse(lifecycle.isAllowed(debuggable = false))
    }

    @Test
    fun `deferred resume requires a resumed live activity`() {
        assertTrue(canResume(destroyed = false, state = RESUMED))
        assertFalse(canResume(destroyed = false, state = CREATED))
        assertFalse(canResume(destroyed = true, state = RESUMED))
        assertFalse(canResume(destroyed = true, state = DESTROYED))
    }

    private data class Entry(val id: String, val automatic: Boolean)

    private fun lifecycle(
        checkMainThread: () -> Unit = {},
        debugActivities: Set<String> = emptySet(),
        installed: MutableList<String> = mutableListOf(),
        removeProd: () -> Unit = {
            installed.removeAll { it !in debugActivities }
        },
    ) = ScannerLifecycle<String, Unit>(
        checkMainThread = checkMainThread,
        isDebuggable = debugActivities::contains,
        install = { activity, _ -> installed += activity },
        removeProd = removeProd,
    )
}
