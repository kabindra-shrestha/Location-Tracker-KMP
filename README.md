# Location Tracker KMP

A Kotlin Multiplatform (KMP) project containing a persistent Android/iOS location-tracking library
and a Compose Multiplatform sample app.

## Overview

This repository consists of two main parts:
1.  **[location-tracker](location-tracker/)**: A publishable KMP library for continuous background location tracking on Android and iOS.
2.  **Sample Application**: A Compose Multiplatform implementation demonstrating the library's features, including a real-time dashboard and developer tools.

## Implementation App (Sample)

The sample app demonstrates how to integrate the library into a modern Compose Multiplatform project.

### Features

- **Persistent engine**: The app initializes `LocationTrackingEngine` before Compose, allowing a
  recreated Android service or iOS coordinator to restore durable state and host delivery.
- **Policy-mode simulator**: The controls in the sample stand in for backend responses, not a real
  backend. `TIME_RANGE` starts/stops automatically at its configured boundaries. `CHECK_IN_OUT`
  shows Check In/Check Out actions that call the host callback and apply its returned policy.
  Production hosts fetch a complete `LocationTrackerPolicy` and call `updatePolicy(policy)`.
- **No generic tracking controls**: The sample deliberately has no Start Tracking or Stop Tracking
  buttons. Enabling background-location permission is separate from deciding whether policy permits
  collection.
- **Background permissions**: The sample requests foreground and Always/background location. Android
  notification permission is requested for visibility but does not block tracking.
- **Developer Tools**:
    - **Persistent Session Logs**: View every location fix recorded in the current session.
    - **Expressive Filtering**: Filter logs by status (Synced, Pending, Filtered, Failed) with Material 3 Expressive UI.
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

Background reliability must be verified on physical devices. Android force-stop and iOS user
force-quit intentionally require the user to reopen the application.

## Library Documentation

For detailed information on how to use the `location-tracker` library in your own project, including setup, APIs, and platform-specific notes, see:

👉 **[Location Tracker Library README](location-tracker/README.md)**

## Technical Deep Dive

For information on the internal tracking flows, distance filtering logic, and platform-specific background implementations, see:

- **[Location Tracking Flow](docs/LOCATION_TRACKING_FLOW.md)**
- **[Internal Technical Docs](location-tracker/TECHNICAL_DOCS.md)**
- **[Testing Guide](docs/TESTING.md)**

## CI/CD & Publishing

This project is configured to publish the `:location-tracker` library to both **GitHub Packages** and **GitLab Package Registry**.

### GitHub Actions
The library is automatically tested and built on every push to `main`. When a new tag starting with `v` is pushed, it is published to GitHub Packages.

### GitLab CI
A `.gitlab-ci.yml` is provided for GitLab environments, supporting automated tests and manual release publishing to the GitLab Package Registry.

## Consumption

### From GitHub Packages
`settings.gradle.kts`:
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/OWNER/REPO")
        credentials {
            username = "GITHUB_USER"
            password = "GITHUB_TOKEN"
        }
    }
}
```

### From GitLab Package Registry
`settings.gradle.kts`:
```kotlin
repositories {
    maven {
        url = uri("https://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven")
        credentials {
            username = "Private-Token"
            password = "YOUR_PERSONAL_ACCESS_TOKEN"
        }
    }
}
```

## License
MIT
