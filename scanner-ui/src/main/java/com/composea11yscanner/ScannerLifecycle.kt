package com.composea11yscanner

internal class ScannerLifecycle<Activity, Config>(
    private val checkMainThread: () -> Unit,
    private val isDebuggable: (Activity) -> Boolean,
    private val install: (Activity, Config) -> Unit,
    private val removeProd: () -> Unit,
) {
    private val resumed = LinkedHashMap<Activity, Config>()
    private val pending = mutableSetOf<Activity>()
    private val suppressed = mutableSetOf<Activity>()

    @Volatile private var prodAllowed = false

    fun toggle(enabled: Boolean) {
        checkMainThread()
        if (prodAllowed == enabled) return
        prodAllowed = enabled
        if (enabled) resumed.forEach(install) else removeProd()
    }

    fun prepare(activity: Activity) {
        checkMainThread()
        pending += activity
    }

    fun resume(activity: Activity, config: Config) {
        checkMainThread()
        pending.remove(activity)
        if (activity in suppressed) return
        resumed.remove(activity)
        resumed[activity] = config
        if (isDebuggable(activity) || prodAllowed) install(activity, config)
    }

    fun pause(activity: Activity) {
        checkMainThread()
        pending.remove(activity)
        resumed.remove(activity)
        suppressed.remove(activity)
    }

    fun uninstall(activity: Activity) {
        checkMainThread()
        val tracked = activity in pending || activity in resumed
        resumed.remove(activity)
        if (tracked) suppressed += activity
    }

    fun destroy(activity: Activity) {
        checkMainThread()
        pending.remove(activity)
        resumed.remove(activity)
        suppressed.remove(activity)
    }

    fun resumedActivities(): List<Activity> = resumed.keys.toList()

    fun isAllowed(debuggable: Boolean) = debuggable || prodAllowed

    fun reset() {
        resumed.clear()
        pending.clear()
        suppressed.clear()
        prodAllowed = false
    }
}

internal fun <Activity, Entry> selectEntry(
    resumedActivities: List<Activity>,
    entries: Map<Activity, Entry>,
    isAutomatic: (Entry) -> Boolean,
): Entry? = resumedActivities
    .asReversed()
    .firstNotNullOfOrNull { entries[it]?.takeIf(isAutomatic) }
    ?: entries.values.lastOrNull { !isAutomatic(it) }
