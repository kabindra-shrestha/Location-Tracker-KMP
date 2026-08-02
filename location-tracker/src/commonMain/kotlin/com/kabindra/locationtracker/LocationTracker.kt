package com.kabindra.locationtracker

import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Presentation-facing view of continuous, background-capable location tracking.
 *
 * - Android: backed by a bound [android.app.Service] running as a foreground
 *   service with `foregroundServiceType="location"`, using FusedLocationProviderClient.
 * - iOS: backed by CLLocationManager with `allowsBackgroundLocationUpdates = true`.
 *
 * Prefer [LocationTrackingEngine] for application initialization, backend-policy updates, and
 * persistent start/stop commands. This interface only exposes live fixes and platform state to a
 * screen. Location permission MUST already be granted before calling [start], and a complete
 * backend policy must already have been applied through [LocationTrackingEngine.updatePolicy].
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
     * Starts continuous tracking using the persisted policy already applied to
     * [LocationTrackingEngine]. A policy-ineligible or already-active session is not restarted.
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
