# Product Requirements Document (PRD): KMP/CMP Background Location Tracker

## 1. Overview & Problem Statement

The KMP Location Tracker SDK provides robust, continuous, background-capable location tracking
across Android and iOS platforms built with Kotlin Multiplatform (KMP) and Compose Multiplatform (
CMP).

Currently, the application UI does not trigger permission dialogs or start the background location
tracking engine due to unimplemented button click handlers and missing iOS permission key
definitions in `Info.plist`. Additionally, enterprise requirements (backend control toggles,
schedule/attendance windows, distance displacement filtering, and periodic sync dispatching) must be
fully specified and implemented in an decoupled SDK architecture.

---

## 2. Core Requirements

### Requirement 1: Core Location Tracking

- Continuously obtain high-accuracy GPS / network location updates (`latitude`, `longitude`,
  `accuracy`, `speed`, `bearing`, `altitude`, `timestamp`).
- Support configurable accuracy levels (`HIGH_ACCURACY`, `BALANCED`, `LOW_POWER`).

### Requirement 2: Background Location Tracking

- **Android**: Execute inside an explicit Foreground Service (`LocationForegroundService`)
  configured with `android:foregroundServiceType="location"`. Continue capturing location fixes even
  when the app is minimized, screen is locked, or app is removed from recent tasks.
- **iOS**: Utilize `CLLocationManager` with `allowsBackgroundLocationUpdates = true`,
  `pausesLocationUpdatesAutomatically = false`, and background mode `location` configured in
  `Info.plist`.

### Requirement 3: Backend Policy Model (`LocationTrackerPolicy`)

- Define a policy configuration model initialized with sensible defaults in the SDK and
  supplied/overridden by the host application upon SDK start.
- `isTrackingEnabled: Boolean` (default `true`): If `false`, immediately suspend location
  collection, stop background services, and remove tracking notifications.

### Requirement 4: Notification & Status Indicator

- **Android**: Display an ongoing, non-dismissible notification while background tracking is active.
  Show current tracking state and total points tracked.
- **iOS**: Enable `showsBackgroundLocationIndicator = true` so the blue status bar banner / dynamic
  island indicator is visible when location updates are captured in the background.

### Requirement 5: Tracking Modes (Schedule vs Check-In/Out)

The system must support two operating modes provided via `LocationTrackerPolicy`:

1. **Time Range Schedule Mode**: Automatically activate location tracking during specified daily
   time windows (e.g. 09:00 - 17:00 or 10:00 - 18:00). Automatically pause outside this window.
2. **Attendance / Check-In Check-Out Mode**: Track location only while the user is actively
   checked-in (`isCheckedIn: Boolean`). Tracking starts upon Check-In and halts immediately upon
   Check-Out.

### Requirement 6: Distance Threshold Filtering (50m Displacement)

- To conserve battery and backend storage/bandwidth, perform displacement filtering inside the SDK.
- Compare each new location fix against the **last reported/sent location point**.
- If the calculated distance between the new fix and the last reported position is **less than the
  threshold** (default 50 meters, or policy-configured `minDistanceThresholdMeters`), **do not
  send/queue** the update.

### Requirement 7: Periodic Sync Callback (Decoupled Network API)

- **Architecture Constraint**: The SDK MUST NOT handle network HTTP requests directly or contain
  backend API client logic. Network APIs reside entirely in the parent host application.
- The SDK buffers locations passing the 50m displacement filter and invokes a callback / listener
  interface (`onSyncLocations: suspend (List<TrackedLocation>) -> Unit`) supplied by the host
  application at configurable time intervals (e.g. every 4m, 5m, 10m).
- Host application receives batch location fixes in the callback and dispatches them to its own
  backend server.

### Requirement 8: Explicit 2-Step Permission Request Flow

- **Foreground Request**: Prompt user for `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` on
  Android, `WhenInUse` on iOS.
- **Background Upgrade Request**: Prompt user for `ACCESS_BACKGROUND_LOCATION` on Android (guiding
  user to system settings if Android 11+), `Always` authorization on iOS.
- **Android Notification Permission**: Request `POST_NOTIFICATIONS` on Android 13+ (API 33+) before
  launching the foreground service.

---

## 3. Architecture & Data Models

### Policy & Configuration Models (`location-tracker`)

```kotlin
data class LocationTrackerPolicy(
    val isTrackingEnabled: Boolean = true,
    val trackingMode: TrackingMode = TrackingMode.TIME_RANGE,
    val scheduleWindow: ScheduleWindow? = ScheduleWindow(startHour = 9, startMinute = 0, endHour = 17, endMinute = 0),
    val isCheckedIn: Boolean = false,
    val minDistanceThresholdMeters: Float = 50.0f,
    val syncIntervalMinutes: Int = 5
)

enum class TrackingMode { TIME_RANGE, CHECK_IN_OUT }

data class ScheduleWindow(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

/** Callback supplied by the host app; return true only after backend acknowledgement. */
fun interface LocationTrackingListener {
    suspend fun onTrackingEvent(event: LocationTrackingEvent): Boolean
}
```

---

## 4. Platform Specifications & Permissions

- **Android `AndroidManifest.xml`**:
    - `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
    - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`
- **iOS `Info.plist`**:
    - `NSLocationWhenInUseUsageDescription`
    - `NSLocationAlwaysAndWhenInUseUsageDescription`
    - `NSLocationAlwaysUsageDescription`
    - `UIBackgroundModes` array containing `location`
