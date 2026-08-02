package com.kabindra.locationtracker.session

import com.kabindra.locationtracker.model.TrackedLocation
import platform.Foundation.NSUserDefaults

actual object TrackingSessionStore {
    private const val PREFIX = "location_tracking_session."
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun read(): TrackingSessionState = TrackingSessionState(
        isActive = defaults.boolForKey(PREFIX + "active"),
        sessionId = defaults.stringForKey(PREFIX + "session_id"),
        hasDeliveredStart = defaults.boolForKey(PREFIX + "started"),
        lastKnownLocation = defaults.stringForKey(PREFIX + "last_known")?.toLocation(),
        lastSuccessfullyDeliveredLocation = defaults.stringForKey(PREFIX + "last_sent")
            ?.toLocation(),
        lastSyncTimestampMs = defaults.doubleForKey(PREFIX + "last_sync").toLong(),
        nextSyncTimestampMs = defaults.doubleForKey(PREFIX + "next_sync").toLong(),
        pendingEvents = defaults.stringForKey(PREFIX + "pending").orEmpty().lineSequence()
            .filter { it.isNotBlank() }.mapNotNull { it.toEvent() }.toList(),
        trackedLocations = defaults.stringForKey(PREFIX + "tracked_locations").orEmpty()
            .lineSequence().filter { it.isNotBlank() }.mapNotNull { it.toDebugEntry() }.toList(),
        lastError = defaults.stringForKey(PREFIX + "error"),
        activeConfig = defaults.stringForKey(PREFIX + "config")?.toConfig(),
        activePolicy = defaults.stringForKey(PREFIX + "policy")?.toPolicy(),
    )

    actual fun write(state: TrackingSessionState) {
        defaults.setBool(state.isActive, PREFIX + "active")
        defaults.setObject(state.sessionId, PREFIX + "session_id")
        defaults.setBool(state.hasDeliveredStart, PREFIX + "started")
        defaults.setObject(state.lastKnownLocation?.encode(), PREFIX + "last_known")
        defaults.setObject(state.lastSuccessfullyDeliveredLocation?.encode(), PREFIX + "last_sent")
        defaults.setDouble(state.lastSyncTimestampMs.toDouble(), PREFIX + "last_sync")
        defaults.setDouble(state.nextSyncTimestampMs.toDouble(), PREFIX + "next_sync")
        defaults.setObject(
            state.pendingEvents.joinToString("\n") { it.encode() },
            PREFIX + "pending"
        )
        defaults.setObject(
            state.trackedLocations.joinToString("\n") { it.encode() },
            PREFIX + "tracked_locations"
        )
        defaults.setObject(state.lastError, PREFIX + "error")
        defaults.setObject(state.activeConfig?.encode(), PREFIX + "config")
        defaults.setObject(state.activePolicy?.encode(), PREFIX + "policy")
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
