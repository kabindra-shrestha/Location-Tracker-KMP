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
                // A backend TIME_RANGE policy without a window is malformed. Fail closed rather
                // than silently treating it as an all-day tracking authorization.
                window?.isWithinWindow(currentHour, currentMinute) == true
            }

            TrackingMode.CHECK_IN_OUT -> {
                policy.isCheckedIn
            }
        }
    }

    /** One deterministic rule for Start-button availability and engine-start eligibility. */
    fun canStart(
        policy: LocationTrackerPolicy,
        currentHour: Int,
        currentMinute: Int,
        isSessionActive: Boolean,
    ): Boolean = !isSessionActive && shouldTrackLocation(policy, currentHour, currentMinute)

    /**
     * Returns the delay to the next local TIME_RANGE state transition. The evaluation is minute
     * based because the backend policy itself contains hour/minute boundaries. Schedule windows
     * are start-inclusive/end-exclusive, so a 09:00–17:00 shift stops at 17:00.
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
            minutes = MINUTES_PER_DAY
        }
        return minutes * MILLIS_PER_MINUTE
    }

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MILLIS_PER_MINUTE = 60_000L
}
