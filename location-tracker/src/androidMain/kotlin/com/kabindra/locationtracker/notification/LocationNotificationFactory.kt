package com.kabindra.locationtracker.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kabindra.locationtracker.model.TrackingConfig

internal object LocationNotificationFactory {

    const val CHANNEL_ID = "location_tracking_channel"
    const val NOTIFICATION_ID = 4200

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when your location is actively being tracked"
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, config: TrackingConfig, trackedPointCount: Long = 0): Notification {
        ensureChannel(context)

        // A host app MUST supply a valid drawable resource id via TrackingConfig,
        // since this library module has no app icon of its own to fall back on.
        val iconResId = config.notificationSmallIconResId.takeIf { it != 0 }
            ?: context.applicationInfo.icon

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(config.notificationTitle)
            .setContentText("${config.notificationText} • $trackedPointCount points tracked")
            .setSmallIcon(iconResId)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_LOCATION_SHARING)
            .build()
    }
}
