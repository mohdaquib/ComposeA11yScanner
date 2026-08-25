package com.composea11yscanner

import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.model.ScannerState
import java.time.Duration
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ComposeA11yScannerIntegrationTest {
    private val config = ScannerConfig(enabledRules = emptySet(), autoScan = false)

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        appContext.applicationInfo.flags = appContext.applicationInfo.flags or FLAG_DEBUGGABLE
        ComposeA11yScanner.resetForTests()
    }

    @After
    fun tearDown() {
        ComposeA11yScanner.resetForTests()
    }

    @Test
    fun `queued resume cannot recreate explicitly uninstalled overlay`() {
        val activity = activity()
        ComposeA11yScanner.prepare(activity)
        activity.window.decorView.post { ComposeA11yScanner.resume(activity, config) }

        ComposeA11yScanner.uninstall(activity)
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(activity in ComposeA11yScanner.installedActivitiesForTests())
        assertNull(ComposeA11yScanner.overlayForTests(activity))

        ComposeA11yScanner.pause(activity)
        ComposeA11yScanner.resume(activity, config)

        assertTrue(activity in ComposeA11yScanner.installedActivitiesForTests())
        assertTrue(ComposeA11yScanner.overlayForTests(activity)?.parent != null)
    }

    @Test
    fun `repeated manual installation remains single and detach removes overlay`() {
        val activity = activity()

        ComposeA11yScanner.install(activity, config)
        val overlay = ComposeA11yScanner.overlayForTests(activity)
        val controller = ComposeA11yScanner.controllerForTests(activity)
        ComposeA11yScanner.install(activity, config)

        assertEquals(listOf(activity), ComposeA11yScanner.installedActivitiesForTests())
        assertSame(overlay, ComposeA11yScanner.overlayForTests(activity))
        assertSame(controller, ComposeA11yScanner.controllerForTests(activity))

        ComposeA11yScanner.uninstall(activity)

        assertNull(overlay?.parent)
        assertNull(ComposeA11yScanner.controllerForTests(activity))
        assertNull(ComposeA11yScanner.activeActivityForTests())
    }

    @Test
    fun `detach cancels queued initial and rescan callbacks`() {
        val activity = activity()
        val autoConfig = config.copy(autoScan = true)

        ComposeA11yScanner.install(activity, autoConfig)
        val overlay = ComposeA11yScanner.overlayForTests(activity)
        val controller = ComposeA11yScanner.controllerForTests(activity)
        ComposeA11yScanner.notifyScreenChanged()
        ComposeA11yScanner.uninstall(activity)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))

        assertNull(overlay?.parent)
        assertTrue(controller?.currentState is ScannerState.Idle)
        assertNull(ComposeA11yScanner.controllerForTests(activity))
    }

    @Test
    fun `manual routing resumes after automatic activity pauses`() {
        val manual = activity()
        val automatic = activity()
        ComposeA11yScanner.resetForTests()

        ComposeA11yScanner.install(manual, config)
        ComposeA11yScanner.resume(automatic, config)

        assertSame(automatic, ComposeA11yScanner.activeActivityForTests())
        ComposeA11yScanner.install(manual, config)
        assertSame(automatic, ComposeA11yScanner.activeActivityForTests())
        ComposeA11yScanner.triggerScan()
        assertTrue(ComposeA11yScanner.controllerForTests(automatic)?.currentState is ScannerState.Scanning)
        ComposeA11yScanner.notifyScreenChanged()
        assertTrue(ComposeA11yScanner.controllerForTests(automatic)?.currentState is ScannerState.Idle)

        ComposeA11yScanner.pause(automatic)

        assertSame(manual, ComposeA11yScanner.activeActivityForTests())
        ComposeA11yScanner.triggerScan()
        assertTrue(ComposeA11yScanner.controllerForTests(manual)?.currentState is ScannerState.Scanning)
        ComposeA11yScanner.notifyScreenChanged()
        assertTrue(ComposeA11yScanner.controllerForTests(manual)?.currentState is ScannerState.Idle)
    }

    @Test
    fun `trusted triggers quietly no-op before installation and after disable`() = runBlocking {
        val activity = activity()
        ComposeA11yScanner.resetForTests()
        activity.applicationInfo.flags = activity.applicationInfo.flags and FLAG_DEBUGGABLE.inv()
        ComposeA11yScanner.initialize(activity.applicationContext)

        assertNull(ComposeA11yScanner.triggerIfEnabled().firstOrNull())

        ComposeA11yScanner.toggleScanner(true)
        ComposeA11yScanner.resume(activity, config)
        assertTrue(activity in ComposeA11yScanner.installedActivitiesForTests())

        ComposeA11yScanner.toggleScanner(false)

        assertFalse(activity in ComposeA11yScanner.installedActivitiesForTests())
        assertNull(ComposeA11yScanner.triggerIfEnabled().firstOrNull())
    }

    private fun activity(): TestActivity =
        Robolectric.buildActivity(TestActivity::class.java).create().start().get()

    class TestActivity : ComponentActivity()
}
