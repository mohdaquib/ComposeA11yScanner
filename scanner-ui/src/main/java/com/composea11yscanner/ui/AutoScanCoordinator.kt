package com.composea11yscanner.ui

import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.ui.platform.ComposeView
import com.composea11yscanner.core.model.ScannerState

/** Coordinates initial scans and stable rescans independently of installation and UI rendering. */
internal class AutoScanCoordinator(
    val controller: A11yScannerController,
    val overlayView: ComposeView,
    private val autoScan: Boolean,
    private val screenSnapshotProvider: () -> ScreenSnapshot?,
) : ViewTreeObserver.OnPreDrawListener {
    private var baselineFingerprint: ScreenFingerprint? = null
    private var completedScanId: String? = null
    private var lastCheckUptimeMillis = 0L
    private var initialVerificationSnapshot: ScreenSnapshot? = null
    private var rescanVerificationSnapshot: ScreenSnapshot? = null
    private val initialStabilityTracker = newStabilityTracker()
    private val rescanStabilityTracker = newStabilityTracker()

    private val initialScanRunnable = Runnable {
        val now = SystemClock.uptimeMillis()
        val snapshot = screenSnapshotProvider() ?: run {
            resetInitialStability()
            scheduleInitialScanCheck()
            return@Runnable
        }
        val verification = initialVerificationSnapshot
        if (verification != null) {
            if (snapshot == verification) {
                Log.d(LOG_TAG, "Initial host stable; starting scan")
                resetInitialStability()
                controller.startScan()
                return@Runnable
            }
            Log.d(LOG_TAG, "Initial semantics changed during verification; settling again: ${snapshot.readiness}")
            resetInitialStability()
            initialStabilityTracker.observe(snapshot.readiness, now)
            scheduleInitialScanCheck()
            return@Runnable
        }
        if (initialStabilityTracker.observe(snapshot.readiness, now)) {
            initialVerificationSnapshot = snapshot
            Log.d(LOG_TAG, "Initial semantics stable; verifying once more before scanning")
        } else {
            Log.d(LOG_TAG, "Waiting for sustained initial semantic stability: ${snapshot.readiness}")
        }
        scheduleInitialScanCheck()
    }

    private val rescanRunnable = Runnable {
        val now = SystemClock.uptimeMillis()
        val snapshot = screenSnapshotProvider() ?: run {
            resetRescanStability()
            scheduleRescan()
            return@Runnable
        }
        val verification = rescanVerificationSnapshot
        if (verification != null) {
            if (snapshot == verification) {
                Log.d(LOG_TAG, "Destination stable; starting rescan")
                resetRescanStability()
                controller.startScan()
                return@Runnable
            }
            Log.d(LOG_TAG, "Rescan semantics changed during verification; settling again: ${snapshot.readiness}")
            resetRescanStability()
            rescanStabilityTracker.observe(snapshot.readiness, now)
            scheduleRescan()
            return@Runnable
        }
        if (rescanStabilityTracker.observe(snapshot.readiness, now)) {
            rescanVerificationSnapshot = snapshot
            Log.d(LOG_TAG, "Rescan semantics stable; verifying once more before scanning")
        } else {
            Log.d(LOG_TAG, "Waiting for sustained rescan semantic stability: ${snapshot.readiness}")
        }
        scheduleRescan()
    }

    fun attach() {
        overlayView.rootView.viewTreeObserver.addOnPreDrawListener(this)
        if (autoScan) requestInitialScan()
    }

    override fun onPreDraw(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastCheckUptimeMillis < SCREEN_CHECK_INTERVAL_MILLIS) return true
        lastCheckUptimeMillis = now
        val complete = controller.currentState as? ScannerState.Complete
        if (complete == null) {
            completedScanId = null
            baselineFingerprint = null
            return true
        }
        val currentSnapshot = screenSnapshotProvider() ?: return true
        if (completedScanId != complete.result.scanId) {
            completedScanId = complete.result.scanId
            baselineFingerprint = currentSnapshot.fingerprint
            Log.d(LOG_TAG, "Scan baseline recorded: ${currentSnapshot.fingerprint}")
            return true
        }
        if (baselineFingerprint != currentSnapshot.fingerprint) {
            Log.d(LOG_TAG, "Screen changed: previous=$baselineFingerprint, current=${currentSnapshot.fingerprint}")
            baselineFingerprint = null
            completedScanId = null
            controller.clearState()
            if (autoScan) {
                resetRescanStability()
                rescanStabilityTracker.observe(currentSnapshot.readiness, now)
                scheduleRescan()
            }
        }
        return true
    }

    fun notifyScreenChanged() {
        Log.d(LOG_TAG, "Screen change explicitly notified")
        baselineFingerprint = null
        completedScanId = null
        controller.clearState()
        if (!autoScan) return
        val snapshot = screenSnapshotProvider()
        if (snapshot == null) requestInitialScan() else {
            resetRescanStability()
            rescanStabilityTracker.observe(snapshot.readiness, SystemClock.uptimeMillis())
            scheduleRescan()
        }
    }

    fun detach() {
        val observer = overlayView.rootView.viewTreeObserver
        if (observer.isAlive) observer.removeOnPreDrawListener(this)
        overlayView.removeCallbacks(initialScanRunnable)
        overlayView.removeCallbacks(rescanRunnable)
        resetInitialStability()
        resetRescanStability()
        overlayView.disposeComposition()
        (overlayView.parent as? ViewGroup)?.removeView(overlayView)
        controller.stopScan()
        controller.destroy()
    }

    private fun requestInitialScan() {
        resetInitialStability()
        screenSnapshotProvider()?.readiness?.let {
            initialStabilityTracker.observe(it, SystemClock.uptimeMillis())
        }
        scheduleInitialScanCheck()
    }

    private fun resetInitialStability() {
        initialVerificationSnapshot = null
        initialStabilityTracker.reset()
    }

    private fun resetRescanStability() {
        rescanVerificationSnapshot = null
        rescanStabilityTracker.reset()
    }

    private fun scheduleInitialScanCheck() {
        overlayView.removeCallbacks(initialScanRunnable)
        overlayView.postDelayed(initialScanRunnable, SETTLE_DELAY_MILLIS)
    }

    private fun scheduleRescan() {
        overlayView.removeCallbacks(rescanRunnable)
        overlayView.postDelayed(rescanRunnable, SETTLE_DELAY_MILLIS)
    }

    private companion object {
        const val LOG_TAG = "ComposeA11yLifecycle"
        const val SCREEN_CHECK_INTERVAL_MILLIS = 500L
        const val SETTLE_DELAY_MILLIS = 250L
        const val REQUIRED_STABLE_SAMPLES = 4
        const val MINIMUM_STABLE_DURATION_MILLIS = 750L

        fun newStabilityTracker() = SemanticStabilityTracker(
            requiredStableSamples = REQUIRED_STABLE_SAMPLES,
            minimumStableDurationMillis = MINIMUM_STABLE_DURATION_MILLIS,
        )
    }
}

internal data class ScreenSnapshot(
    val fingerprint: ScreenFingerprint,
    val readiness: ReadinessFingerprint,
)
