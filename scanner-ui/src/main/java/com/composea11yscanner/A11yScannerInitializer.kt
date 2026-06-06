package com.composea11yscanner

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.startup.Initializer
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.rules.ScannerRules

/**
 * AndroidX App Startup initializer for installing [ComposeA11yScanner] in debug builds.
 *
 * Configure the scanner from the app manifest with:
 * - `a11y_scanner_min_touch_target`
 * - `a11y_scanner_min_contrast`
 * - `a11y_scanner_auto_scan`
 */
class A11yScannerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val appContext = context.applicationContext
        if (!appContext.isDebuggable()) return

        val application = appContext as? Application ?: return
        val config = appContext.readScannerConfig()

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    val componentActivity = activity as? ComponentActivity ?: return
                    componentActivity.window.decorView.post {
                        if (componentActivity.isDestroyed) return@post
                        ComposeA11yScanner.install(componentActivity, config)
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun Context.isDebuggable(): Boolean =
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private fun Context.readScannerConfig(): ScannerConfig {
        val metadata = applicationMetadata()
        val defaults = ScannerConfig(enabledRules = emptySet())
        return ScannerConfig(
            enabledRules = ScannerRules.allRuleIds().toSet(),
            minTouchTargetDp = metadata.intValue(
                key = META_MIN_TOUCH_TARGET,
                defaultValue = defaults.minTouchTargetDp,
            ),
            minContrastRatio = metadata.floatValue(
                key = META_MIN_CONTRAST,
                defaultValue = defaults.minContrastRatio,
            ),
            autoScan = metadata.booleanValue(
                key = META_AUTO_SCAN,
                defaultValue = defaults.autoScan,
            ),
        )
    }

    private fun Context.applicationMetadata(): Bundle {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return appInfo.metaData ?: Bundle.EMPTY
    }

    private fun Bundle.intValue(key: String, defaultValue: Int): Int =
        when (val value = get(key)) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }

    private fun Bundle.floatValue(key: String, defaultValue: Float): Float =
        when (val value = get(key)) {
            is Float -> value
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: defaultValue
            else -> defaultValue
        }

    private fun Bundle.booleanValue(key: String, defaultValue: Boolean): Boolean =
        when (val value = get(key)) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: defaultValue
            else -> defaultValue
        }

    private companion object {
        const val META_MIN_TOUCH_TARGET = "a11y_scanner_min_touch_target"
        const val META_MIN_CONTRAST = "a11y_scanner_min_contrast"
        const val META_AUTO_SCAN = "a11y_scanner_auto_scan"
    }
}
