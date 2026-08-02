# Implementation Plan - Fix Duplicate Keys in Tracked Locations List

Address the `IllegalArgumentException` caused by duplicate keys in the `LazyColumn` of the `TrackedLocationsBottomSheet`. This occurs when multiple location events share the same timestamp and kind.

## User Review Required

> [!IMPORTANT]
> **Schema Change in Persistence**
> I am adding a unique `id` field to the `TrackedLocationDebugEntry` data class. I will update the serialization logic in `TrackingSessionStore` to handle this new field. Existing persisted sessions might fail to decode individual debug entries (falling back to null/empty) until a new session is started, but the overall app state will remain stable.

## Proposed Changes

### [location-tracker](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker)

#### [MODIFY] [TrackingSession.kt](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker/src/commonMain/kotlin/com/kabindra/locationtracker/session/TrackingSession.kt)
- Add `val id: String` to `TrackedLocationDebugEntry`.
- Update `markStarted`, `onLocation`, `stop`, and `evaluateLatestLocationForSync` to generate unique IDs for each debug entry (e.g., `"${eventKind.name}-${timestampMs}-${randomSuffix}"`).

#### [MODIFY] [TrackingSessionStore.android.kt](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker/src/androidMain/kotlin/com/kabindra/locationtracker/session/TrackingSessionStore.android.kt) & [TrackingSessionStore.ios.kt](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker/src/iosMain/kotlin/com/kabindra/locationtracker/session/TrackingSessionStore.ios.kt)
- Update `encode` and `toDebugEntry` to include the `id` field.

### [shared](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/shared)

#### [MODIFY] [App.kt](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/shared/src/commonMain/kotlin/com/kabindra/locationtrackerkmp/App.kt)
- Update the `LazyColumn` key to use the new `id` field: `key = { it.id }`.

## Verification Plan

### Manual Verification
- Deploy to an Android device.
- Start tracking and generate several location fixes.
- Open the "Tracked Locations" bottom sheet and verify it no longer crashes.
- Verify that filtering and scrolling work smoothly with the new unique keys.
- Repeat for iOS.
