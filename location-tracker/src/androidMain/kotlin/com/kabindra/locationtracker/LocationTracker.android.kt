package com.kabindra.locationtracker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.kabindra.locationtracker.internal.AndroidLocationTrackerContext
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import com.kabindra.locationtracker.service.LocationForegroundService
import com.kabindra.locationtracker.session.LocationTrackingSession
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

    // Service binding and ServiceConnection callbacks belong on the main thread. The actual
    // location collection remains in the foreground service, independent of this UI adapter.
    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

    init {
        // Policy reconciliation starts the foreground service directly, without calling this
        // presentation adapter. Observe the durable session so a tracker created before a
        // Check-In attaches once that service becomes active and receives its Running state.
        trackerScope.launch {
            var wasActive = false
            LocationTrackingSession.state.collect { session ->
                if (session.isActive) {
                    _state.value = TrackingState.Starting
                    bindToService()
                } else if (wasActive) {
                    unbindFromService()
                    _state.value = TrackingState.Stopped
                }
                wasActive = session.isActive
            }
        }
    }

    override fun start(config: TrackingConfig) {
        val policy = LocationTrackingSession.state.value.activePolicy
            ?: LocationTrackerPolicy(scheduleWindow = null)
        if (LocationTrackingEngine.start(policy, config)) bindToService()
    }

    override fun stop() {
        // A recreated UI is not necessarily bound to the started foreground service. The engine
        // always sends an explicit command, so Stop works in either case.
        LocationTrackingEngine.stop()
        unbindFromService()
        _state.value = TrackingState.Stopped
    }

    private fun bindToService() {
        if (isBound) return
        appContext.bindService(
            Intent(appContext, LocationForegroundService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        isBound = true
    }

    private fun unbindFromService() {
        if (isBound) {
            runCatching { appContext.unbindService(connection) }
            isBound = false
        }
        boundService = null
        collectJob?.cancel()
        collectJob = null
    }
}

actual fun createLocationTracker(): LocationTracker =
    AndroidLocationTracker(AndroidLocationTrackerContext.require())
