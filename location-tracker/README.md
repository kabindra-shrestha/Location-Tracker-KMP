# Location Tracker Library

A Kotlin Multiplatform library for persistent, policy-controlled location tracking on Android and
iOS. The host owns authentication and HTTP; the library owns platform collection, durable session
state, policy evaluation, filtering, and event retry.

## What it does

- Android collection is owned by a started `LocationForegroundService`.
- iOS collection is owned by a singleton `CLLocationManager` coordinator.
- The shared engine persists the active policy/configuration, session id, latest raw fix, last
  successfully synced fix, next sync deadline, retry queue, and debug history.
- The default collection profile is a 30-second request interval with a 20m platform displacement.
  Ongoing backend delivery is policy-controlled: 5-minute interval and 50m threshold by default.
- `Started` and `Stopped` locations are immediate; only `LocationUpdated` uses the periodic 50m
  decision.

Normal backgrounding, screen lock, and Android Recents removal are supported. Android force-stop
and iOS user force-quit are operating-system boundaries and require reopening the app.

## Host initialization

Initialize before creating any Compose UI or restoring a platform collector. Register the uploader
first, because a restored retry event may be delivered immediately.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocationTrackerInit.initialize(this)
        LocationTrackingEngine.initialize(
            listener = LocationTrackingListener { event ->
                // Call the host application's authenticated API.
                // Return true only after the backend accepts this exact event.
                trackingApi.upload(event)
            },
            checkInOutListener = CheckInOutListener { action ->
                // Return the updated, complete backend policy.
                attendanceApi.perform(action)
            },
            developerMode = BuildConfig.DEBUG,
        )
    }
}
```

On iOS, make the equivalent engine initialization from the `App` initializer, before constructing
the Compose view controller. The sample exposes `initializeLocationTracking` for that purpose.

## Applying backend policy

The backend is the only policy source. Supply a complete `LocationTrackerPolicy` whenever it is
fetched or changed:

```kotlin
LocationTrackingEngine.updatePolicy(
    LocationTrackerPolicy(
        isTrackingEnabled = true,
        trackingMode = TrackingMode.TIME_RANGE,
        scheduleWindow = ScheduleWindow(9, 0, 17, 0),
        minDistanceThresholdMeters = 50f,
        syncIntervalMinutes = 5,
    ),
)
```

`TIME_RANGE` is start-inclusive/end-exclusive: `09:00–17:00` runs at 09:00 and stops at 17:00.
Overnight windows work. A `TIME_RANGE` policy with no `scheduleWindow` fails closed and cannot
start tracking. In `CHECK_IN_OUT`, call `requestCheckIn()` / `requestCheckOut()`; the host callback
must return the authoritative updated policy.

After background location permission is granted, use controls that reflect the policy mode rather
than generic Start/Stop controls:

- `TIME_RANGE`: call `updatePolicy(policy)` after every backend response. The engine starts and
  stops automatically at the eligible schedule boundaries.
- `CHECK_IN_OUT`: show Check In and Check Out actions. They call `requestCheckIn()` and
  `requestCheckOut()` respectively; the `CheckInOutListener` must return the updated backend policy
  before the engine starts or stops collection.

The Compose screen may request permission and render `LocationTrackingSession.state`, but it must
not own syncing or issue direct start/stop commands for either mode.

## Backend events and statuses

The host receives `LocationTrackingEvent.Started`, `LocationUpdated`, and `Stopped` events. Every
location contains latitude, longitude, timestamp, and optional accuracy/speed/bearing/altitude.

| Status | Meaning |
| --- | --- |
| `PENDING` | Awaiting a sync decision or backend acknowledgement. |
| `SYNCED` | The host confirmed delivery; this becomes the new distance reference. |
| `FILTERED` | At the sync interval, movement was `< 50m`; no upload occurred. |
| `FAILED` | Upload failed or was rejected; the durable event stays queued for retry. |

Exactly 50m is delivered. Only a successful host response advances the 50m reference.

## Platform setup

### Android

The library manifest merges fine/coarse/background location, foreground-service location, and
notification permissions. The host must supply a valid notification small icon. A foreground and
background location grant is required for reliable automatic/background starts.

### iOS

`Info.plist` must declare `UIBackgroundModes` with `location`, plus When In Use and Always location
usage descriptions. The user must grant **Always** location authorization for background delivery.
Notification permission is optional on iOS.

## More documentation

- [Production lifecycle and host flow](../docs/LOCATION_TRACKING_FLOW.md)
- [Internal architecture](TECHNICAL_DOCS.md)
- [Test commands and device scenarios](../docs/TESTING.md)
