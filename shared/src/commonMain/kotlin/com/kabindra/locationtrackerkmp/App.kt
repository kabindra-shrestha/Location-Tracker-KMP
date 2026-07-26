package com.kabindra.locationtrackerkmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kabindra.locationtracker.createLocationTracker
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingState
import com.kabindra.locationtracker.permission.LocationPermissionStatus
import com.kabindra.locationtracker.permission.rememberLocationPermissionController

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LocationTrackingScreen()
        }
    }
}

@Composable
private fun LocationTrackingScreen() {
    val permissionController = rememberLocationPermissionController()
    val tracker = remember { createLocationTracker() }

    val trackingState by tracker.state.collectAsState()
    var lastLocation by remember { mutableStateOf<TrackedLocation?>(null) }
    var permissionStatus by remember { mutableStateOf<LocationPermissionStatus?>(null) }

    LaunchedEffect(Unit) {
        permissionStatus = permissionController.status()
    }

    LaunchedEffect(tracker) {
        tracker.locations.collect { location -> lastLocation = location }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Permission: ${permissionStatus ?: "unknown"}")
        Text("Tracking state: $trackingState")
        lastLocation?.let {
            Text(
                "Last fix: %.5f, %.5f (±%.0fm)".format(
                    it.latitude,
                    it.longitude,
                    it.accuracyMeters
                )
            )
        }

        Button(onClick = {
            // In a real app, run this from a coroutine scope tied to the screen's
            // lifecycle (e.g. a ViewModel), not directly in an onClick lambda.
        }) {
            Text("See README for the full request → start flow")
        }

        Button(
            enabled = trackingState !is TrackingState.Running,
            onClick = { /* wire to a coroutine that awaits permission then calls tracker.start(...) */ },
        ) {
            Text("Start tracking")
        }

        Button(
            enabled = trackingState is TrackingState.Running,
            onClick = { tracker.stop() },
        ) {
            Text("Stop tracking")
        }
    }
}

/**
 * The permission → start sequence, extracted so it reads clearly as the
 * intended usage pattern. Call this from a `rememberCoroutineScope().launch { }`
 * bound to a button click.
 */
suspend fun requestPermissionThenStartTracking(
    permissionController: com.kabindra.locationtracker.permission.LocationPermissionController,
    tracker: com.kabindra.locationtracker.LocationTracker,
    config: TrackingConfig = TrackingConfig(),
) {
    val foregroundStatus = permissionController.requestForeground()
    if (foregroundStatus == LocationPermissionStatus.Denied) return // show rationale / settings deep link

    val backgroundStatus = permissionController.requestBackground()
    if (backgroundStatus != LocationPermissionStatus.GrantedAlways) {
        // Foreground-only tracking still works while the app is visible; decide
        // per your product requirements whether that's acceptable to proceed with.
    }

    tracker.start(config)
}
