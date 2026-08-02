package com.kabindra.locationtracker

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.kabindra.locationtracker.internal.AndroidLocationTrackerContext
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingMode
import com.kabindra.locationtracker.schedule.ScheduleEvaluator
import com.kabindra.locationtracker.schedule.currentLocalScheduleTime
import com.kabindra.locationtracker.service.LocationForegroundService
import com.kabindra.locationtracker.service.LocationServiceIntents
import com.kabindra.locationtracker.session.TrackingStopReason

/** Android's process-independent owner is the started foreground service, never an Activity/UI. */
internal actual object PlatformTrackingController {
    private const val TAG = "LocationTracker"

    actual fun canStart(): Boolean {
        val context = AndroidLocationTrackerContext.require()
        val foregroundGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return foregroundGranted && backgroundGranted
    }

    actual fun start(config: TrackingConfig): Boolean = runCatching {
        val context = AndroidLocationTrackerContext.require()
        ContextCompat.startForegroundService(
            context,
            LocationServiceIntents.putConfig(Intent(context, LocationForegroundService::class.java), config),
        )
        true
    }.onFailure {
        Log.e(TAG, "Unable to start foreground location service", it)
    }.getOrDefault(false)

    actual fun restore(config: TrackingConfig) {
        // Re-sending a start command is idempotent: the service removes its old callback before
        // requesting updates again, and the shared session preserves its original session id.
        start(config)
    }

    actual fun stop(reason: TrackingStopReason) {
        runCatching {
            val context = AndroidLocationTrackerContext.require()
            context.startService(
                LocationServiceIntents.stop(Intent(context, LocationForegroundService::class.java)),
            )
        }.onFailure {
            Log.w(TAG, "Unable to send explicit stop command to location service", it)
        }
    }

    actual fun schedule(policy: LocationTrackerPolicy?) {
        val context = AndroidLocationTrackerContext.require()
        val intent = Intent(context, TrackingScheduleReceiver::class.java).apply {
            action = ACTION_RECONCILE_SCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(pendingIntent)
        if (policy?.isTrackingEnabled != true || policy.trackingMode != TrackingMode.TIME_RANGE ||
            policy.scheduleWindow == null) return

        val now = currentLocalScheduleTime()
        val triggerAtMs = System.currentTimeMillis() + ScheduleEvaluator.millisUntilNextTransition(
            policy.scheduleWindow,
            now.hour,
            now.minute,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } else {
            alarms.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    private const val ACTION_RECONCILE_SCHEDULE =
        "com.kabindra.locationtracker.action.RECONCILE_SCHEDULE"
    private const val SCHEDULE_REQUEST_CODE = 29041
}

/** One persisted alarm is enough: each reconciliation schedules the following boundary. */
internal class TrackingScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.kabindra.locationtracker.action.RECONCILE_SCHEDULE") {
            LocationTrackingEngine.reconcileNow()
        }
    }
}
