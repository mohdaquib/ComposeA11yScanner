package com.composea11yscanner.triggers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow.newInstanceOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ScanTriggerExtensionsTest {
    @Test
    fun `disabled long press returns original modifier`() {
        val modifier = Modifier

        val result = modifier.scanOnLongPress(enabled = false) {
            error("Disabled long press must not invoke its callback")
        }

        assertSame(modifier, result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `disabled shake registers no sensor listener`() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowSensorManager = shadowOf(sensorManager)
        shadowSensorManager.addSensor(
            Sensor.TYPE_ACCELEROMETER,
            newInstanceOf(Sensor::class.java),
        )
        val composeView = ComposeView(activity)
        activity.setContentView(composeView)

        composeView.setContent {
            scanOnShake(enabled = false) {
                error("Disabled shake must not invoke its callback")
            }
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(shadowSensorManager.listeners.isEmpty())
    }

    class TestActivity : ComponentActivity()
}
