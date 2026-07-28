package com.kabindra.locationtracker.model

enum class LocationPriority {
    /** Highest accuracy, highest battery cost. Use for ride-sharing driver tracking. */
    HIGH_ACCURACY,

    /** Balanced accuracy/battery. Good default for employee tracking with longer intervals. */
    BALANCED,

    /** Coarse accuracy, lowest battery cost. */
    LOW_POWER,
}

/**
 * Configuration for a tracking session. Passed to [com.kabindra.locationtracker.LocationTracker.start].
 *
 * @param intervalMs desired interval between platform location updates. It does not by itself
 *   cause a backend upload; the session's 50m displacement rule remains the upload gate.
 * @param minUpdateDistanceMeters minimum displacement before a platform location update is emitted.
 * @param priority desired accuracy/power tradeoff.
 * @param notificationTitle Android-only: title of the persistent foreground service notification.
 * @param notificationText Android-only: body text of the persistent foreground service notification.
 * @param notificationSmallIconResId Android-only: drawable resource id for the notification icon.
 *   Defaults to 0, in which case the host app MUST supply one via [com.kabindra.locationtracker.LocationTrackerConfig]
 *   at initialization time (see README) or the service will fail to start on Android 8+.
 */
data class TrackingConfig(
    val intervalMs: Long = 10_000L,
    val minUpdateDistanceMeters: Float = 20f,
    val priority: LocationPriority = LocationPriority.HIGH_ACCURACY,
    val notificationTitle: String = "Tracking location",
    val notificationText: String = "Your location is being shared while this is active",
    val notificationSmallIconResId: Int = 0,
)
