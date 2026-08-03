package com.kabindra.locationtrackerkmp

import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.ScheduleWindow
import com.kabindra.locationtracker.session.CheckInOutAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Debug-only stand-in for the host application's backend policy and attendance APIs.
 *
 * The sample's controls modify this object as if they were responses from a backend. Production
 * applications must replace it with their own authenticated API calls and complete policy response.
 */
object DemoTrackingBackend {
    private val _policy = MutableStateFlow(defaultPolicy())
    val policy: StateFlow<LocationTrackerPolicy> = _policy.asStateFlow()

    fun restorePersistedPolicy(persistedPolicy: LocationTrackerPolicy?) {
        if (persistedPolicy != null) _policy.value = persistedPolicy
    }

    fun updatePolicy(transform: (LocationTrackerPolicy) -> LocationTrackerPolicy) {
        _policy.value = transform(_policy.value)
    }

    /** Simulates the updated, authoritative policy returned by a Check-In/Out backend API. */
    fun performCheckInOut(action: CheckInOutAction): LocationTrackerPolicy {
        return _policy.value.copy(
            isCheckedIn = action == CheckInOutAction.CHECK_IN,
        ).also { _policy.value = it }
    }

    private fun defaultPolicy() = LocationTrackerPolicy(
        isTrackingEnabled = true,
        trackingMode = com.kabindra.locationtracker.model.TrackingMode.TIME_RANGE,
        // The demo remains eligible for most of the day so automatic-mode behavior is easy to test.
        scheduleWindow = ScheduleWindow(0, 0, 23, 59),
        isCheckedIn = false,
        minDistanceThresholdMeters = 50f,
        syncIntervalMinutes = 5,
    )
}
