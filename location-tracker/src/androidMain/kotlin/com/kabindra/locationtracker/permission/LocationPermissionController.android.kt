package com.kabindra.locationtracker.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

/**
 * Android implementation backed by [androidx.activity.result.contract.ActivityResultContracts].
 *
 * Registering the launcher requires composition (it must be registered before
 * the host Activity reaches STARTED), which is why this is exposed only via
 * the @Composable [rememberLocationPermissionController] factory rather than a
 * plain constructor.
 */
private class AndroidLocationPermissionController(
    private val context: Context,
    private val requestForegroundLauncher: PermissionLauncher,
    private val requestBackgroundLauncher: PermissionLauncher,
) : LocationPermissionController {

    override suspend fun status(): LocationPermissionStatus {
        val fineGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fineGranted && !coarseGranted) return LocationPermissionStatus.Denied

        val backgroundGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return if (backgroundGranted) {
            LocationPermissionStatus.GrantedAlways
        } else {
            LocationPermissionStatus.GrantedForeground
        }
    }

    override suspend fun requestForeground(): LocationPermissionStatus {
        requestForegroundLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
        )
        return status()
    }

    override suspend fun requestBackground(): LocationPermissionStatus {
        // Below Android 10, background location is implied by the foreground grant.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return status()

        requestBackgroundLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        return status()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Bridges the callback-based [androidx.activity.result.ActivityResultLauncher] to a
 * single-shot suspend call, so [LocationPermissionController]'s methods can be `suspend fun`
 * without exposing callback plumbing to the caller.
 */
private class PermissionLauncher(
    private val launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
) {
    private var pending: CompletableDeferred<Map<String, Boolean>>? = null

    suspend fun launch(permissions: Array<String>): Map<String, Boolean> {
        val deferred = CompletableDeferred<Map<String, Boolean>>()
        pending = deferred
        launcher.launch(permissions)
        return deferred.await()
    }

    fun onResult(result: Map<String, Boolean>) {
        pending?.complete(result)
        pending = null
    }
}

@Composable
actual fun rememberLocationPermissionController(): LocationPermissionController {
    val context = LocalContext.current

    val foregroundLauncherHolder = remember { arrayOfNulls<PermissionLauncher>(1) }
    val foregroundActivityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> foregroundLauncherHolder[0]?.onResult(result) }

    val backgroundLauncherHolder = remember { arrayOfNulls<PermissionLauncher>(1) }
    val backgroundActivityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> backgroundLauncherHolder[0]?.onResult(result) }

    return remember {
        val foregroundLauncher = PermissionLauncher(foregroundActivityLauncher)
            .also { foregroundLauncherHolder[0] = it }
        val backgroundLauncher = PermissionLauncher(backgroundActivityLauncher)
            .also { backgroundLauncherHolder[0] = it }

        AndroidLocationPermissionController(context, foregroundLauncher, backgroundLauncher)
    }
}
