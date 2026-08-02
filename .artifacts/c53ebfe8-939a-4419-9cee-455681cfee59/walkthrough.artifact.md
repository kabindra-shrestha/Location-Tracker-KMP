# Walkthrough - Unique Keys for Tracked Locations

I have resolved the `IllegalArgumentException` in the "Tracked Locations" bottom sheet by ensuring each item in the list has a unique identifier.

## The Problem
The `LazyColumn` used a key derived from the event type and timestamp. If multiple events occurred within the same millisecond (e.g., a "LOCATION" update and a "SYNCED" status update), Compose detected a duplicate key and crashed.

## The Solution
I introduced a unique `id` field for every location event recorded by the library.

### 1. Model & Logic Changes
- **`TrackedLocationDebugEntry`**: Added a mandatory `id: String` field to the data class.
- **Unique Generation**: In `LocationTrackingSession`, IDs are now generated using the event kind, timestamp, and a random numeric suffix to guarantee uniqueness even within the same millisecond.

### 2. Durable Persistence
- **Serialization**: Updated the `TrackingSessionStore` for both **Android** and **iOS** to correctly encode and decode the new `id` field. This ensures that the unique identifiers are preserved across app restarts and process kills.

### 3. UI Alignment
- **`App.kt`**: Updated the `LazyColumn` in the bottom sheet to use the new `it.id` as its unique key.

## Verification
- **Android**: Verified that high-frequency updates no longer trigger a crash.
- **iOS**: Verified that the app remains stable during rapid location transitions.
- **Persistence**: Confirmed that session logs are correctly restored with their unique IDs after an app restart.

![Crash Resolved](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.artifacts/c53ebfe8-939a-4419-9cee-455681cfee59/walkthrough_crash_fix.png)
*(Note: The list now handles overlapping events gracefully)*
