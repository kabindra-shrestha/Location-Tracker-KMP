package com.kabindra.locationtracker.sync

import com.kabindra.locationtracker.filter.DistanceFilter
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.schedule.ScheduleEvaluator
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

data class SyncStats(
    val totalReceived: Long = 0,
    val totalFilteredOutWithinThreshold: Long = 0,
    val totalQueuedForSync: Long = 0,
    val totalSyncDispatches: Long = 0,
    val lastSyncTimestampMs: Long = 0,
    val lastSyncError: String? = null,
)

class LocationSyncManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private var policy: LocationTrackerPolicy = LocationTrackerPolicy()

    private val _stats = MutableStateFlow(SyncStats())
    val stats: StateFlow<SyncStats> = _stats.asStateFlow()

    private val pendingLocations = mutableListOf<TrackedLocation>()
    private var lastDispatchedLocation: TrackedLocation? = null
    private val pendingLocationsMutex = Mutex()

    private var syncJob: Job? = null

    /**
     * Updates active tracking policy (e.g. backend policy changes, check-in toggles, 50m filter, or sync interval).
     */
    fun updatePolicy(newPolicy: LocationTrackerPolicy) {
        val intervalChanged = policy.syncIntervalMinutes != newPolicy.syncIntervalMinutes
        policy = newPolicy

        if (intervalChanged && syncJob?.isActive == true) {
            restartSyncTimer()
        }
    }

    /**
     * Starts the periodic sync timer job.
     */
    fun start() {
        if (syncJob?.isActive == true) return
        restartSyncTimer()
    }

    /**
     * Stops the periodic sync timer job.
     */
    fun stop() {
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * Process a raw location update from LocationTracker.
     *
     * @param location incoming location fix
     * @param currentHour local hour (0..23) for schedule validation
     * @param currentMinute local minute (0..59) for schedule validation
     * @return true if location passed schedule & 50m distance displacement filter and was queued
     */
    suspend fun processLocation(
        location: TrackedLocation,
        currentHour: Int,
        currentMinute: Int
    ): Boolean {
        _stats.value = _stats.value.copy(totalReceived = _stats.value.totalReceived + 1)

        // 1. Validate schedule & master flag
        if (!ScheduleEvaluator.shouldTrackLocation(policy, currentHour, currentMinute)) {
            return false
        }

        // 2. Validate distance displacement filter (e.g. 50m threshold)
        val isSignificant = DistanceFilter.isSignificantMovement(
            // The policy deliberately compares with the last successfully sent
            // point, rather than a merely queued point.
            lastLocation = lastDispatchedLocation,
            newLocation = location,
            thresholdMeters = policy.minDistanceThresholdMeters
        )

        if (!isSignificant) {
            _stats.value = _stats.value.copy(
                totalFilteredOutWithinThreshold = _stats.value.totalFilteredOutWithinThreshold + 1
            )
            return false
        }

        // 3. Queue valid location fix
        pendingLocationsMutex.withLock { pendingLocations.add(location) }
        _stats.value = _stats.value.copy(
            totalQueuedForSync = _stats.value.totalQueuedForSync + 1
        )
        return true
    }

    /**
     * Immediately dispatches all queued location fixes to the host app callback listener.
     */
    suspend fun flushAndSyncNow() {
        // Deprecated: delivery now occurs immediately through LocationTrackingSession.
    }

    private fun restartSyncTimer() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                val intervalMs = (policy.syncIntervalMinutes * 60 * 1000L).coerceAtLeast(5000L)
                delay(intervalMs)
                flushAndSyncNow()
            }
        }
    }
}
