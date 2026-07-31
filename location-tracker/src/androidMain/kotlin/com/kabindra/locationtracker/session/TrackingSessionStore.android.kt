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
    private const val LAST_SYNC = "last_sync"
    private const val NEXT_SYNC = "next_sync"
    private const val PENDING = "pending"
    private const val TRACKED_LOCATIONS = "tracked_locations"
    private const val ERROR = "error"

    private fun preferences() = AndroidLocationTrackerContext.require()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun read(): TrackingSessionState = preferences().run {
        TrackingSessionState(
            isActive = getBoolean(ACTIVE, false),
            hasDeliveredStart = getBoolean(STARTED, false),
            lastKnownLocation = getString(LAST_KNOWN, null)?.toLocation(),
            lastSuccessfullyDeliveredLocation = getString(LAST_SENT, null)?.toLocation(),
            lastSyncTimestampMs = getLong(LAST_SYNC, 0L),
            nextSyncTimestampMs = getLong(NEXT_SYNC, 0L),
            pendingEvents = getString(PENDING, "").orEmpty().lineSequence()
                .filter { it.isNotBlank() }.mapNotNull { it.toEvent() }.toList(),
            trackedLocations = getString(TRACKED_LOCATIONS, "").orEmpty().lineSequence()
                .filter { it.isNotBlank() }.mapNotNull { it.toDebugEntry() }.toList(),
            lastError = getString(ERROR, null),
        )
    }

    actual fun write(state: TrackingSessionState) {
        preferences().edit()
            .putBoolean(ACTIVE, state.isActive)
            .putBoolean(STARTED, state.hasDeliveredStart)
            .putString(LAST_KNOWN, state.lastKnownLocation?.encode())
            .putString(LAST_SENT, state.lastSuccessfullyDeliveredLocation?.encode())
            .putLong(LAST_SYNC, state.lastSyncTimestampMs)
            .putLong(NEXT_SYNC, state.nextSyncTimestampMs)
            .putString(PENDING, state.pendingEvents.joinToString("\n") { it.encode() })
            .putString(TRACKED_LOCATIONS, state.trackedLocations.joinToString("\n") { it.encode() })
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

private fun TrackedLocationDebugEntry.encode(): String =
    "${eventKind.name}|${syncStatus.name}|${location.encode()}"

private fun String.toDebugEntry(): TrackedLocationDebugEntry? = runCatching {
    val p = split("|", limit = 3)
    TrackedLocationDebugEntry(
        location = requireNotNull(p.getOrNull(2)?.toLocation()),
        eventKind = TrackingEventKind.valueOf(p[0]),
        syncStatus = LocationSyncStatus.valueOf(p[1]),
    )
}.getOrNull()
