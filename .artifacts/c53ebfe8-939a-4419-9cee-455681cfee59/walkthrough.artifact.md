# Walkthrough - One-Shot Geofence Check

I have implemented the "Get Current Location" API and integrated a Geofence verification tool into the sample application. This feature allows for instant proximity checks against a target office location without requiring the background tracking service to be active.

## Key Changes

### 1. SDK: One-Shot Location API
- **Unified Entry Point**: Added `LocationTrackingEngine.getCurrentLocation()` which provides a simple `suspend` function to retrieve the device's coordinates.
- **Cross-Platform**: Leverages the library's internal `CompassCurrentLocation` bridge, ensuring consistent behavior and automatic permission handling on both Android and iOS.

### 2. SDK: Geofence Logic
- **Policy Expansion**: Updated `LocationTrackerPolicy` to include `officeLatitude`, `officeLongitude`, and `geofenceRadiusMeters`.
- **Utility Method**: Added `DistanceFilter.isWithinRadius()` to perform Haversine distance calculations and boundary checks in a single call.

### 3. Sample App: Proximity Dashboard
- **Interactive Geofence Tool**: Added a new "One-Shot Geofence Check" section to the main dashboard.
- **Distance Calculation**: Real-time display of the distance between the device and the simulated "Office" location.
- **Expressive Status**: Visual feedback showing "INSIDE RADIUS" (Green) or "OUTSIDE RADIUS" (Red) based on the active backend policy.
- **Testing Utilities**:
    - "Set Current as Office": Allows testers to quickly designate their current spot as the target for proximity checks.
    - Configurable Radius: Added quick-toggle buttons (50m, 100m, 500m) to test different boundary constraints.

## Verification Results

### Automated Tests
- **Logic Validation**: Ran `:location-tracker:allTests`. All 15 tests passed, including the new `distanceFilterCorrectlyEvaluatesRadiusCheck` suite.

### Manual Verification
- **One-Shot retrieval**: Verified that clicking "Check Distance" retrieves coordinates instantly without triggering a foreground service notification.
- **Geofence Accuracy**: Confirmed that the "Status" label correctly reflects the distance vs. radius rule.
- **Policy Persistence**: Verified that changing the radius in the backend controls immediately updates the status of the one-shot check.

![Geofence Tool](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.artifacts/c53ebfe8-939a-4419-9cee-455681cfee59/geofence_screenshot.png)
*(Note: Use the "Set Current as Office" button to test the geofence logic at your current physical location)*
