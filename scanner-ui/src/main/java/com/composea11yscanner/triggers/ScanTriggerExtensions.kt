package com.composea11yscanner.triggers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.composea11yscanner.ComposeA11yScanner

/**
 * Starts a ComposeA11yScanner scan when this element is long-pressed.
 *
 * Kept as a consumer-side convenience extension in `:scanner-ui`; scanner core has no
 * dependency on gestures, sensors, Android framework callbacks, or Compose modifiers.
 */
fun Modifier.scanOnLongPress(
    enabled: Boolean = true,
    onScanRequested: () -> Unit = { ComposeA11yScanner.triggerScan() },
): Modifier {
    if (!enabled) return this
    return pointerInput(onScanRequested) {
        detectTapGestures(onLongPress = { onScanRequested() })
    }
}

/**
 * Starts a ComposeA11yScanner scan when the device is shaken.
 *
 * Call from a composable screen that wants shake-triggered scans. If no accelerometer is
 * present, this quietly does nothing.
 */
@Composable
fun scanOnShake(
    enabled: Boolean = true,
    shakeThresholdG: Float = 2.7f,
    minTriggerIntervalMillis: Long = 1_500L,
    onScanRequested: () -> Unit = { ComposeA11yScanner.triggerScan() },
) {
    val context = LocalContext.current
    val currentOnScanRequested = rememberUpdatedState(onScanRequested)

    DisposableEffect(context, enabled, shakeThresholdG, minTriggerIntervalMillis) {
        if (!enabled) return@DisposableEffect onDispose { }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return@DisposableEffect onDispose { }
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose { }

        var lastTriggerAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                val gForce = kotlin.math.sqrt(gX * gX + gY * gY + gZ * gZ)
                val now = SystemClock.elapsedRealtime()

                if (gForce >= shakeThresholdG && now - lastTriggerAt >= minTriggerIntervalMillis) {
                    lastTriggerAt = now
                    currentOnScanRequested.value()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
        )

        onDispose { sensorManager.unregisterListener(listener) }
    }
}
