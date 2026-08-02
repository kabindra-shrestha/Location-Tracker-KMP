package com.kabindra.locationtracker.session

import android.content.Context
import com.kabindra.locationtracker.internal.AndroidLocationTrackerContext
import com.kabindra.locationtracker.model.TrackedLocation

actual object TrackingSessionStore {
    private const val PREFS = "location_tracking_session"
    private const val ACTIVE = "active"
    private const val SESSION_ID = "session_id"
    private const val STARTED = "started"
    private const val LAST_KNOWN = "last_known"
    private const val LAST_SENT = "last_sent"
    private const val LAST_SYNC = "last_sync"
    private const val NEXT_SYNC = "next_sync"
    private const val PENDING = "pending"
    private const val TRACKED_LOCATIONS = "tracked_locations"
    private const val ERROR = "error"
    private const val CONFIG = "config"
    private const val POLICY = "policy"

    private fun preferences() = AndroidLocationTrackerContext.require()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun read(): TrackingSessionState = preferences().run {
        TrackingSessionState(
            isActive = getBoolean(ACTIVE, false),
            sessionId = getString(SESSION_ID, null),
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
            activeConfig = getString(CONFIG, null)?.toConfig(),
            activePolicy = getString(POLICY, null)?.toPolicy(),
        )
    }

    actual fun write(state: TrackingSessionState) {
        preferences().edit()
            .putBoolean(ACTIVE, state.isActive)
            .putString(SESSION_ID, state.sessionId)
            .putBoolean(STARTED, state.hasDeliveredStart)
            .putString(LAST_KNOWN, state.lastKnownLocation?.encode())
            .putString(LAST_SENT, state.lastSuccessfullyDeliveredLocation?.encode())
            .putLong(LAST_SYNC, state.lastSyncTimestampMs)
            .putLong(NEXT_SYNC, state.nextSyncTimestampMs)
            .putString(PENDING, state.pendingEvents.joinToString("\n") { it.encode() })
            .putString(TRACKED_LOCATIONS, state.trackedLocations.joinToString("\n") { it.encode() })
            .putString(ERROR, state.lastError)
            .putString(CONFIG, state.activeConfig?.encode())
            .putString(POLICY, state.activePolicy?.encode())
            .apply()
    }
}

private fun com.kabindra.locationtracker.model.LocationTrackerPolicy.encode(): String = listOf(
    isTrackingEnabled,
    trackingMode.name,
    scheduleWindow?.startHour ?: "",
    scheduleWindow?.startMinute ?: "",
    scheduleWindow?.endHour ?: "",
    scheduleWindow?.endMinute ?: "",
    isCheckedIn,
    minDistanceThresholdMeters,
    syncIntervalMinutes,
).joinToString("|")

private fun String.toPolicy(): com.kabindra.locationtracker.model.LocationTrackerPolicy? =
    runCatching {
        val p = split("|")
        val window = p[2].toIntOrNull()?.let {
            com.kabindra.locationtracker.model.ScheduleWindow(
                it,
                p[3].toInt(),
                p[4].toInt(),
                p[5].toInt()
            )
        }
        com.kabindra.locationtracker.model.LocationTrackerPolicy(
            isTrackingEnabled = p[0].toBoolean(),
            trackingMode = com.kabindra.locationtracker.model.TrackingMode.valueOf(p[1]),
            scheduleWindow = window,
            isCheckedIn = p[6].toBoolean(),
            minDistanceThresholdMeters = p[7].toFloat(),
            syncIntervalMinutes = p[8].toInt(),
        )
    }.getOrNull()

private fun com.kabindra.locationtracker.model.TrackingConfig.encode(): String = listOf(
    intervalMs, minUpdateDistanceMeters, priority.name, notificationTitle, notificationText,
    notificationSmallIconResId,
).joinToString("|")

private fun String.toConfig(): com.kabindra.locationtracker.model.TrackingConfig? = runCatching {
    val p = split("|")
    com.kabindra.locationtracker.model.TrackingConfig(
        p[0].toLong(),
        p[1].toFloat(),
        com.kabindra.locationtracker.model.LocationPriority.valueOf(p[2]),
        p[3],
        p[4],
        p[5].toInt()
    )
}.getOrNull()

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
    "${id}|${eventKind.name}|${syncStatus.name}|${location.encode()}"

private fun String.toDebugEntry(): TrackedLocationDebugEntry? = runCatching {
    val p = split("|", limit = 4)
    TrackedLocationDebugEntry(
        id = p[0],
        location = requireNotNull(p.getOrNull(3)?.toLocation()),
        eventKind = TrackingEventKind.valueOf(p[1]),
        syncStatus = LocationSyncStatus.valueOf(p[2]),
    )
}.getOrNull()
