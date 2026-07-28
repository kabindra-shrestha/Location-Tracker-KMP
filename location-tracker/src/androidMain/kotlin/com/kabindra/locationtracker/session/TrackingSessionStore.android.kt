package com.kabindra.locationtracker.session

import android.content.Context
import com.kabindra.locationtracker.internal.AndroidLocationTrackerContext
import com.kabindra.locationtracker.model.TrackedLocation

actual object TrackingSessionStore {
    private const val PREFS = "location_tracking_session"
    private const val ACTIVE = "active"
    private const val STARTED = "started"
    private const val LAST_KNOWN = "last_known"
    private const val LAST_SENT = "last_sent"
    private const val PENDING = "pending"
    private const val ERROR = "error"

    private fun preferences() = AndroidLocationTrackerContext.require()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun read(): TrackingSessionState = preferences().run {
        TrackingSessionState(
            isActive = getBoolean(ACTIVE, false),
            hasDeliveredStart = getBoolean(STARTED, false),
            lastKnownLocation = getString(LAST_KNOWN, null)?.toLocation(),
            lastSuccessfullyDeliveredLocation = getString(LAST_SENT, null)?.toLocation(),
            pendingEvents = getString(PENDING, "").orEmpty().lineSequence()
                .filter { it.isNotBlank() }.mapNotNull { it.toEvent() }.toList(),
            lastError = getString(ERROR, null),
        )
    }

    actual fun write(state: TrackingSessionState) {
        preferences().edit()
            .putBoolean(ACTIVE, state.isActive)
            .putBoolean(STARTED, state.hasDeliveredStart)
            .putString(LAST_KNOWN, state.lastKnownLocation?.encode())
            .putString(LAST_SENT, state.lastSuccessfullyDeliveredLocation?.encode())
            .putString(PENDING, state.pendingEvents.joinToString("\n") { it.encode() })
            .putString(ERROR, state.lastError)
            .apply()
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
