package com.kabindra.locationtracker.model

enum class TrackingMode {
    /** Track location during specified daily time windows (e.g. 09:00 - 17:00). */
    TIME_RANGE,

    /** Track location only while the user is actively checked-in. */
    CHECK_IN_OUT
}

data class ScheduleWindow(
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 20,
    val endMinute: Int = 0
) {
    init {
        require(startHour in 0..23) { "startHour must be between 0 and 23" }
        require(endHour in 0..23) { "endHour must be between 0 and 23" }
        require(startMinute in 0..59) { "startMinute must be between 0 and 59" }
        require(endMinute in 0..59) { "endMinute must be between 0 and 59" }
    }

    /**
     * Validates whether a local time is in this daily window. The start is inclusive and the end
     * is exclusive: a 09:00–17:00 policy is active at 09:00 and inactive at 17:00. A window that
     * crosses midnight (for example 22:00–06:00) is supported; equal endpoints are zero length.
     */
    fun isWithinWindow(currentHour: Int, currentMinute: Int): Boolean {
        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (endMinutes > startMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight shift (for example 22:00–06:00). Equal endpoints are zero length.
            endMinutes != startMinutes &&
                (currentMinutes >= startMinutes || currentMinutes < endMinutes)
        }
    }
}

/**
 * Enterprise policy rules for location tracking. Provided with default values and
 * configurable dynamically from backend responses.
 */
data class LocationTrackerPolicy(
    /** Master flag from backend. If false, location tracking is suspended. */
    val isTrackingEnabled: Boolean = true,

    /** Determines whether tracking follows a schedule or check-in/check-out events. */
    val trackingMode: TrackingMode = TrackingMode.TIME_RANGE,

    /** Daily time window during which location tracking is active (for TIME_RANGE mode). */
    val scheduleWindow: ScheduleWindow? = ScheduleWindow(
        startHour = 0,
        startMinute = 0,
        endHour = 23,
        endMinute = 59
    ),

    /** Current check-in state (for CHECK_IN_OUT mode). */
    val isCheckedIn: Boolean = false,

    /** Distance displacement threshold in meters (default 50m). Locations within this distance are filtered out. */
    val minDistanceThresholdMeters: Float = 50.0f,

    /** Interval between displacement checks and ongoing location uploads. Start/stop events are immediate. */
    val syncIntervalMinutes: Int = 5,

    /** Latitude of the authorized tracking center (e.g. office or worksite). */
    val officeLatitude: Double? = 27.71799075127108,

    /** Longitude of the authorized tracking center. */
    val officeLongitude: Double? = 85.2985656622389,

    /** Allowed radius in meters around the office location for geofence checks. */
    val geofenceRadiusMeters: Float = 50.0f,
) {
    init {
        require(minDistanceThresholdMeters >= 0f) {
            "minDistanceThresholdMeters must be non-negative"
        }
        require(syncIntervalMinutes > 0) { "syncIntervalMinutes must be greater than zero" }
        require(geofenceRadiusMeters >= 0f) { "geofenceRadiusMeters must be non-negative" }
    }
}
