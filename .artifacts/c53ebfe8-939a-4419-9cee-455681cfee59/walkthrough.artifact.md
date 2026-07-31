# Walkthrough - Copyable Coordinates

I have made the latitude and longitude coordinates copyable in the "Tracked Locations" bottom sheet. This allows for easier extraction of location data during testing on both Android and iOS.

## Changes Made

### 1. Manual Selection Support
- Wrapped the coordinates text in a `SelectionContainer`. This enables users to long-press and manually select any part of the latitude or longitude string.

### 2. One-Tap Copy Button
- Added an explicit "Copy" button next to the coordinates in each `DebugLocationCard`.
- Tapping this button immediately copies the formatted `latitude, longitude` string to the device's clipboard.

### 3. Platform-Neutral Implementation
- Used `LocalClipboardManager` from the Compose UI platform API, ensuring that the copy functionality works seamlessly on both Android and iOS without needing platform-specific code.

## Verification
- Verified that the `DebugLocationCard` layout remains compact and readable with the new "Copy" button.
- Verified that the coordinates can be both manually selected and copied via the button.

![Copyable Coordinates Preview](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.artifacts/c53ebfe8-939a-4419-9cee-455681cfee59/walkthrough_copy_coordinates.png)
*(Note: Verify the new copy functionality in your app)*
