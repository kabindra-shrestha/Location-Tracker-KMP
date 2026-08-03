package com.kabindra.locationtrackerkmp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import com.kabindra.locationtracker.LocationTrackingEngine
import com.kabindra.locationtracker.createLocationTracker
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
import locationtrackerkmp.shared.generated.resources.Res
import locationtrackerkmp.shared.generated.resources.info
import org.jetbrains.compose.resources.painterResource

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

    var permissionStatus by remember { mutableStateOf<LocationPermissionStatus?>(null) }
    var isRequestingPermission by remember { mutableStateOf(false) }
    var showTrackedLocations by remember { mutableStateOf(false) }

    // In production the host app receives this complete policy from its backend.
    val policy by DemoTrackingBackend.policy.collectAsState()

    LaunchedEffect(policy) {
        LocationTrackingEngine.updatePolicy(policy)
    }

    LaunchedEffect(Unit) {
        permissionStatus = permissionController.status()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
                Text(
                    "Tracker Engine State: ${if (sessionState.isActive) "Running" else "Idle"}",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Platform Tracker State: $trackingState",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                sessionState.lastKnownLocation?.let {
                    Text("Last Fix: ${it.latitude}, ${it.longitude}")
                    Text("Accuracy: ±${it.accuracyMeters}m | Speed: ${it.speedMetersPerSecond ?: 0f} m/s")
                } ?: Text("Last Fix: No location fix yet", color = Color.Gray)
            }
        }

        // Permission enables policy-controlled tracking; it is not a manual tracking control.
        if (permissionStatus != LocationPermissionStatus.GrantedAlways) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRequestingPermission,
                onClick = {
                    scope.launch {
                        isRequestingPermission = true
                        val granted = requestBackgroundTrackingPermission(permissionController)
                        permissionStatus = permissionController.status()
                        if (granted) LocationTrackingEngine.updatePolicy(policy)
                        isRequestingPermission = false
                    }
                }
            ) {
                Text(if (isRequestingPermission) "Requesting..." else "Enable Background Location")
            }
        } else {
            Text(
                "Background location is enabled. Tracking follows the current backend policy.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    Text(
                        text = "Backend Master Flag (isTrackingEnabled)",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Switch(
                        checked = policy.isTrackingEnabled,
                        onCheckedChange = { enabled ->
                            DemoTrackingBackend.updatePolicy { it.copy(isTrackingEnabled = enabled) }
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tracking Mode: ${policy.trackingMode.name}",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(onClick = {
                        DemoTrackingBackend.updatePolicy {
                            it.copy(
                                trackingMode = if (it.trackingMode == TrackingMode.TIME_RANGE) {
                                    TrackingMode.CHECK_IN_OUT
                                } else {
                                    TrackingMode.TIME_RANGE
                                },
                            )
                        }
                    }) {
                        Text("Switch Mode")
                    }
                }

                if (policy.trackingMode == TrackingMode.CHECK_IN_OUT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !policy.isCheckedIn && !isRequestingPermission,
                            onClick = {
                                scope.launch {
                                    isRequestingPermission = true
                                    val granted =
                                        requestBackgroundTrackingPermission(permissionController)
                                    permissionStatus = permissionController.status()
                                    if (granted) LocationTrackingEngine.requestCheckIn()
                                    isRequestingPermission = false
                                }
                            },
                        ) { Text("Check In") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = policy.isCheckedIn && !isRequestingPermission,
                            onClick = { scope.launch { LocationTrackingEngine.requestCheckOut() } },
                        ) { Text("Check Out") }
                    }
                    Text(
                        "Check-In and Check-Out call the host attendance API. Its returned policy " +
                                "is the only thing that starts or stops tracking.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    val window = policy.scheduleWindow
                    Text(
                        "Automatic: the engine starts at ${
                            window?.startHour?.toString()?.padStart(2, '0') ?: "--"
                        }:" +
                                "${
                                    window?.startMinute?.toString()?.padStart(2, '0') ?: "--"
                                } and stops at " +
                                "${window?.endHour?.toString()?.padStart(2, '0') ?: "--"}:" +
                                "${window?.endMinute?.toString()?.padStart(2, '0') ?: "--"}.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text("Distance Displacement Filter: ${policy.minDistanceThresholdMeters.toInt()} meters")
                Text("Location sampling is configured separately; backend delivery uses the displacement filter.")
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
                Text("Session ID: ${sessionState.sessionId ?: "None"}")
                Text("Pending Backend Events: ${sessionState.pendingEventCount}")
                Text("Retry Queue: ${sessionState.pendingEventCount}")
                Text("Last Raw Fix: ${sessionState.lastKnownLocation ?: "None"}")
                Text("Last Successful Backend Location: ${sessionState.lastSuccessfullyDeliveredLocation ?: "None"}")
                Text(
                    "Last Sync: ${
                        sessionState.lastSyncTimestampMs.takeIf { it > 0L }
                            ?.let(::formatTrackingTimestamp) ?: "None"
                    }",
                )
                Text(
                    "Next Sync: ${
                        sessionState.nextSyncTimestampMs.takeIf { it > 0L }
                            ?.let(::formatTrackingTimestamp) ?: "Not scheduled"
                    }",
                )
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
                    Text(
                        "Platform boundary: normal backgrounding and Android Recents swipe are supported. " +
                                "Android force-stop and iOS force-quit require reopening the app and starting again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            Text(
                "Tracking is controlled by the persisted backend policy, not generic Start/Stop buttons.",
                color = Color.Gray
            )
        }
    }

    if (showTrackedLocations && developerMode) {
        TrackedLocationsBottomSheet(
            locations = sessionState.trackedLocations,
            onDismiss = { showTrackedLocations = false },
        )
    }
}

enum class LocationFilter(val label: String, val description: String) {
    ALL("All", "Shows every location fix received from the platform engine."),
    SYNCED("Synced", "Locations successfully accepted by the backend uploader."),
    PENDING("Pending", "Locations waiting for the next sync interval or retry."),
    FILTERED(
        "Filtered",
        "Locations dropped because movement was less than the distance threshold."
    ),
    FAILED("Failed", "Locations that failed to upload after multiple retries.")
}

@Composable
private fun ExpressiveFilterItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
    // Morph from soft-rounded (12dp) to capsule (28dp or more)
    val cornerRadius by animateDpAsState(if (isSelected) 28.dp else 12.dp)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() },
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TrackedLocationsBottomSheet(
    locations: List<TrackedLocationDebugEntry>,
    onDismiss: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(LocationFilter.ALL) }
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    // Custom position provider to center the tooltip horizontally regardless of anchor position
    val tooltipPositionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = (windowSize.width - popupContentSize.width) / 2
                val verticalGap = 8
                var y = anchorBounds.bottom + verticalGap

                // If tooltip doesn't fit below, show it above the anchor
                if (y + popupContentSize.height > windowSize.height) {
                    y = anchorBounds.top - popupContentSize.height - verticalGap
                }
                return IntOffset(x, y)
            }
        }
    }

    val totalCount = locations.size
    val syncedCount = locations.count { it.syncStatus == LocationSyncStatus.SYNCED }
    val pendingCount = locations.count { it.syncStatus == LocationSyncStatus.PENDING }
    val filteredCount = locations.count { it.syncStatus == LocationSyncStatus.FILTERED }
    val failedCount = locations.count { it.syncStatus == LocationSyncStatus.FAILED }

    val filteredLocations = when (selectedFilter) {
        LocationFilter.ALL -> locations
        LocationFilter.SYNCED -> locations.filter { it.syncStatus == LocationSyncStatus.SYNCED }
        LocationFilter.PENDING -> locations.filter { it.syncStatus == LocationSyncStatus.PENDING }
        LocationFilter.FILTERED -> locations.filter { it.syncStatus == LocationSyncStatus.FILTERED }
        LocationFilter.FAILED -> locations.filter { it.syncStatus == LocationSyncStatus.FAILED }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tracked Locations", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                TooltipBox(
                    positionProvider = tooltipPositionProvider,
                    tooltip = {
                        RichTooltip(
                            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                            title = { Text("Filter Definitions") },
                            action = {
                                TextButton(onClick = { scope.launch { tooltipState.dismiss() } }) {
                                    Text("Got it")
                                }
                            }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                LocationFilter.entries.forEach { filter ->
                                    Text(
                                        text = filter.label,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = filter.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (filter != LocationFilter.FAILED) {
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    },
                    state = tooltipState
                ) {
                    IconButton(onClick = { scope.launch { tooltipState.show() } }) {
                        Icon(
                            painter = painterResource(Res.drawable.info),
                            contentDescription = "Filter definitions",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text("Total: $totalCount  •  Synced: $syncedCount", color = Color.Gray)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LocationFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        LocationFilter.ALL -> totalCount
                        LocationFilter.SYNCED -> syncedCount
                        LocationFilter.PENDING -> pendingCount
                        LocationFilter.FILTERED -> filteredCount
                        LocationFilter.FAILED -> failedCount
                    }
                    ExpressiveFilterItem(
                        label = filter.label,
                        count = count,
                        isSelected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            if (filteredLocations.isEmpty()) {
                val emptyMessage = if (locations.isEmpty()) {
                    "No locations recorded for this tracking session."
                } else {
                    "No locations match the '${selectedFilter.label}' filter."
                }
                Text(
                    emptyMessage,
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        filteredLocations,
                        key = { it.id }) { entry ->
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
    val clipboardManager = LocalClipboardManager.current

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        "Latitude & Longitude: ${location.latitude}, ${location.longitude}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("${location.latitude}, ${location.longitude}"))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }
            }

            Text("Date & Time: ${formatTrackingTimestamp(location.timestampMs)}")
            Text("Accuracy: ±${location.accuracyMeters} m")
            location.speedMetersPerSecond?.let { Text("Speed: $it m/s") }
            location.bearingDegrees?.let { Text("Bearing: $it°") }
            location.altitudeMeters?.let { Text("Altitude: $it m") }
        }
    }
}

suspend fun requestBackgroundTrackingPermission(
    permissionController: LocationPermissionController,
): Boolean {
    val foregroundStatus = permissionController.requestForeground()
    if (foregroundStatus == LocationPermissionStatus.Denied) return false

    // Android's foreground-service notification and iOS notifications are useful user feedback,
    // but neither permission result is allowed to block the location lifecycle.
    permissionController.requestNotifications()

    val backgroundStatus = permissionController.requestBackground()
    if (backgroundStatus != LocationPermissionStatus.GrantedAlways) return false
    return true
}
