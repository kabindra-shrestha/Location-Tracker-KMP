# Location Tracker KMP

A Kotlin Multiplatform (KMP) project providing a robust location tracking library and a comprehensive sample application.

## Overview

This repository consists of two main parts:
1.  **[location-tracker](location-tracker/)**: A publishable KMP library for continuous background location tracking on Android and iOS.
2.  **Sample Application**: A Compose Multiplatform implementation demonstrating the library's features, including a real-time dashboard and developer tools.

## Implementation App (Sample)

The sample app demonstrates how to integrate the library into a modern Compose Multiplatform project.

### Features
- **Real-time Dashboard**: Monitor tracking engine state, last known location, and sync status.
- **Dynamic Policy Controls**: Simulate backend overrides for tracking modes (Time Range vs. Check-in), distance filters, and sync intervals.
- **Multi-stage Permissions**: Interactive flow for Foreground, Notification, and Background location permissions.
- **Developer Tools**:
    - **Persistent Session Logs**: View every location fix recorded in the current session.
    - **Expressive Filtering**: Filter logs by status (Synced, Pending, Filtered, Failed) with Material 3 Expressive UI.
    - **Rich Tooltips**: Detailed explanations for library behavior directly in the UI.
    - **Testing Utilities**: Copy coordinates to clipboard for easy verification.

### Project Structure
- `:location-tracker`: The core tracking library.
- `:shared`: Common Compose UI and business logic for the sample app.
- `:androidApp`: Android-specific entry point and configuration.
- `iosApp/`: Xcode project for the iOS entry point.

## Getting Started

### Prerequisites
- Android Studio Ladybug+ or IntelliJ IDEA.
- Xcode (for iOS development).
- Android device/emulator with Google Play Services.

### Running the App
- **Android**: Select the `androidApp` configuration and click Run.
- **iOS**: Open `iosApp/iosApp.xcworkspace` in Xcode and Run on a simulator or device.

## Library Documentation

For detailed information on how to use the `location-tracker` library in your own project, including setup, APIs, and platform-specific notes, see:

👉 **[Location Tracker Library README](location-tracker/README.md)**

## Technical Deep Dive

For information on the internal tracking flows, distance filtering logic, and platform-specific background implementations, see:

- **[Location Tracking Flow](docs/LOCATION_TRACKING_FLOW.md)**
- **[Internal Technical Docs](location-tracker/TECHNICAL_DOCS.md)**

## License
MIT
