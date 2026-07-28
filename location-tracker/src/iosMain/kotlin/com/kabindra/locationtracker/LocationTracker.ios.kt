package com.kabindra.locationtracker

import com.kabindra.locationtracker.model.LocationPriority
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import com.kabindra.locationtracker.session.LocationTrackingSession
import com.kabindra.locationtracker.session.TrackingStopReason
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationAccuracy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/**
 * CLLocationManager-backed tracker. The three lines in [start] marked below are
 * what actually make background delivery work — omitting any one of them will
 * cause iOS to silently pause updates once the app is suspended.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosLocationTracker : LocationTracker {

    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val manager = CLLocationManager()

    private val _locations = MutableSharedFlow<TrackedLocation>(extraBufferCapacity = 64)
    override val locations: Flow<TrackedLocation> = _locations.asSharedFlow()

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    override val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            if (!LocationTrackingSession.isTrackingAllowed()) {
                stopForPolicy()
                return
            }
            val trackedLocation = location.toTrackedLocation()
            LocationTrackingSession.onLocation(trackedLocation)
            trackerScope.launch { _locations.emit(trackedLocation) }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            _state.value = TrackingState.Error(didFailWithError.localizedDescription)
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways) return
            // Foreground-only auth can't sustain background delivery; surface it so the
            // host app can prompt the user rather than silently losing background fixes.
        }
    }

    override fun start(config: TrackingConfig) {
        _state.value = TrackingState.Starting

        manager.delegate = delegate
        manager.desiredAccuracy = config.priority.toClAccuracy()
        manager.distanceFilter = config.minUpdateDistanceMeters.toDouble()

        // --- The three settings that make background tracking actually work ---
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.showsBackgroundLocationIndicator = true
        // ------------------------------------------------------------------------

        manager.startUpdatingLocation()
        LocationTrackingSession.markStarted()
        _state.value = TrackingState.Running
    }

    override fun stop() {
        LocationTrackingSession.stop(TrackingStopReason.USER)
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
        _state.value = TrackingState.Stopped
    }

    private fun stopForPolicy() {
        LocationTrackingSession.stop(TrackingStopReason.POLICY)
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
        _state.value = TrackingState.Stopped
    }

    private fun LocationPriority.toClAccuracy(): CLLocationAccuracy = when (this) {
        LocationPriority.HIGH_ACCURACY -> kCLLocationAccuracyBest
        LocationPriority.BALANCED -> kCLLocationAccuracyNearestTenMeters
        LocationPriority.LOW_POWER -> kCLLocationAccuracyHundredMeters
    }

    private fun CLLocation.toTrackedLocation(): TrackedLocation {
        val (lat, lon) = coordinate.useContents { latitude to longitude }
        return TrackedLocation(
            latitude = lat,
            longitude = lon,
            accuracyMeters = horizontalAccuracy.toFloat(),
            speedMetersPerSecond = speed.toFloat().takeIf { it >= 0f },
            bearingDegrees = course.toFloat().takeIf { it >= 0f },
            altitudeMeters = altitude,
            timestampMs = (timestamp.timeIntervalSince1970 * 1000).toLong(),
        )
    }
}

actual fun createLocationTracker(): LocationTracker = IosLocationTracker()
