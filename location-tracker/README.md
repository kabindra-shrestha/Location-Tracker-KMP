# Location Tracker Library

A Kotlin Multiplatform library for continuous foreground **and background** location tracking on Android and iOS.

## Why this exists

While excellent KMP location toolkits like [Compass](https://github.com/jordond/compass) exist, they often focus on foreground use cases. This library extends these capabilities by implementing **continuous background tracking natively**:
- **Android**: An explicit Foreground Service (`FusedLocationProviderClient` + `foregroundServiceType="location"`).
- **iOS**: `CLLocationManager` configured with `allowsBackgroundLocationUpdates`.

## Core Technologies
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform** (for UI-related helpers)
- **Compass**: Used for one-shot current location and permission management infrastructure.
- **Google Play Services (Location)**: Powering the Android implementation.
- **CoreLocation**: Powering the iOS implementation.

## Features
- ✅ **Background Persistence**: Tracking continues even if the app is minimized or the screen is locked.
- ✅ **Distance Filtering**: Built-in Haversine distance filtering (default 50m) to save battery and bandwidth.
- ✅ **Tracking Modes**: Support for Schedule-based (Time Range) and Attendance-based (Check-in/out) tracking.
- ✅ **Durable Session**: In-memory and persistent state management for location synchronization.
- ✅ **Retry Mechanism**: Automatically retries failed backend syncs.

## Setup

### 1. Add the GitHub Packages repository
`settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/<owner>/location-tracker-kmp")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.token").orNull
            }
        }
    }
}
```

### 2. Add the dependency
```kotlin
commonMain.dependencies {
    implementation("com.kabindra:location-tracker:0.1.0")
}
```

### 3. Initialize on Android
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocationTrackerInit.initialize(this)
    }
}
```

## Usage

```kotlin
@Composable
fun TrackingScreen() {
    val permissionController = rememberLocationPermissionController()
    val tracker = remember { createLocationTracker() }
    val scope = rememberCoroutineScope()
    val state by tracker.state.collectAsState()

    LaunchedEffect(tracker) {
        tracker.locations.collect { location ->
            // Use location fix
        }
    }

    Button(onClick = {
        scope.launch {
            val foreground = permissionController.requestForeground()
            if (foreground != LocationPermissionStatus.Granted) return@launch

            val background = permissionController.requestBackground()
            // Recommendation: Check for background status if persistence is required

            tracker.start(TrackingConfig(
                intervalMs = 10_000,
                minUpdateDistanceMeters = 20f,
                priority = LocationPriority.HIGH_ACCURACY,
                notificationSmallIconResId = R.drawable.ic_notification // Android REQUIRED
            ))
        }
    }) {
        Text("Start Tracking")
    }
}
```

## Platform Setup

### Android Manifest
Permissions are merged automatically. You must provide a notification icon for the foreground service.

### iOS Info.plist
```xml
<key>UIBackgroundModes</key>
<array>
    <string>location</string>
</array>
<key>NSLocationWhenInUseUsageDescription</key>
<string>Description for foreground tracking</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>Description for background tracking</string>
```

## Internal Synchronization Flow

The library handles synchronization state through `LocationTrackingSession`:

| Status | Meaning |
| :--- | :--- |
| `PENDING` | Fix accepted, waiting for backend acknowledgement. |
| `SYNCED` | Backend confirmed success. Reference point updated. |
| `FILTERED` | Fix dropped due to distance threshold (e.g., < 50m moved). |
| `FAILED` | Upload failed; fix retained in queue for retry. |

## Technical Documentation
For a deep dive into the native implementations and synchronization logic, see **[TECHNICAL_DOCS.md](TECHNICAL_DOCS.md)**.
