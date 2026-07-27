package com.kabindra.locationtracker.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kabindra.locationtracker.model.LocationPriority
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import com.kabindra.locationtracker.notification.LocationNotificationFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the FusedLocationProviderClient subscription.
 *
 * This is a bound + started service: "started" so it survives independent of any
 * bound client (and can be launched from the background via
 * [android.content.Context.startForegroundService]), "bound" so [com.kabindra.locationtracker.LocationTracker]
 * can observe its flows directly without going through a BroadcastReceiver or similar.
 */
class LocationForegroundService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedClient: FusedLocationProviderClient
    private var activeCallback: LocationCallback? = null
    private var activeConfig = TrackingConfig()
    private var trackedPointCount = 0L

    private val _locations = MutableSharedFlow<TrackedLocation>(extraBufferCapacity = 64)
    val locations: SharedFlow<TrackedLocation> = _locations.asSharedFlow()

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = LocationServiceIntents.readConfig(intent)
        activeConfig = config
        startForeground(
            LocationNotificationFactory.NOTIFICATION_ID,
            LocationNotificationFactory.build(this, config)
        )
        restartLocationUpdates(config)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        removeActiveCallback()
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Called both from [onStartCommand] and from a live bound client requesting a config change. */
    @SuppressLint("MissingPermission") // Caller is required to have checked permission before start()
    fun restartLocationUpdates(config: TrackingConfig) {
        removeActiveCallback()
        _state.value = TrackingState.Starting

        val request = LocationRequest.Builder(config.priority.toGmsPriority(), config.intervalMs)
            .setMinUpdateDistanceMeters(config.minUpdateDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trackedPointCount += 1
                updateNotification()
                serviceScope.launch {
                    _locations.emit(
                        TrackedLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() },
                            bearingDegrees = location.bearing.takeIf { location.hasBearing() },
                            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
                            timestampMs = location.time,
                        ),
                    )
                }
            }

            override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    _state.value = TrackingState.LocationServicesDisabled
                }
            }
        }

        activeCallback = callback
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        _state.value = TrackingState.Running
    }

    fun stopTracking() {
        removeActiveCallback()
        _state.value = TrackingState.Stopped
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(
            LocationNotificationFactory.NOTIFICATION_ID,
            LocationNotificationFactory.build(this, activeConfig, trackedPointCount),
        )
    }

    private fun removeActiveCallback() {
        activeCallback?.let { fusedClient.removeLocationUpdates(it) }
        activeCallback = null
    }

    private fun LocationPriority.toGmsPriority(): Int = when (this) {
        LocationPriority.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
        LocationPriority.BALANCED -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationPriority.LOW_POWER -> Priority.PRIORITY_LOW_POWER
    }
}
