package com.kabindra.locationtrackerkmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabindra.locationtracker.createLocationTracker
import com.kabindra.locationtracker.model.LocationSyncListener
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.ScheduleWindow
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingMode
import com.kabindra.locationtracker.model.TrackingState
import com.kabindra.locationtracker.permission.LocationPermissionController
import com.kabindra.locationtracker.permission.LocationPermissionStatus
import com.kabindra.locationtracker.permission.rememberLocationPermissionController
import com.kabindra.locationtracker.schedule.currentLocalScheduleTime
import com.kabindra.locationtracker.sync.LocationSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()
    val permissionController = rememberLocationPermissionController()
    val tracker = remember { createLocationTracker() }
    val syncManager = remember { LocationSyncManager() }

    val trackingState by tracker.state.collectAsState()
    val syncStats by syncManager.stats.collectAsState()

    var lastLocation by remember { mutableStateOf<TrackedLocation?>(null) }
    var permissionStatus by remember { mutableStateOf<LocationPermissionStatus?>(null) }
    var isRequestingPermission by remember { mutableStateOf(false) }

    // Host app API log
    val apiLogs = remember { mutableStateListOf<String>() }

    // Backend policy simulation state
    var backendTrackingEnabled by remember { mutableStateOf(true) }
    var trackingMode by remember { mutableStateOf(TrackingMode.TIME_RANGE) }
    var isCheckedIn by remember { mutableStateOf(false) }
    var minDistanceThreshold by remember { mutableStateOf(50f) }
    var syncIntervalMinutes by remember { mutableStateOf(1) } // 1 min default for demo/testing

    // Create policy with host app API callback
    val policy = remember(
        backendTrackingEnabled,
        trackingMode,
        isCheckedIn,
        minDistanceThreshold,
        syncIntervalMinutes
    ) {
        LocationTrackerPolicy(
            isTrackingEnabled = backendTrackingEnabled,
            trackingMode = trackingMode,
            scheduleWindow = ScheduleWindow(
                startHour = 0,
                startMinute = 0,
                endHour = 23,
                endMinute = 59
            ), // 24h for active testing
            isCheckedIn = isCheckedIn,
            minDistanceThresholdMeters = minDistanceThreshold,
            syncIntervalMinutes = syncIntervalMinutes,
            onSyncLocations = LocationSyncListener { locations ->
                val logEntry =
                    "[API Host Sync] Dispatched ${locations.size} location point(s) to Backend API!"
                withContext(Dispatchers.Main) {
                    apiLogs.add(0, logEntry)
                    if (apiLogs.size > 15) apiLogs.removeLast()
                }
            }
        )
    }

    LaunchedEffect(policy) {
        syncManager.updatePolicy(policy)
        if (!policy.isTrackingEnabled ||
            (policy.trackingMode == TrackingMode.CHECK_IN_OUT && !policy.isCheckedIn)
        ) {
            tracker.stop()
            syncManager.stop()
        }
    }

    LaunchedEffect(Unit) {
        permissionStatus = permissionController.status()
    }

    // Collect location updates and route through syncManager
    LaunchedEffect(tracker) {
        tracker.locations.collect { location ->
            lastLocation = location
            val localTime = currentLocalScheduleTime()
            // Route through 50m displacement filter & schedule evaluator
            syncManager.processLocation(
                location = location,
                currentHour = localTime.hour,
                currentMinute = localTime.minute,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "KMP Location Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Permission Status: ${permissionStatus ?: "Checking..."}",
                    fontWeight = FontWeight.SemiBold
                )
                Text("Tracker Engine State: $trackingState", fontWeight = FontWeight.SemiBold)
                lastLocation?.let {
                    Text("Last Fix: ${it.latitude}, ${it.longitude}")
                    Text("Accuracy: ±${it.accuracyMeters}m | Speed: ${it.speedMetersPerSecond ?: 0f} m/s")
                } ?: Text("Last Fix: No location fix yet", color = Color.Gray)
            }
        }

        // Action Buttons: Permission & Tracking Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = trackingState !is TrackingState.Running && !isRequestingPermission,
                onClick = {
                    scope.launch {
                        isRequestingPermission = true
                        val started =
                            requestPermissionThenStartTracking(permissionController, tracker)
                        if (started) syncManager.start()
                        permissionStatus = permissionController.status()
                        isRequestingPermission = false
                    }
                }
            ) {
                Text(if (isRequestingPermission) "Requesting..." else "Start Tracking")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = trackingState is TrackingState.Running,
                onClick = {
                    tracker.stop()
                    syncManager.stop()
                }
            ) {
                Text("Stop Tracking")
            }
        }

        HorizontalDivider()

        // Backend Policy Controls
        Text("Backend Policy Controls", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Backend Master Flag (isTrackingEnabled)")
                    Switch(
                        checked = backendTrackingEnabled,
                        onCheckedChange = { backendTrackingEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tracking Mode: ${trackingMode.name}")
                    Button(onClick = {
                        trackingMode = if (trackingMode == TrackingMode.TIME_RANGE) {
                            TrackingMode.CHECK_IN_OUT
                        } else {
                            TrackingMode.TIME_RANGE
                        }
                    }) {
                        Text("Switch Mode")
                    }
                }

                if (trackingMode == TrackingMode.CHECK_IN_OUT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Check-In Status: ${if (isCheckedIn) "Checked IN" else "Checked OUT"}")
                        Switch(
                            checked = isCheckedIn,
                            onCheckedChange = { isCheckedIn = it }
                        )
                    }
                }

                Text("Distance Displacement Filter: ${minDistanceThreshold.toInt()} meters")
                Text("Sync Dispatch Interval: $syncIntervalMinutes minute(s)")
            }
        }

        HorizontalDivider()

        // Engine & Displacement Filter Statistics
        Text("Displacement Filter & Engine Stats", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Total Raw Fixes Received: ${syncStats.totalReceived}")
                Text("Filtered Out (<50m movement): ${syncStats.totalFilteredOutWithinThreshold}")
                Text("Queued for Sync: ${syncStats.totalQueuedForSync}")
                Text("Host App API Dispatches Triggered: ${syncStats.totalSyncDispatches}")
                syncStats.lastSyncError?.let {
                    Text(
                        "Last Sync Error: $it",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        scope.launch { syncManager.flushAndSyncNow() }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Force Flush & Sync to Host API Now")
                }
            }
        }

        HorizontalDivider()

        // Host Application API Dispatch Log
        Text("Host App API Callback Log", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(12.dp)
        ) {
            if (apiLogs.isEmpty()) {
                Text("No API sync dispatches logged yet...", color = Color.Gray)
            } else {
                apiLogs.forEach { log ->
                    Text(log, color = Color(0xFF4CAF50), fontSize = 12.sp)
                }
            }
        }
    }
}

suspend fun requestPermissionThenStartTracking(
    permissionController: LocationPermissionController,
    tracker: com.kabindra.locationtracker.LocationTracker,
    config: TrackingConfig = TrackingConfig(),
): Boolean {
    val foregroundStatus = permissionController.requestForeground()
    if (foregroundStatus == LocationPermissionStatus.Denied) return false

    if (!permissionController.requestNotifications()) return false

    val backgroundStatus = permissionController.requestBackground()
    if (backgroundStatus != LocationPermissionStatus.GrantedAlways) return false
    tracker.start(config)
    return true
}
