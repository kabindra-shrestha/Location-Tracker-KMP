package com.kabindra.locationtracker.session

import com.kabindra.locationtracker.filter.DistanceFilter
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.schedule.ScheduleEvaluator
import com.kabindra.locationtracker.schedule.currentLocalScheduleTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TrackingStopReason { USER, POLICY }

enum class TrackingEventKind { START, LOCATION, STOP }

enum class LocationSyncStatus { PENDING, SYNCED, FILTERED, FAILED }

/** Debug-only record of every location observed during the current tracking session. */
data class TrackedLocationDebugEntry(
    val location: TrackedLocation,
    val eventKind: TrackingEventKind,
    val syncStatus: LocationSyncStatus,
)

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
    val lastSyncTimestampMs: Long = 0L,
    val nextSyncTimestampMs: Long = 0L,
    val pendingEvents: List<LocationTrackingEvent> = emptyList(),
    val trackedLocations: List<TrackedLocationDebugEntry> = emptyList(),
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
    private var syncJob: Job? = null
    private val _state = MutableStateFlow(TrackingSessionStore.read())
    val state: StateFlow<TrackingSessionState> = _state.asStateFlow()
    private val _developerMode = MutableStateFlow(false)
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

    /** Enable [developerMode] only from a debug/developer host build. */
    fun initialize(listener: LocationTrackingListener?, developerMode: Boolean = false) {
        this.listener = listener
        _developerMode.value = developerMode
        retryPendingEvents()
    }

    fun updatePolicy(policy: LocationTrackerPolicy): Boolean {
        val intervalChanged = this.policy.syncIntervalMinutes != policy.syncIntervalMinutes
        this.policy = policy
        val now = currentLocalScheduleTime()
        val remainsEligible = ScheduleEvaluator.shouldTrackLocation(policy, now.hour, now.minute)
        if (_state.value.isActive && !remainsEligible) {
            stop(TrackingStopReason.POLICY)
            return true
        }
        if (_state.value.isActive && intervalChanged) restartSyncTimer()
        return false
    }

    fun canStart(): Boolean {
        val now = currentLocalScheduleTime()
        return !_state.value.isActive &&
                ScheduleEvaluator.shouldTrackLocation(policy, now.hour, now.minute)
    }

    fun markStarted() {
        if (_state.value.isActive) return
        mutate {
            it.copy(
                isActive = true,
                hasDeliveredStart = false,
                lastSuccessfullyDeliveredLocation = null,
                trackedLocations = emptyList(),
                lastError = null,
            )
        }
        restartSyncTimer()
        retryPendingEvents()
    }

    fun onLocation(location: TrackedLocation) {
        scope.launch {
            mutex.withLock {
                var current = _state.value.copy(lastKnownLocation = location)
                val event = when {
                    !current.isActive -> null
                    // Do not enqueue a second first-fix upload while the initial event is waiting
                    // for the host/backend acknowledgement.
                    !current.hasDeliveredStart && current.pendingEvents.none {
                        it is LocationTrackingEvent.Started
                    } -> LocationTrackingEvent.Started(location)

                    else -> null
                }
                current = current.copy(
                    pendingEvents = if (event == null) current.pendingEvents else current.pendingEvents + event,
                    trackedLocations = current.trackedLocations + TrackedLocationDebugEntry(
                        location = location,
                        eventKind = event?.kind() ?: TrackingEventKind.LOCATION,
                        // Raw fixes wait for the interval decision. PENDING means not yet uploaded.
                        syncStatus = LocationSyncStatus.PENDING,
                    ),
                )
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
        syncJob?.cancel()
        syncJob = null
        // Persist this synchronously so the UI immediately changes from Stop-enabled to Start-eligible.
        mutate {
            val stopEvent = LocationTrackingEvent.Stopped(
                location = it.lastKnownLocation,
                reason = reason,
            )
            it.copy(
                isActive = false,
                pendingEvents = it.pendingEvents + stopEvent,
                trackedLocations = it.lastKnownLocation?.let { location ->
                    it.trackedLocations + TrackedLocationDebugEntry(
                        location = location,
                        eventKind = TrackingEventKind.STOP,
                        syncStatus = LocationSyncStatus.PENDING,
                    )
                } ?: it.trackedLocations,
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
                        current = current.copy(
                            lastError = it.message ?: "Backend delivery failed",
                            trackedLocations = current.trackedLocations.updateStatus(
                                event,
                                LocationSyncStatus.FAILED
                            ),
                        )
                        saveLocked(current)
                        return@withLock
                    }
                    if (!delivered) {
                        current = current.copy(
                            lastError = "Backend did not accept tracking event",
                            trackedLocations = current.trackedLocations.updateStatus(
                                event,
                                LocationSyncStatus.FAILED
                            ),
                        )
                        saveLocked(current)
                        return@withLock
                    }
                    current = current.copy(
                        hasDeliveredStart = current.hasDeliveredStart || event is LocationTrackingEvent.Started,
                        lastSuccessfullyDeliveredLocation = event.location
                            ?: current.lastSuccessfullyDeliveredLocation,
                        pendingEvents = current.pendingEvents.drop(1),
                        trackedLocations = current.trackedLocations.updateStatus(
                            event,
                            LocationSyncStatus.SYNCED
                        ),
                        lastError = null,
                    )
                    saveLocked(current)
                }
            }
        }
    }

    private fun restartSyncTimer() {
        syncJob?.cancel()
        val intervalMs = policy.syncIntervalMinutes * 60_000L
        mutate { it.copy(nextSyncTimestampMs = nowMs() + intervalMs) }
        syncJob = scope.launch {
            while (_state.value.isActive) {
                delay(intervalMs)
                evaluateLatestLocationForSync()
            }
        }
    }

    private suspend fun evaluateLatestLocationForSync() {
        mutex.withLock {
            val current = _state.value
            val location = current.lastKnownLocation ?: return@withLock
            if (!current.isActive) return@withLock
            val shouldUpload = current.lastSuccessfullyDeliveredLocation == null ||
                DistanceFilter.isSignificantMovement(
                    current.lastSuccessfullyDeliveredLocation,
                    location,
                    policy.minDistanceThresholdMeters,
                )
            val event = if (shouldUpload) LocationTrackingEvent.LocationUpdated(location) else null
            val status = if (shouldUpload) LocationSyncStatus.PENDING else LocationSyncStatus.FILTERED
            val updatedEntries = current.trackedLocations.updateLatestLocationStatus(location, status)
            saveLocked(
                current.copy(
                    pendingEvents = if (event == null) current.pendingEvents else current.pendingEvents + event,
                    trackedLocations = updatedEntries,
                    lastSyncTimestampMs = nowMs(),
                    nextSyncTimestampMs = nowMs() + policy.syncIntervalMinutes * 60_000L,
                ),
            )
        }
        retryPendingEvents()
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

private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

private fun LocationTrackingEvent.kind(): TrackingEventKind = when (this) {
    is LocationTrackingEvent.Started -> TrackingEventKind.START
    is LocationTrackingEvent.LocationUpdated -> TrackingEventKind.LOCATION
    is LocationTrackingEvent.Stopped -> TrackingEventKind.STOP
}

private fun List<TrackedLocationDebugEntry>.updateStatus(
    event: LocationTrackingEvent,
    status: LocationSyncStatus,
): List<TrackedLocationDebugEntry> {
    val location = event.location ?: return this
    val kind = event.kind()
    val index =
        indexOfLast { it.location.timestampMs == location.timestampMs && it.eventKind == kind }
    return if (index < 0) this else toMutableList().also {
        it[index] = it[index].copy(syncStatus = status)
    }
}

private fun List<TrackedLocationDebugEntry>.updateLatestLocationStatus(
    location: TrackedLocation,
    status: LocationSyncStatus,
): List<TrackedLocationDebugEntry> {
    val index = indexOfLast { it.location.timestampMs == location.timestampMs && it.eventKind == TrackingEventKind.LOCATION }
    return if (index < 0) this else toMutableList().also { it[index] = it[index].copy(syncStatus = status) }
}
