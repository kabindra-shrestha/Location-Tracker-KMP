# Implementation Plan - Copyable Coordinates

Make the latitude and longitude in the `DebugLocationCard` copyable on both Android and iOS to facilitate testing.

## Proposed Changes

### [shared](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/shared)

#### [MODIFY] [App.kt](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/shared/src/commonMain/kotlin/com/kabindra/locationtrackerkmp/App.kt)
- Wrap the coordinates text in `SelectionContainer` to allow users to manually select and copy them.
- Add an explicit "Copy Coordinates" icon button next to the coordinates for a better "one-tap" experience during testing.
- Use `LocalClipboardManager` to handle the copy action in a platform-neutral way.

## Verification Plan

### Manual Verification
- Deploy to an Android device/emulator.
- Open the "Tracked Locations" bottom sheet.
- Verify that you can long-press the coordinates to select them.
- Verify that tapping the "Copy" icon button copies the coordinates to the clipboard and provides visual feedback (e.g., a simple snackbar or icon change).
- Verify the same on an iOS device/simulator.
