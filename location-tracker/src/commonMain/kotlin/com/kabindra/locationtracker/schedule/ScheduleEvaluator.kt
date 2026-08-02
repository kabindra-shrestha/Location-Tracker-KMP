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

    /**
     * Returns the delay to the next local TIME_RANGE state transition. The evaluation is minute
     * based because the backend policy itself contains hour/minute boundaries. At an inclusive end
     * minute we wait one minute before re-evaluating, so a 09:00–17:00 shift stops just after 17:00.
     */
    fun millisUntilNextTransition(
        window: com.kabindra.locationtracker.model.ScheduleWindow,
        currentHour: Int,
        currentMinute: Int,
    ): Long {
        val now = currentHour * 60 + currentMinute
        val start = window.startHour * 60 + window.startMinute
        val end = window.endHour * 60 + window.endMinute
        val active = window.isWithinWindow(currentHour, currentMinute)
        var minutes = if (active) (end - now + MINUTES_PER_DAY) % MINUTES_PER_DAY
        else (start - now + MINUTES_PER_DAY) % MINUTES_PER_DAY

        if (minutes == 0) {
            minutes = if (active && start != end) 1 else MINUTES_PER_DAY
        }
        return minutes * MILLIS_PER_MINUTE
    }

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MILLIS_PER_MINUTE = 60_000L
}
