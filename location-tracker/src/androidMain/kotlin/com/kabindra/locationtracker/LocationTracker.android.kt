package com.kabindra.locationtracker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kabindra.locationtracker.internal.AndroidLocationTrackerContext
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import com.kabindra.locationtracker.service.LocationForegroundService
import com.kabindra.locationtracker.service.LocationServiceIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Binds to [LocationForegroundService] and republishes its flows through the
 * common [LocationTracker] contract. Kept intentionally "dumb" — all the actual
 * location-request logic lives in the service so it survives this class being
 * recreated (e.g. across process/activity lifecycle), not the other way around.
 */
internal class AndroidLocationTracker(private val appContext: Context) : LocationTracker {

    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _locations = MutableSharedFlow<TrackedLocation>(extraBufferCapacity = 64)
    override val locations: Flow<TrackedLocation> = _locations.asSharedFlow()

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    override val state: StateFlow<TrackingState> = _state.asStateFlow()

    private var boundService: LocationForegroundService? = null
    private var collectJob: Job? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as LocationForegroundService.LocalBinder).getService()
            boundService = service
            collectJob = trackerScope.launch {
                launch { service.locations.collect { _locations.emit(it) } }
                launch { service.state.collect { _state.value = it } }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            collectJob?.cancel()
        }
    }

    override fun start(config: TrackingConfig) {
        val intent = LocationServiceIntents.putConfig(
            Intent(appContext, LocationForegroundService::class.java),
            config,
        )

        // startForegroundService (not startService) is required on Android 8+ so the
        // service has a grace period to call startForeground() before the OS kills it.
        ContextCompat.startForegroundService(appContext, intent)

        if (!isBound) {
            appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            isBound = true
        } else {
            // Already bound and running — just push the new config to the live service.
            boundService?.restartLocationUpdates(config)
        }
    }

    override fun stop() {
        boundService?.stopTracking()
        if (isBound) {
            runCatching { appContext.unbindService(connection) }
            isBound = false
        }
        boundService = null
        collectJob?.cancel()
        _state.value = TrackingState.Stopped
    }
}

actual fun createLocationTracker(): LocationTracker =
    AndroidLocationTracker(AndroidLocationTrackerContext.require())
