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

private val defaultScanRequest: () -> Unit = { ComposeA11yScanner.triggerIfEnabled() }

/**
 * Starts a ComposeA11yScanner scan when this element is long-pressed.
 *
 * The default callback quietly does nothing when the scanner is disabled or not installed.
 * Scanner core has no dependency on gestures, sensors, framework callbacks, or Compose modifiers.
 *
 * @param enabled Whether long-press scanning is active.
 * @param onScanRequested Callback invoked after a long press.
 * @return Modifier with the long-press scanner trigger installed.
 */
fun Modifier.scanOnLongPress(
    enabled: Boolean = true,
    onScanRequested: () -> Unit = defaultScanRequest,
): Modifier {
    if (!enabled) return this
    return pointerInput(onScanRequested) {
        detectTapGestures(onLongPress = { onScanRequested() })
    }
}

/**
 * Starts a ComposeA11yScanner scan when the device is shaken.
 *
 * Call from a composable screen that wants shake-triggered scans. If no accelerometer is present,
 * or the scanner is disabled or not installed, the default callback quietly does nothing.
 *
 * @param enabled Whether shake scanning is active.
 * @param shakeThresholdG Required acceleration force in Gs.
 * @param minTriggerIntervalMillis Minimum time between scan triggers.
 * @param onScanRequested Callback invoked after a qualifying shake.
 */
@Composable
fun scanOnShake(
    enabled: Boolean = true,
    shakeThresholdG: Float = 2.7f,
    minTriggerIntervalMillis: Long = 1_500L,
    onScanRequested: () -> Unit = defaultScanRequest,
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
