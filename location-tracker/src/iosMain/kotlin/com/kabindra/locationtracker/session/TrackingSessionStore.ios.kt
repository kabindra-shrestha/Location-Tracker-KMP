package com.kabindra.locationtracker.session

import com.kabindra.locationtracker.model.TrackedLocation
import platform.Foundation.NSUserDefaults

actual object TrackingSessionStore {
    private const val PREFIX = "location_tracking_session."
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun read(): TrackingSessionState = TrackingSessionState(
        isActive = defaults.boolForKey(PREFIX + "active"),
        hasDeliveredStart = defaults.boolForKey(PREFIX + "started"),
        lastKnownLocation = defaults.stringForKey(PREFIX + "last_known")?.toLocation(),
        lastSuccessfullyDeliveredLocation = defaults.stringForKey(PREFIX + "last_sent")
            ?.toLocation(),
        pendingEvents = defaults.stringForKey(PREFIX + "pending").orEmpty().lineSequence()
            .filter { it.isNotBlank() }.mapNotNull { it.toEvent() }.toList(),
        lastError = defaults.stringForKey(PREFIX + "error"),
    )

    actual fun write(state: TrackingSessionState) {
        defaults.setBool(state.isActive, PREFIX + "active")
        defaults.setBool(state.hasDeliveredStart, PREFIX + "started")
        defaults.setObject(state.lastKnownLocation?.encode(), PREFIX + "last_known")
        defaults.setObject(state.lastSuccessfullyDeliveredLocation?.encode(), PREFIX + "last_sent")
        defaults.setObject(
            state.pendingEvents.joinToString("\n") { it.encode() },
            PREFIX + "pending"
        )
        defaults.setObject(state.lastError, PREFIX + "error")
    }
}

private fun TrackedLocation.encode(): String = listOf(
    latitude, longitude, accuracyMeters, speedMetersPerSecond ?: "", bearingDegrees ?: "",
    altitudeMeters ?: "", timestampMs,
).joinToString(",")

private fun String.toLocation(): TrackedLocation? = runCatching {
    val p = split(",")
    TrackedLocation(
        p[0].toDouble(), p[1].toDouble(), p[2].toFloat(), p[3].toFloatOrNull(),
        p[4].toFloatOrNull(), p[5].toDoubleOrNull(), p[6].toLong()
    )
}.getOrNull()

private fun LocationTrackingEvent.encode(): String = when (this) {
    is LocationTrackingEvent.Started -> "START|${location.encode()}"
    is LocationTrackingEvent.LocationUpdated -> "UPDATE|${location.encode()}"
    is LocationTrackingEvent.Stopped -> "STOP|${reason.name}|${location?.encode().orEmpty()}"
}

private fun String.toEvent(): LocationTrackingEvent? = runCatching {
    val p = split("|", limit = 3)
    when (p[0]) {
        "START" -> LocationTrackingEvent.Started(requireNotNull(p.getOrNull(1)?.toLocation()))
        "UPDATE" -> LocationTrackingEvent.LocationUpdated(
            requireNotNull(
                p.getOrNull(1)?.toLocation()
            )
        )

        "STOP" -> LocationTrackingEvent.Stopped(
            p.getOrNull(2)?.toLocation(),
            TrackingStopReason.valueOf(p[1])
        )

        else -> null
    }
}.getOrNull()
