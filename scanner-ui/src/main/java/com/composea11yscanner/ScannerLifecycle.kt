package com.composea11yscanner

internal class ScannerLifecycle<Activity, Config>(
    private val checkMainThread: () -> Unit,
    private val isDebuggable: (Activity) -> Boolean,
    private val install: (Activity, Config) -> Unit,
    private val removeProd: () -> Unit,
) {
    private val resumed = LinkedHashMap<Activity, Config>()
    private val suppressed = mutableSetOf<Activity>()

    @Volatile private var prodAllowed = false

    fun toggle(enabled: Boolean) {
        checkMainThread()
        if (prodAllowed == enabled) return
        prodAllowed = enabled
        if (enabled) {
            resumed.filterKeys { it !in suppressed }.forEach(install)
        } else {
            removeProd()
        }
    }

    fun resume(activity: Activity, config: Config) {
        checkMainThread()
        resumed.remove(activity)
        resumed[activity] = config
        suppressed.remove(activity)
        if (isDebuggable(activity) || prodAllowed) install(activity, config)
    }

    fun pause(activity: Activity) {
        checkMainThread()
        resumed.remove(activity)
    }

    fun uninstall(activity: Activity) {
        checkMainThread()
        suppressed += activity
    }

    fun destroy(activity: Activity) {
        checkMainThread()
        resumed.remove(activity)
        suppressed.remove(activity)
    }

    fun resumedActivities(): List<Activity> = resumed.keys.filterNot(suppressed::contains)

    fun isAllowed(debuggable: Boolean) = debuggable || prodAllowed
}
