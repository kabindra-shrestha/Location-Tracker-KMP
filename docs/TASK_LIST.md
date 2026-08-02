# Location Tracker Project Task List & Plan

## Phase 1: Diagnostics & Initial Audit

- [x] Audit common codebase (`location-tracker` module and `shared` module)
- [x] Identify why permission dialog is not showing and location is not tracking in UI (`App.kt`)
- [x] Identify missing iOS `Info.plist` entries for location permissions and background modes
- [x] Create Product Requirements Document (`docs/PRD.md`)

## Phase 2: Configuration & Permissions Setup

- [x] Update iOS `iosApp/iosApp/Info.plist` with required permission descriptions and
  `UIBackgroundModes` (`location`)
- [x] Verify Android `AndroidManifest.xml` permissions for `POST_NOTIFICATIONS`, foreground service,
  and location
- [x] Add Android 13+ `POST_NOTIFICATIONS` permission request flow in permission controller

## Phase 3: Core Business Logic & SDK Models

- [x] Implement `LocationTrackerPolicy` model (defaults for `isTrackingEnabled`, `trackingMode`,
  `scheduleWindow`, `50m` distance threshold, `5-min` sync interval)
- [x] Implement `LocationTrackingListener` interface (typed, decoupled host callback for backend
  delivery)
- [x] Implement `DistanceFilter` component to suppress location updates if distance moved is <
  threshold (e.g. 50 meters)
- [x] Implement `ScheduleEvaluator` component to validate policy eligibility, including overnight
  windows and start-inclusive/end-exclusive boundaries
- [x] Implement persistent `LocationTrackingSession` / `LocationTrackingEngine` to queue events,
  apply the interval-based 50m decision, and invoke `LocationTrackingListener`

## Phase 4: UI & Host App Integration

- [x] Update `App.kt` Compose UI to request foreground/background location and render persisted
  engine state; notification permission is non-blocking
- [x] Implement host app API trigger callback (`LocationTrackingListener`) in `App.kt` to simulate
  network transmission
- [x] Implement real-time tracking dashboard showing permission status, tracking state, active mode,
  last tracked location, displacement filter stats, and sync callback logs
- [x] Wire Start/Stop tracking, Check-in/Check-out controls, and schedule policy updates

## Phase 5: Verification & Testing

- [x] Build & compile Android app (`./gradlew :androidApp:assembleDebug`)
- [x] Build & compile iOS app (`./gradlew :location-tracker:compileKotlinIosArm64`)
- [x] Add common policy tests for active-session lockout, schedule boundaries, overnight windows,
  check-in/out, master disable, and missing-window fail-closed behavior
- [ ] Verify background location tracking behaviour on Android and iOS physical devices
