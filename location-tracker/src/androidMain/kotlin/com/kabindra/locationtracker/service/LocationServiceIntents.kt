package com.kabindra.locationtracker.service

import android.content.Intent
import com.kabindra.locationtracker.model.LocationPriority
import com.kabindra.locationtracker.model.TrackingConfig

/**
 * [TrackingConfig] isn't Parcelable to keep commonMain free of Android types,
 * so we pack/unpack its primitive fields onto plain Intent extras instead.
 */
internal object LocationServiceIntents {

    private const val EXTRA_INTERVAL_MS = "extra_interval_ms"
    private const val EXTRA_MIN_DISPLACEMENT_M = "extra_min_displacement_m"
    private const val EXTRA_PRIORITY = "extra_priority"
    private const val EXTRA_NOTIF_TITLE = "extra_notif_title"
    private const val EXTRA_NOTIF_TEXT = "extra_notif_text"
    private const val EXTRA_NOTIF_ICON = "extra_notif_icon"

    fun putConfig(intent: Intent, config: TrackingConfig): Intent = intent.apply {
        putExtra(EXTRA_INTERVAL_MS, config.intervalMs)
        putExtra(EXTRA_MIN_DISPLACEMENT_M, config.minUpdateDistanceMeters)
        putExtra(EXTRA_PRIORITY, config.priority.name)
        putExtra(EXTRA_NOTIF_TITLE, config.notificationTitle)
        putExtra(EXTRA_NOTIF_TEXT, config.notificationText)
        putExtra(EXTRA_NOTIF_ICON, config.notificationSmallIconResId)
    }

    fun readConfig(intent: Intent?): TrackingConfig {
        intent ?: return TrackingConfig()
        return TrackingConfig(
            intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, TrackingConfig().intervalMs),
            minUpdateDistanceMeters = intent.getFloatExtra(
                EXTRA_MIN_DISPLACEMENT_M,
                TrackingConfig().minUpdateDistanceMeters,
            ),
            priority = intent.getStringExtra(EXTRA_PRIORITY)
                ?.let { runCatching { LocationPriority.valueOf(it) }.getOrNull() }
                ?: LocationPriority.HIGH_ACCURACY,
            notificationTitle = intent.getStringExtra(EXTRA_NOTIF_TITLE)
                ?: TrackingConfig().notificationTitle,
            notificationText = intent.getStringExtra(EXTRA_NOTIF_TEXT)
                ?: TrackingConfig().notificationText,
            notificationSmallIconResId = intent.getIntExtra(EXTRA_NOTIF_ICON, 0),
        )
    }
}
