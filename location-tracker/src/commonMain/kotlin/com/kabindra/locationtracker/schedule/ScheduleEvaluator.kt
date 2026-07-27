package com.kabindra.locationtracker.schedule

import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackingMode

object ScheduleEvaluator {

    /**
     * Determines whether tracking should be active based on policy rules.
     *
     * @param policy current backend tracking policy
     * @param currentHour current local hour (0..23)
     * @param currentMinute current local minute (0..59)
     */
    fun shouldTrackLocation(
        policy: LocationTrackerPolicy,
        currentHour: Int,
        currentMinute: Int
    ): Boolean {
        // Master toggle check
        if (!policy.isTrackingEnabled) return false

        return when (policy.trackingMode) {
            TrackingMode.TIME_RANGE -> {
                val window = policy.scheduleWindow
                window == null || window.isWithinWindow(currentHour, currentMinute)
            }

            TrackingMode.CHECK_IN_OUT -> {
                policy.isCheckedIn
            }
        }
    }
}
