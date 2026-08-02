package com.kabindra.locationtracker

import com.kabindra.locationtracker.filter.DistanceFilter
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.ScheduleWindow
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingMode
import com.kabindra.locationtracker.schedule.ScheduleEvaluator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyComponentsTest {

    @Test
    fun distanceFilterAcceptsFirstFixAndRejectsMovementBelowThreshold() {
        val origin = locationAt(27.7172, 85.3240)
        val nearby = locationAt(27.7173, 85.3240)

        assertTrue(DistanceFilter.isSignificantMovement(null, origin, thresholdMeters = 50f))
        assertFalse(DistanceFilter.isSignificantMovement(origin, nearby, thresholdMeters = 50f))
    }

    @Test
    fun scheduleEvaluatorHonoursMasterToggleScheduleAndCheckIn() {
        val scheduledPolicy = LocationTrackerPolicy(
            scheduleWindow = ScheduleWindow(9, 0, 17, 0),
        )
        assertTrue(ScheduleEvaluator.shouldTrackLocation(scheduledPolicy, 9, 0))
        assertFalse(ScheduleEvaluator.shouldTrackLocation(scheduledPolicy, 18, 0))
        assertFalse(
            ScheduleEvaluator.shouldTrackLocation(
                scheduledPolicy.copy(isTrackingEnabled = false),
                10,
                0
            )
        )

        val attendancePolicy = scheduledPolicy.copy(
            trackingMode = TrackingMode.CHECK_IN_OUT,
            isCheckedIn = true,
        )
        assertTrue(ScheduleEvaluator.shouldTrackLocation(attendancePolicy, 2, 0))
        assertFalse(
            ScheduleEvaluator.shouldTrackLocation(
                attendancePolicy.copy(isCheckedIn = false),
                2,
                0
            )
        )
    }

    @Test
    fun scheduleTransitionDelayHandlesDaytimeAndOvernightWindows() {
        val daytime = ScheduleWindow(9, 0, 17, 0)
        assertEquals(
            30 * 60_000L,
            ScheduleEvaluator.millisUntilNextTransition(daytime, 8, 30),
        )
        assertEquals(
            8 * 60 * 60_000L,
            ScheduleEvaluator.millisUntilNextTransition(daytime, 9, 0),
        )
        // End-minute is inclusive; the next minute is the first inactive minute.
        assertEquals(
            60_000L,
            ScheduleEvaluator.millisUntilNextTransition(daytime, 17, 0),
        )

        val overnight = ScheduleWindow(22, 0, 6, 0)
        assertTrue(
            ScheduleEvaluator.shouldTrackLocation(
                LocationTrackerPolicy(scheduleWindow = overnight), 23, 0,
            )
        )
        assertEquals(
            7 * 60 * 60_000L,
            ScheduleEvaluator.millisUntilNextTransition(overnight, 23, 0),
        )
        assertEquals(
            15 * 60 * 60_000L,
            ScheduleEvaluator.millisUntilNextTransition(overnight, 7, 0),
        )
    }

    @Test
    fun distanceFilterUsesInclusiveThreshold() {
        val origin = locationAt(27.7172, 85.3240)
        val fiftyMetersNorth = locationAt(27.7172 + (50.0 / 111_195.0), 85.3240)
        val distance = DistanceFilter.calculateDistanceMeters(origin, fiftyMetersNorth)

        assertTrue(distance >= 49.9)
        assertTrue(DistanceFilter.isSignificantMovement(origin, fiftyMetersNorth, 49.9f))
        assertFalse(DistanceFilter.isSignificantMovement(origin, fiftyMetersNorth, 50.1f))
    }

    private fun locationAt(latitude: Double, longitude: Double) = TrackedLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5f,
        speedMetersPerSecond = null,
        bearingDegrees = null,
        altitudeMeters = null,
        timestampMs = 0L,
    )
}
