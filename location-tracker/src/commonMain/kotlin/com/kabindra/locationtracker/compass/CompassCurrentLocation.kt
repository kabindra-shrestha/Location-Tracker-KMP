package com.kabindra.locationtracker.compass

import com.kabindra.locationtracker.model.TrackedLocation
import dev.jordond.compass.Location
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.mobile.mobile

/**
 * Thin bridge to Compass for the things it's genuinely good at: a single
 * "get current location" call with built-in permission prompting, and GPS
 * enablement checks. This library does NOT use Compass for continuous
 * background tracking — see the module README for why (jordond/compass#250,
 * jordond/compass#90 — background updates aren't supported by Compass yet).
 *
 * NOTE: Compass's public API has moved around across major versions. If this
 * doesn't compile against the version pinned in `libs.versions.toml`, check
 * https://compass.jordond.dev/geolocation/overview for the current signature
 * of `Geolocator.mobile()` / `.current()` — the shape below matches the 3.x line.
 */
object CompassCurrentLocation {

    private val geolocator: Geolocator by lazy { Geolocator.mobile() }

    /**
     * Fetches a single current location fix. Prompts for foreground permission
     * if not already granted (this is Compass's built-in behavior).
     *
     * Returns null if the user denies permission, location services are off,
     * or the platform otherwise fails to produce a fix.
     */
    suspend fun getCurrentLocation(): TrackedLocation? {
        return when (val result = geolocator.current()) {
            is GeolocatorResult.Success -> result.data.toTrackedLocation()
            is GeolocatorResult.NotSupported,
            is GeolocatorResult.NotFound,
            is GeolocatorResult.PermissionError,
            is GeolocatorResult.GeolocationFailed -> null
        }
    }

    private fun Location.toTrackedLocation(): TrackedLocation = TrackedLocation(
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        accuracyMeters = (coordinates.accuracy ?: 0.0).toFloat(),
        speedMetersPerSecond = coordinates.speed?.toFloat(),
        bearingDegrees = coordinates.heading?.toFloat(),
        altitudeMeters = coordinates.altitude,
        timestampMs = timestampMillis ?: 0L,
    )
}
