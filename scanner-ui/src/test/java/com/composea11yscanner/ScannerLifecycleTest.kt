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
    fun `enable before resume installs on resume`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(installed = installed)

        lifecycle.toggle(true)
        lifecycle.resume("first", Unit)

        assertEquals(listOf("first"), installed)
    }

    @Test
    fun `enable after resume installs every resumed activity`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(installed = installed)
        lifecycle.resume("first", Unit)
        lifecycle.resume("second", Unit)

        lifecycle.toggle(true)

        assertEquals(listOf("first", "second"), installed)
    }

    @Test
    fun `paused activity is not reinstalled`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(installed = installed)
        lifecycle.resume("first", Unit)
        lifecycle.resume("second", Unit)
        lifecycle.pause("first")

        lifecycle.toggle(true)

        assertEquals(listOf("second"), installed)
    }

    @Test
    fun `pause exposes previous resumed activity`() {
        val lifecycle = lifecycle()
        lifecycle.resume("first", Unit)
        lifecycle.resume("second", Unit)

        lifecycle.pause("second")

        assertEquals(listOf("first"), lifecycle.resumedActivities())
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
    fun `manual-only uninstall does not suppress later resume`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(installed = installed)
        lifecycle.uninstall("manual")
        lifecycle.toggle(true)

        lifecycle.resume("manual", Unit)

        assertEquals(listOf("manual"), installed)
    }

    @Test
    fun `manual routing survives removal of latest entry`() {
        val entries = linkedMapOf(
            "first" to Entry("first", automatic = false),
            "second" to Entry("second", automatic = false),
        )

        assertEquals("second", selectEntry(emptyList(), entries, Entry::automatic)?.id)
        entries.remove("second")
        assertEquals("first", selectEntry(emptyList(), entries, Entry::automatic)?.id)
    }

    @Test
    fun `resumed automatic entry takes priority over manual fallback`() {
        val entries = linkedMapOf(
            "manual" to Entry("manual", automatic = false),
            "first" to Entry("first", automatic = true),
            "second" to Entry("second", automatic = true),
        )

        val active = selectEntry(
            resumedActivities = listOf("first", "second"),
            entries = entries,
            isAutomatic = Entry::automatic,
        )
        val fallback = selectEntry(
            resumedActivities = emptyList(),
            entries = entries,
            isAutomatic = Entry::automatic,
        )

        assertEquals("second", active?.id)
        assertEquals("manual", fallback?.id)
    }

    @Test
    fun `disable removes production installs`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(installed = installed)
        lifecycle.toggle(true)
        lifecycle.resume("first", Unit)

        lifecycle.toggle(false)

        assertTrue(installed.isEmpty())
        assertFalse(lifecycle.isAllowed(debuggable = false))
    }

    @Test
    fun `debug activity remains installed when production is disabled`() {
        val installed = mutableListOf<String>()
        val lifecycle = lifecycle(
            debugActivities = setOf("debug"),
            installed = installed,
        )
        lifecycle.resume("debug", Unit)
        lifecycle.toggle(true)
        lifecycle.toggle(false)

        assertEquals(listOf("debug", "debug"), installed)
    }

    @Test
    fun `repeated toggle is idempotent`() {
        var removals = 0
        val lifecycle = lifecycle(removeProd = { removals++ })

        lifecycle.toggle(true)
        lifecycle.toggle(true)
        lifecycle.toggle(false)
        lifecycle.toggle(false)

        assertEquals(1, removals)
    }

    @Test
    fun `worker thread rejection happens before state change`() {
        var mainThread = false
        val lifecycle = lifecycle(checkMainThread = { check(mainThread) })

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
