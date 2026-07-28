package com.kabindra.locationtracker.session

import com.kabindra.locationtracker.filter.DistanceFilter
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.schedule.ScheduleEvaluator
import com.kabindra.locationtracker.schedule.currentLocalScheduleTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TrackingStopReason { USER, POLICY }

sealed interface LocationTrackingEvent {
    val location: TrackedLocation?

    data class Started(override val location: TrackedLocation) : LocationTrackingEvent
    data class LocationUpdated(override val location: TrackedLocation) : LocationTrackingEvent
    data class Stopped(
        override val location: TrackedLocation?,
        val reason: TrackingStopReason,
    ) : LocationTrackingEvent
}

/**
 * Implemented by the host application. Return true only after its backend has accepted the event.
 * The SDK deliberately has no HTTP client or backend endpoint.
 */
fun interface LocationTrackingListener {
    suspend fun onTrackingEvent(event: LocationTrackingEvent): Boolean
}

data class TrackingSessionState(
    val isActive: Boolean = false,
    val hasDeliveredStart: Boolean = false,
    val lastKnownLocation: TrackedLocation? = null,
    val lastSuccessfullyDeliveredLocation: TrackedLocation? = null,
    val pendingEvents: List<LocationTrackingEvent> = emptyList(),
    val lastError: String? = null,
) {
    val pendingEventCount: Int get() = pendingEvents.size
}

/** Platform-backed persistent store. Android uses SharedPreferences and iOS uses NSUserDefaults. */
expect object TrackingSessionStore {
    fun read(): TrackingSessionState
    fun write(state: TrackingSessionState)
}

/**
 * Process-wide location session. Initialize it from the Android Application/iOS app entry point
 * so a recreated platform tracker can use the host uploader without a Compose screen.
 */
object LocationTrackingSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var listener: LocationTrackingListener? = null
    private var policy = LocationTrackerPolicy()
    private val _state = MutableStateFlow(TrackingSessionStore.read())
    val state: StateFlow<TrackingSessionState> = _state.asStateFlow()

    fun initialize(listener: LocationTrackingListener?) {
        this.listener = listener
        retryPendingEvents()
    }

    fun updatePolicy(policy: LocationTrackerPolicy): Boolean {
        this.policy = policy
        val now = currentLocalScheduleTime()
        val remainsEligible = ScheduleEvaluator.shouldTrackLocation(policy, now.hour, now.minute)
        if (_state.value.isActive && !remainsEligible) {
            stop(TrackingStopReason.POLICY)
            return true
        }
        return false
    }

    fun canStart(): Boolean {
        val now = currentLocalScheduleTime()
        val shouldTrackLocation = !_state.value.isActive &&
                ScheduleEvaluator.shouldTrackLocation(policy, now.hour, now.minute)
        println("Checking if tracking can start... ${_state.value.isActive} - $policy - $now - $shouldTrackLocation")
        return shouldTrackLocation
    }

    fun markStarted() {
        if (_state.value.isActive) return
        mutate {
            it.copy(
                isActive = true,
                hasDeliveredStart = false,
                lastSuccessfullyDeliveredLocation = null,
                lastError = null,
            )
        }
        retryPendingEvents()
    }

    fun onLocation(location: TrackedLocation) {
        scope.launch {
            mutex.withLock {
                var current = _state.value.copy(lastKnownLocation = location)
                val event = when {
                    !current.isActive -> null
                    !current.hasDeliveredStart -> LocationTrackingEvent.Started(location)
                    DistanceFilter.isSignificantMovement(
                        current.lastSuccessfullyDeliveredLocation,
                        location,
                        policy.minDistanceThresholdMeters,
                    ) -> LocationTrackingEvent.LocationUpdated(location)

                    else -> null
                }
                if (event != null) current =
                    current.copy(pendingEvents = current.pendingEvents + event)
                saveLocked(current)
            }
            retryPendingEvents()
        }
    }

    /** Used by platform trackers before accepting the next fix or restoring a session. */
    fun isTrackingAllowed(): Boolean {
        val now = currentLocalScheduleTime()
        return ScheduleEvaluator.shouldTrackLocation(policy, now.hour, now.minute)
    }

    fun stop(reason: TrackingStopReason) {
        val current = _state.value
        if (!current.isActive) return
        // Persist this synchronously so the UI immediately changes from Stop-enabled to Start-eligible.
        mutate {
            it.copy(
                isActive = false,
                pendingEvents = it.pendingEvents + LocationTrackingEvent.Stopped(
                    location = it.lastKnownLocation,
                    reason = reason,
                ),
            )
        }
        retryPendingEvents()
    }

    fun retryPendingEvents() {
        scope.launch {
            mutex.withLock {
                val uploader = listener ?: return@withLock
                var current = _state.value
                while (current.pendingEvents.isNotEmpty()) {
                    val event = current.pendingEvents.first()
                    val delivered = runCatching { uploader.onTrackingEvent(event) }.getOrElse {
                        current = current.copy(lastError = it.message ?: "Backend delivery failed")
                        saveLocked(current)
                        return@withLock
                    }
                    if (!delivered) {
                        current = current.copy(lastError = "Backend did not accept tracking event")
                        saveLocked(current)
                        return@withLock
                    }
                    current = current.copy(
                        hasDeliveredStart = current.hasDeliveredStart || event is LocationTrackingEvent.Started,
                        lastSuccessfullyDeliveredLocation = event.location
                            ?: current.lastSuccessfullyDeliveredLocation,
                        pendingEvents = current.pendingEvents.drop(1),
                        lastError = null,
                    )
                    saveLocked(current)
                }
            }
        }
    }

    private fun mutate(transform: (TrackingSessionState) -> TrackingSessionState) {
        val next = transform(_state.value)
        _state.value = next
        TrackingSessionStore.write(next)
    }

    private fun saveLocked(next: TrackingSessionState) {
        _state.value = next
        TrackingSessionStore.write(next)
    }
}
