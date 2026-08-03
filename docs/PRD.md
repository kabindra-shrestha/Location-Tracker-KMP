# Product Requirements: KMP Background Location Tracker

## Goal

Provide a KMP library that records locations reliably during normal Android/iOS background use,
applies backend-controlled eligibility rules, and hands typed delivery events to the host app. The
SDK must not implement HTTP or make policy decisions from UI state.

## Product requirements

### Collection and lifecycle

- Capture latitude, longitude, timestamp, accuracy, and available speed, bearing, and altitude.
- Use a started Android location foreground service that is independent of the activity binding and
  does not stop when the task is removed from Recents.
- Use an iOS app-lifecycle `CLLocationManager` configured for Always permission, background updates,
  and no automatic pausing.
- Restore persisted session state and the host upload callback before Compose renders.
- Treat Android force-stop and iOS user force-quit as unavoidable limits; reopening is required.

### Backend policy

The host supplies a complete `LocationTrackerPolicy` through
`LocationTrackingEngine.updatePolicy(policy)`. The policy is persisted locally and is the sole
source of truth for tracking eligibility.

| Mode | Start rule | Stop rule |
| --- | --- | --- |
| `TIME_RANGE` | Backend master flag is enabled and local time is inside `scheduleWindow`. | The window ends, the master flag is disabled, or policy is replaced with an ineligible value. |
| `CHECK_IN_OUT` | Backend master flag is enabled and the returned policy says `isCheckedIn = true`. | The returned policy says checked out or master flag is disabled. |

Time windows are start-inclusive/end-exclusive: `09:00–17:00` stops at 17:00. Overnight windows
are supported. A `TIME_RANGE` policy with no window is malformed and fails closed.

### Synchronization

- Default raw collection: 30 seconds and 20m platform displacement.
- Default backend decision: every 5 minutes and at least 50m from the last **successfully synced**
  location.
- Send `Started(firstLocation)` immediately; send `Stopped(lastKnownLocation, reason)` immediately.
- At each interval, mark movement `< 50m` as `FILTERED`; queue `>= 50m` as `LocationUpdated`.
- Retain failures and retry them without advancing the last-synced reference.
- The host returns `true` only after its API acknowledges the typed event.

### Permissions and indicators

- Android requires fine/coarse, background location, and a persistent foreground-service
  notification. Notification permission is requested for user visibility but does not block the
  tracking lifecycle.
- iOS requires When In Use then Always location authorization and the `location` background mode.
  Its background-location indicator is used; notification permission is optional.

### Host controls

- Do not expose generic Start Tracking or Stop Tracking controls.
- In `TIME_RANGE`, the host displays the current schedule and the engine starts/stops from backend
  policy and time boundaries after permission is available.
- In `CHECK_IN_OUT`, the host displays Check In while `isCheckedIn` is false and Check Out while it
  is true. Each action is sent through `CheckInOutListener`; only its returned policy changes the
  tracking session.
- Location permission is a separate prerequisite action, not a tracking-start action.

## Acceptance criteria

- The host has no generic Start/Stop controls. Time Range is automatic; Check-In/Out is the only
  manual tracking action.
- Updating policy stops an active session when the master flag, schedule, or check-in state becomes
  ineligible.
- Android and iOS share the same 50m, status, retry, and policy evaluation logic.
- Debug builds show persisted engine state and all current-session location records.
- Common tests cover policy gates, schedule boundaries, overnight schedules, and displacement rules.
- Device verification covers backgrounding, lock screen, Android Recents removal, iOS suspension,
  retry, and force-stop/force-quit limitation messaging.
