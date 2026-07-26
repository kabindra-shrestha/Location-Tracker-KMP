package com.kabindra.locationtracker

import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Continuous, background-capable location tracking.
 *
 * - Android: backed by a bound [android.app.Service] running as a foreground
 *   service with `foregroundServiceType="location"`, using FusedLocationProviderClient.
 * - iOS: backed by CLLocationManager with `allowsBackgroundLocationUpdates = true`.
 *
 * Location permission MUST already be granted before calling [start] — use
 * [com.kabindra.locationtracker.permission.LocationPermissionController] first.
 *
 * This interface intentionally does NOT expose one-shot "get current location"
 * — for that, use [com.kabindra.locationtracker.compass.CompassCurrentLocation],
 * which delegates to Compass's `Geolocator` directly.
 */
interface LocationTracker {

    /** Hot stream of location fixes. Starts emitting once [state] reaches [TrackingState.Running]. */
    val locations: Flow<TrackedLocation>

    /** Current lifecycle state of the tracker. */
    val state: StateFlow<TrackingState>

    /**
     * Starts (or restarts with a new config) continuous location tracking.
     * Safe to call again while already running — the platform implementation
     * will update the active request in place rather than stacking listeners.
     */
    fun start(config: TrackingConfig = TrackingConfig())

    /** Stops tracking and tears down the platform-specific resources (service/manager). */
    fun stop()
}

/**
 * Creates the platform-appropriate [LocationTracker].
 *
 * Android: requires [com.kabindra.locationtracker.LocationTrackerInit.initialize] to have
 * been called once with an application [android.content.Context] (e.g. in your
 * `Application.onCreate()`), since a `Context` can't be threaded through this
 * no-arg factory directly.
 */
expect fun createLocationTracker(): LocationTracker
