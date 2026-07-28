package com.kabindra.locationtrackerkmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.kabindra.locationtracker.model.LocationTrackerPolicy
import com.kabindra.locationtracker.model.ScheduleWindow
import com.kabindra.locationtracker.model.TrackedLocation
import com.kabindra.locationtracker.model.TrackingConfig
import com.kabindra.locationtracker.model.TrackingMode
import com.kabindra.locationtracker.permission.LocationPermissionController
import com.kabindra.locationtracker.permission.LocationPermissionStatus
import com.kabindra.locationtracker.permission.rememberLocationPermissionController
import com.kabindra.locationtracker.session.LocationSyncStatus
import com.kabindra.locationtracker.session.LocationTrackingSession
import com.kabindra.locationtracker.session.TrackedLocationDebugEntry
import com.kabindra.locationtracker.session.formatTrackingTimestamp
import com.kabindra.locationtrackerkmp.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun App() {
    AppTheme {
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

    val trackingState by tracker.state.collectAsState()
    val sessionState by LocationTrackingSession.state.collectAsState()
    val developerMode by LocationTrackingSession.developerMode.collectAsState()

    var lastLocation by remember { mutableStateOf<TrackedLocation?>(null) }
    var permissionStatus by remember { mutableStateOf<LocationPermissionStatus?>(null) }
    var isRequestingPermission by remember { mutableStateOf(false) }
    var showTrackedLocations by remember { mutableStateOf(false) }

    // Backend policy simulation state
    var backendTrackingEnabled by remember { mutableStateOf(true) }
    var trackingMode by remember { mutableStateOf(TrackingMode.TIME_RANGE) }
    var isCheckedIn by remember { mutableStateOf(false) }
    var minDistanceThreshold by remember { mutableStateOf(50f) }
    var syncIntervalMinutes by remember { mutableStateOf(1) } // 1 min default for demo/testing

    // In production the host app receives this policy from its backend.
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
        )
    }

    LaunchedEffect(policy) {
        val stoppedByPolicy = LocationTrackingSession.updatePolicy(policy)
        if (stoppedByPolicy) {
            tracker.stop()
        }
    }

    LaunchedEffect(Unit) {
        permissionStatus = permissionController.status()
    }

    // Collect location updates and route through syncManager
    LaunchedEffect(tracker) {
        tracker.locations.collect { location ->
            lastLocation = location
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
                enabled = LocationTrackingSession.canStart() && !isRequestingPermission,
                onClick = {
                    scope.launch {
                        isRequestingPermission = true
                        val started =
                            requestPermissionThenStartTracking(permissionController, tracker)
                        permissionStatus = permissionController.status()
                        isRequestingPermission = false
                    }
                }
            ) {
                Text(if (isRequestingPermission) "Requesting..." else "Start Tracking")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = sessionState.isActive,
                onClick = {
                    tracker.stop()
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
                Text("Location sampling is configured separately; backend delivery uses the 50m filter.")
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
                Text("Tracking Session Active: ${sessionState.isActive}")
                Text("Pending Backend Events: ${sessionState.pendingEventCount}")
                Text("Last Successful Backend Location: ${sessionState.lastSuccessfullyDeliveredLocation ?: "None"}")
                sessionState.lastError?.let {
                    Text(
                        "Last Sync Error: $it",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (developerMode) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Developer Tools", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = { showTrackedLocations = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Tracked Locations")
                    }
                }
            }
        }

        HorizontalDivider()

        // Session details are persisted across normal process recreation.
        Text("Persisted Session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(12.dp)
        ) {
            Text(
                "Last known location: ${sessionState.lastKnownLocation ?: "None"}",
                color = Color.White
            )
            Text("Start is enabled only when the backend policy permits it.", color = Color.Gray)
        }
    }

    if (showTrackedLocations && developerMode) {
        TrackedLocationsBottomSheet(
            locations = sessionState.trackedLocations,
            onDismiss = { showTrackedLocations = false },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TrackedLocationsBottomSheet(
    locations: List<TrackedLocationDebugEntry>,
    onDismiss: () -> Unit,
) {
    val successfulCount = locations.count { it.syncStatus == LocationSyncStatus.SYNCED }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text("Tracked Locations", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Total: ${locations.size}  •  Successfully synced: $successfulCount")
            if (locations.isEmpty()) {
                Text(
                    "No locations recorded for this tracking session.",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        locations,
                        key = { "${it.eventKind}-${it.location.timestampMs}" }) { entry ->
                        DebugLocationCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugLocationCard(entry: TrackedLocationDebugEntry) {
    val location = entry.location
    val isSynced = entry.syncStatus == LocationSyncStatus.SYNCED
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                "${if (isSynced) "✓" else "✕"} ${entry.syncStatus}",
                color = if (isSynced) Color(0xFF2E7D32) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
            Text("Event: ${entry.eventKind}", fontWeight = FontWeight.SemiBold)
            Text("Latitude & Longitude: ${location.latitude}, ${location.longitude}")
            Text("Date & Time: ${formatTrackingTimestamp(location.timestampMs)}")
            Text("Accuracy: ±${location.accuracyMeters} m")
            location.speedMetersPerSecond?.let { Text("Speed: $it m/s") }
            location.bearingDegrees?.let { Text("Bearing: $it°") }
            location.altitudeMeters?.let { Text("Altitude: $it m") }
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
