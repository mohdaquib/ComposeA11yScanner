package com.composea11yscanner

internal class ScannerLifecycle<Activity, Config>(
    private val checkMainThread: () -> Unit,
    private val isDebuggable: (Activity) -> Boolean,
    private val install: (Activity, Config) -> Unit,
    private val removeProd: () -> Unit,
) {
    private val resumed = LinkedHashMap<Activity, Config>()

    @Volatile private var prodAllowed = false

    fun toggle(enabled: Boolean) {
        checkMainThread()
        if (prodAllowed == enabled) return
        prodAllowed = enabled
        if (enabled) resumed.forEach(install) else removeProd()
    }

    fun resume(activity: Activity, config: Config) {
        checkMainThread()
        resumed.remove(activity)
        resumed[activity] = config
        if (isDebuggable(activity) || prodAllowed) install(activity, config)
    }

    fun pause(activity: Activity) {
        checkMainThread()
        resumed.remove(activity)
    }

    fun resumedActivities(): List<Activity> = resumed.keys.toList()

    fun isAllowed(debuggable: Boolean) = debuggable || prodAllowed
}
