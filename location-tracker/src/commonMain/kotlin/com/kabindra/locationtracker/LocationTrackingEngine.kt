package com.kabindra.locationtracker

import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingMode
import com.kabindra.locationtracker.schedule.ScheduleEvaluator
import com.kabindra.locationtracker.schedule.currentLocalScheduleTime
import com.kabindra.locationtracker.session.CheckInOutAction
import com.kabindra.locationtracker.session.CheckInOutListener
import com.kabindra.locationtracker.session.LocationTrackingListener
import com.kabindra.locationtracker.session.LocationTrackingSession
import com.kabindra.locationtracker.session.TrackingStopReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The persistent, platform-owned tracking entry point. Hosts initialize it once at application
 * startup, before UI composition, and keep HTTP/upload details in [LocationTrackingListener].
 */
object LocationTrackingEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scheduleJob: Job? = null

    /** Registers host callbacks before any restored event is delivered. Safe to call repeatedly. */
    fun initialize(
        listener: LocationTrackingListener?,
        developerMode: Boolean = false,
        checkInOutListener: CheckInOutListener? = null,
    ) {
        LocationTrackingSession.initialize(listener, developerMode, checkInOutListener)
        restoreIfActive()
    }

    /** Persists policy/configuration first, then asks the platform-owned collector to start. */
    fun start(policy: LocationTrackerPolicy, config: TrackingConfig = TrackingConfig()): Boolean {
        LocationTrackingSession.updatePolicy(policy)
        configureSchedule(policy)
        if (!LocationTrackingSession.canStart()) return false
        return begin(config)
    }

    /** Stops the native collector with an explicit command, even when no UI binding exists. */
    fun stop(reason: TrackingStopReason = TrackingStopReason.USER) {
        log("stop_requested", "reason=$reason")
        PlatformTrackingController.stop(reason)
        // The platform controller normally stops the session too. This fallback keeps UI state
        // correct if the native service/manager has already gone away.
        LocationTrackingSession.stop(reason)
        configureSchedule(LocationTrackingSession.state.value.activePolicy)
    }

    /**
     * Applies the sole host-provided backend policy. TIME_RANGE and checked-in attendance policies
     * reconcile immediately; future schedule boundaries are coordinated by this engine.
     */
    fun updatePolicy(policy: LocationTrackerPolicy) {
        log("policy_applied", "mode=${policy.trackingMode}, enabled=${policy.isTrackingEnabled}")
        val stoppedByPolicy = LocationTrackingSession.updatePolicy(policy)
        if (stoppedByPolicy) PlatformTrackingController.stop(TrackingStopReason.POLICY)
        configureSchedule(policy)
        reconcile(policy)
    }

    /** Restores shared durable state and reattaches the platform collector without Compose. */
    fun restoreIfActive() {
        log("restore_requested", "active=${LocationTrackingSession.state.value.isActive}")
        val wasActive = LocationTrackingSession.state.value.isActive
        LocationTrackingSession.restoreIfActive()
        if (wasActive && !LocationTrackingSession.state.value.isActive) {
            PlatformTrackingController.stop(TrackingStopReason.POLICY)
        }
        val policy = LocationTrackingSession.state.value.activePolicy ?: return
        configureSchedule(policy)
        reconcile(policy, restoreNativeCollector = true)
    }

    /** Called by a platform schedule wake-up after its application entry point registered the host. */
    internal fun reconcileNow() {
        LocationTrackingSession.state.value.activePolicy?.let(::reconcile)
    }

    suspend fun requestCheckIn(): Boolean = requestCheckInOut(CheckInOutAction.CHECK_IN)

    suspend fun requestCheckOut(): Boolean = requestCheckInOut(CheckInOutAction.CHECK_OUT)

    private suspend fun requestCheckInOut(action: CheckInOutAction): Boolean {
        val updatedPolicy = LocationTrackingSession.requestCheckInOut(action) ?: return false
        updatePolicy(updatedPolicy)
        return when (action) {
            CheckInOutAction.CHECK_IN -> updatedPolicy.isCheckedIn
            CheckInOutAction.CHECK_OUT -> !updatedPolicy.isCheckedIn
        }
    }

    private fun reconcile(policy: LocationTrackerPolicy, restoreNativeCollector: Boolean = false) {
        val active = LocationTrackingSession.state.value.isActive
        val allowed = LocationTrackingSession.isTrackingAllowed()
        if (active && !allowed) {
            stop(TrackingStopReason.POLICY)
            return
        }
        if (active && restoreNativeCollector) {
            PlatformTrackingController.restore(LocationTrackingSession.state.value.activeConfig ?: TrackingConfig())
        }
        if (active) return

        val policyCanAutoStart = when (policy.trackingMode) {
            TrackingMode.TIME_RANGE -> allowed
            TrackingMode.CHECK_IN_OUT -> policy.isCheckedIn && allowed
        }
        if (policyCanAutoStart && PlatformTrackingController.canStart()) {
            begin(LocationTrackingSession.state.value.activeConfig ?: TrackingConfig())
        }
    }

    private fun begin(config: TrackingConfig): Boolean {
        if (!PlatformTrackingController.canStart()) return false
        log("start_requested", "session=${LocationTrackingSession.state.value.sessionId ?: "new"}")
        // This is synchronous persistence, so session state is already correct when the UI resumes.
        LocationTrackingSession.markStarted(config)
        if (PlatformTrackingController.start(config)) return true
        // A native start can fail if the OS revoked permission after the host's last check.
        LocationTrackingSession.stop(TrackingStopReason.POLICY)
        return false
    }

    private fun configureSchedule(policy: LocationTrackerPolicy?) {
        scheduleJob?.cancel()
        scheduleJob = null
        // Android additionally persists a one-shot alarm so an inactive session can be reconciled
        // after process death. iOS uses significant-location monitoring as a best-effort signal.
        PlatformTrackingController.schedule(policy)
        if (policy?.isTrackingEnabled != true || policy.trackingMode != TrackingMode.TIME_RANGE ||
            policy.scheduleWindow == null) return

        scheduleJob = scope.launch {
            while (true) {
                val now = currentLocalScheduleTime()
                delay(
                    ScheduleEvaluator.millisUntilNextTransition(
                        policy.scheduleWindow,
                        now.hour,
                        now.minute,
                    ),
                )
                // The policy may have been replaced while this coroutine was waiting.
                val currentPolicy = LocationTrackingSession.state.value.activePolicy
                if (currentPolicy != null) {
                    log("schedule_boundary", "mode=${currentPolicy.trackingMode}")
                    reconcile(currentPolicy)
                }
            }
        }
    }

    private fun log(event: String, details: String) {
        println("LocationTrackingEngine event=$event $details")
    }
}

/** Native lifecycle owner: Android foreground service or iOS app-lifecycle CLLocation coordinator. */
internal expect object PlatformTrackingController {
    fun canStart(): Boolean
    fun start(config: TrackingConfig): Boolean
    fun restore(config: TrackingConfig)
    fun stop(reason: TrackingStopReason)
    fun schedule(policy: LocationTrackerPolicy?)
}
