# Location Tracker Project Task List & Plan

## Phase 1: Diagnostics & Initial Audit

- [x] Audit common codebase (`location-tracker` module and `shared` module)
- [x] Identify why permission dialog is not showing and location is not tracking in UI (`App.kt`)
- [x] Identify missing iOS `Info.plist` entries for location permissions and background modes
- [x] Create Product Requirements Document (`docs/PRD.md`)

## Phase 2: Configuration & Permissions Setup

- [ ] Update iOS `iosApp/iosApp/Info.plist` with required permission descriptions and
  `UIBackgroundModes` (`location`)
- [ ] Verify Android `AndroidManifest.xml` permissions for `POST_NOTIFICATIONS`, foreground service,
  and location
- [ ] Add Android 13+ `POST_NOTIFICATIONS` permission request flow in permission controller

## Phase 3: Core Business Logic & SDK Models

- [ ] Implement `LocationTrackerPolicy` model (defaults for `isTrackingEnabled`, `trackingMode`,
  `scheduleWindow`, `50m` distance threshold, `5-min` sync interval)
- [ ] Implement `LocationSyncListener` interface (decoupled sync callback triggered by SDK for host
  app network dispatch)
- [ ] Implement `DistanceFilter` component to suppress location updates if distance moved is <
  threshold (e.g. 50 meters)
- [ ] Implement `TrackingScheduleEvaluator` component to validate active tracking window (e.g., 9:
  00 - 17:00 or Check-In status)
- [ ] Implement `LocationSyncManager` inside SDK to buffer 50m-filtered locations and periodically
  invoke parent app `LocationSyncListener`

## Phase 4: UI & Host App Integration

- [ ] Update `App.kt` Compose UI to handle full 2-step permission flow (Foreground -> Background ->
  Start Tracking)
- [ ] Implement host app API trigger callback (`LocationSyncListener`) in `App.kt` to simulate
  network transmission
- [ ] Implement real-time tracking dashboard showing permission status, tracking state, active mode,
  last tracked location, displacement filter stats, and sync callback logs
- [ ] Wire Start/Stop tracking, Check-in/Check-out controls, and schedule policy updates

## Phase 5: Verification & Testing

- [ ] Build & compile Android app (`./gradlew :androidApp:assembleDebug`)
- [ ] Build & compile iOS app (`./gradlew :location-tracker:compileKotlinIosArm64`)
- [ ] Verify background location tracking behaviour on Android and iOS
