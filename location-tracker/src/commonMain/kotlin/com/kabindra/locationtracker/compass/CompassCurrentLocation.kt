package com.kabindra.locationtracker.compass

import com.kabindra.locationtracker.model.TrackedLocation
import dev.jordond.compass.Location
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.Locator
import dev.jordond.compass.geolocation.mobile.mobile

/**
 * Thin bridge to Compass for the things it's genuinely good at: a single
 * "get current location" call with built-in permission prompting, and GPS
 * enablement checks. This library does NOT use Compass for continuous
 * background tracking — see the module README for why (jordond/compass#250,
 * jordond/compass#90 — background updates aren't supported by Compass yet).
 */
object CompassCurrentLocation {

    private val geolocator: Geolocator by lazy { Geolocator(Locator.mobile()) }

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
            else -> null
        }
    }

    private fun Location.toTrackedLocation(): TrackedLocation = TrackedLocation(
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        accuracyMeters = accuracy.toFloat(),
        speedMetersPerSecond = speed?.mps,
        bearingDegrees = azimuth?.degrees,
        altitudeMeters = mslAltitude?.meters ?: ellipsoidalAltitude?.meters,
        timestampMs = timestampMillis,
    )
}

