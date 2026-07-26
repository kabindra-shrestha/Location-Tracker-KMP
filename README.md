# KMP Location Tracker

Kotlin Multiplatform / Compose Multiplatform library for continuous foreground
**and background** location tracking on Android and iOS — built as a reusable
library module you can publish to GitHub Packages and pull into any project
(employee tracking, ride-sharing, delivery, etc.).

## Why this exists

[Compass](https://github.com/jordond/compass) is a great KMP location toolkit,
but as of this writing it does **not** support background location updates
([issue #250](https://github.com/jordond/compass/issues/250),
[issue #90](https://github.com/jordond/compass/issues/90)) — its
`Geolocator` is built for foreground "get current location" / "track while
app is open" use cases.

This library:

- Uses **Compass** (`compass-geolocation` / `compass-geolocation-mobile`) for
  one-shot current-location lookups — see [
  `CompassCurrentLocation`](location-tracker/src/commonMain/kotlin/dev/kabin/locationtracker/compass/CompassCurrentLocation.kt).
- Implements **continuous background tracking natively** for the parts Compass
  doesn't cover yet: an Android foreground service (`FusedLocationProviderClient`
    + `foregroundServiceType="location"`) and an iOS `CLLocationManager` configured
      with `allowsBackgroundLocationUpdates`.

If Compass adds background support in a future release, the native
implementations here can be swapped out without touching the public
`LocationTracker` / `LocationPermissionController` interfaces — that's the
whole point of keeping them behind `expect`/`actual`.

## Project structure

Restructured per
the [official recommended KMP structure](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html):
app entry points live in their own modules, separate from shared/library code,
and the library's Android target uses the newer `com.android.kotlin.multiplatform.library`
plugin (`androidLibrary {}` inside `kotlin {}`) instead of the older `androidTarget {}`

+ standalone `android {}` block — this is mandatory once a consumer is on AGP 9+.

```
kmp-location-tracker/
├── location-tracker/              ← the publishable library module (shared code)
│   └── src/
│       ├── commonMain/            ← public API: LocationTracker, TrackingConfig,
│       │                            TrackedLocation, LocationPermissionController
│       ├── androidMain/           ← foreground service + FusedLocationProviderClient
│       └── iosMain/               ← CLLocationManager + background modes
├── sample/
│   ├── shared/                    ← shared Compose UI demo code (commonMain + iOS targets)
│   └── androidApp/                ← thin Android app entry point (depends on
│                                     :location-tracker and :sample:shared)
└── .github/workflows/publish.yml  ← publishes :location-tracker to GitHub Packages
```

Why the sample is split this way: the recommended structure says entry-point
modules (the ones with `applicationId`/`MainActivity`/etc.) should stay separate
from any module producing shared code, so an `androidApp` can depend on
`:sample:shared` without `:sample:shared` needing to know anything about being
launched. If you only want the library, delete the whole `sample/` folder —
`location-tracker/` has zero dependency on it.

An analogous `iosApp` Xcode project would consume the XCFrameworks produced by
`:location-tracker` and `:sample:shared` directly (see "Update the iOS
integration" in the linked doc); it isn't a Gradle module and isn't included here.

## Setup (as a consumer)

### 1. Add the GitHub Packages repository

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/<owner>/kmp-location-tracker")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.token").orNull
            }
        }
    }
}
```

`~/.gradle/gradle.properties` (or CI secrets):

```properties
gpr.user=your-github-username
gpr.token=ghp_yourPersonalAccessTokenWithReadPackagesScope
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

No initialization step is needed on iOS.

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
            // send to your backend, update a map, etc.
        }
    }

    Button(onClick = {
        scope.launch {
            val foreground = permissionController.requestForeground()
            if (foreground == LocationPermissionStatus.Denied) return@launch

            val background = permissionController.requestBackground()
            // Decide whether foreground-only is acceptable for your use case
            // if the user declines the "Always"/"all the time" upgrade.

            tracker.start(
                TrackingConfig(
                    intervalMs = 10_000,
                    minUpdateDistanceMeters = 20f,
                    priority = LocationPriority.HIGH_ACCURACY,
                    notificationSmallIconResId = R.drawable.ic_notification, // Android only, REQUIRED
                ),
            )
        }
    }) {
        Text("Start tracking")
    }
}
```

## Required platform setup in the consuming app

### Android manifest

Permissions and the service declaration are already merged in automatically
from the library's manifest. You only need to supply a notification icon
resource via `TrackingConfig.notificationSmallIconResId` — Android requires a
real icon for the persistent foreground-service notification.

### iOS Info.plist

```xml

<key>UIBackgroundModes</key><array>
<string>location</string>
</array><key>NSLocationWhenInUseUsageDescription</key><string>We use your location to show your
position on the map.
</string><key>NSLocationAlwaysAndWhenInUseUsageDescription</key><string>We use your location in the
background to keep your trip/shift accurate.
</string>
```

## Notes on battery and platform limits

- Android throttles background location delivery outside of a foreground
  service — that's exactly why this library runs one. Expect the OS to still
  reduce delivery frequency under Doze/App Standby if the device is stationary.
- iOS won't wake a *suspended* app on a fixed timer. `allowsBackgroundLocationUpdates`
  keeps updates flowing while the device is moving; for scenarios where you also
  need updates while stationary (e.g. an idle driver), consider layering
  `startMonitoringSignificantLocationChanges()` as a low-power fallback — that
  API can also relaunch a terminated app, which plain `startUpdatingLocation()`
  cannot.
- Always check `TrackingState.PermissionDenied` / `LocationServicesDisabled`
  and prompt the user rather than silently failing — both platforms give users
  easy ways to revoke background access after the fact.

## Publishing a new version

```bash
git tag v0.2.0
git push origin v0.2.0
```

The `publish.yml` workflow builds and publishes automatically. To publish
locally instead:

```bash
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=ghp_yourTokenWithWritePackagesScope
./gradlew :location-tracker:publishAllPublicationsToGitHubPackagesRepository -PVERSION_NAME=0.2.0
```

## License

MIT — see [LICENSE](LICENSE).
