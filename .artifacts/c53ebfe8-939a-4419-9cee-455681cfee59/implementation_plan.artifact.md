# Implementation Plan - Fix Unclickable Buttons in Bottom Sheet (iOS)

Improve the layout and gesture handling of the `TrackedLocationsBottomSheet` to ensure that filters and copy buttons remain clickable when the sheet is expanded to fullscreen, particularly on iOS.

## Proposed Changes

### [shared](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/shared)

#### [MODIFY] [App.kt](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/shared/src/commonMain/kotlin/com/kabindra/locationtrackerkmp/App.kt)
- **Add `statusBarsPadding()` to Bottom Sheet Content**:
    - Ensure the `Column` inside `ModalBottomSheet` respects the safe area. This prevents the title and filter chips from being obscured by the status bar/notch on iOS when fullscreen.
- **Dynamic Content Height**:
    - Replace the fixed `heightIn(max = 560.dp)` on the `LazyColumn` with `Modifier.weight(1f)`.
    - This allows the list to occupy all available space when the sheet is expanded, reducing gesture conflicts with the background container.
- **Explicit `navigationBarsPadding()`**:
    - Add padding at the bottom of the `Column` to ensure the last items in the list aren't under the iOS home indicator.
- **Refine `ExpressiveFilterItem` and `DebugLocationCard`**:
    - Ensure `clickable` and `TextButton` components are configured to avoid gesture competition where possible.

## Verification Plan

### Manual Verification
1.  **iOS Test**:
    - Open "Tracked Locations".
    - Drag/scroll the bottom sheet to fullscreen.
    - Verify that the **Filter Chips** at the top are responsive to taps.
    - Verify that the **Copy Button** in each card remains clickable.
2.  **Android Test**:
    - Verify that the layout remains consistent and buttons are clickable on Android.
3.  **Notch/Status Bar Check**:
    - Ensure content doesn't bleed into the status bar area when expanded.
