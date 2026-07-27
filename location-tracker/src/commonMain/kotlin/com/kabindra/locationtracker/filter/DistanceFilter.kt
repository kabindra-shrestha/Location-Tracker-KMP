package com.kabindra.locationtracker.filter

import com.kabindra.locationtracker.model.TrackedLocation
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceFilter {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two locations in meters using the Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1.toRadians()) * cos(lat2.toRadians()) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_METERS * c
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    /**
     * Calculates distance between two [TrackedLocation] points in meters.
     */
    fun calculateDistanceMeters(loc1: TrackedLocation, loc2: TrackedLocation): Double {
        return calculateDistanceMeters(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude)
    }

    /**
     * Returns true if [newLocation] is displaced by at least [thresholdMeters] from [lastLocation].
     * If [lastLocation] is null, returns true (first location fix).
     */
    fun isSignificantMovement(
        lastLocation: TrackedLocation?,
        newLocation: TrackedLocation,
        thresholdMeters: Float
    ): Boolean {
        if (lastLocation == null) return true
        val distance = calculateDistanceMeters(lastLocation, newLocation)
        return distance >= thresholdMeters
    }
}
