package com.kabindra.locationtracker.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.darwin.NSObject

/**
 * iOS requires the same two-step dance as Android: an app must first be granted
 * "When In Use", and only then may it request the "Always" upgrade via
 * `requestAlwaysAuthorization()` — asking for Always up front is rejected/ignored
 * by the system on iOS 13+.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosLocationPermissionController(
    private val manager: CLLocationManager,
) : LocationPermissionController {

    private var pendingAuthResult: CompletableDeferred<CLAuthorizationStatus>? = null

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            pendingAuthResult?.let { deferred ->
                if (!deferred.isCompleted) deferred.complete(manager.authorizationStatus)
            }
        }
    }

    init {
        manager.delegate = delegate
    }

    override suspend fun status(): LocationPermissionStatus =
        manager.authorizationStatus.toStatus()

    override suspend fun requestForeground(): LocationPermissionStatus {
        val current = manager.authorizationStatus
        if (current != kCLAuthorizationStatusNotDetermined) return current.toStatus()

        val deferred = CompletableDeferred<CLAuthorizationStatus>()
        pendingAuthResult = deferred
        manager.requestWhenInUseAuthorization()
        val result = deferred.await()
        pendingAuthResult = null
        return result.toStatus()
    }

    override suspend fun requestNotifications(): Boolean = true

    override suspend fun requestBackground(): LocationPermissionStatus {
        val current = manager.authorizationStatus
        if (current == kCLAuthorizationStatusAuthorizedAlways) return LocationPermissionStatus.GrantedAlways
        if (current != kCLAuthorizationStatusAuthorizedWhenInUse) {
            // Must hold "When In Use" before "Always" can be requested.
            return current.toStatus()
        }

        val deferred = CompletableDeferred<CLAuthorizationStatus>()
        pendingAuthResult = deferred
        manager.requestAlwaysAuthorization()
        val result = deferred.await()
        pendingAuthResult = null
        return result.toStatus()
    }

    private fun CLAuthorizationStatus.toStatus(): LocationPermissionStatus = when (this) {
        kCLAuthorizationStatusAuthorizedAlways -> LocationPermissionStatus.GrantedAlways
        kCLAuthorizationStatusAuthorizedWhenInUse -> LocationPermissionStatus.GrantedForeground
        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted,
        kCLAuthorizationStatusNotDetermined,
            -> LocationPermissionStatus.Denied

        else -> LocationPermissionStatus.Denied
    }
}

@Composable
actual fun rememberLocationPermissionController(): LocationPermissionController {
    return remember { IosLocationPermissionController(CLLocationManager()) }
}
