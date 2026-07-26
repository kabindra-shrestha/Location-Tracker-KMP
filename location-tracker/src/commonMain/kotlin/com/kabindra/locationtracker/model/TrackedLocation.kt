package com.kabindra.locationtracker.model

/**
 * A single location fix, normalized across Android's FusedLocationProviderClient
 * and iOS's CLLocationManager into one common shape.
 */
data class TrackedLocation(
    val latitude: Double,
    val longitude: Double,
    /** Estimated horizontal accuracy in meters. Smaller is better. */
    val accuracyMeters: Float,
    /** Speed in meters/second, if the platform reported a valid value. */
    val speedMetersPerSecond: Float?,
    /** Bearing in degrees (0-360), if available. */
    val bearingDegrees: Float?,
    /** Altitude in meters, if available. */
    val altitudeMeters: Double?,
    /** Epoch milliseconds when the fix was recorded. */
    val timestampMs: Long,
)
