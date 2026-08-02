# Location Tracker Testing

## Automated tests

The project uses `kotlin.test` in `location-tracker/src/commonTest`; those tests execute on Android
host and iOS simulator. There is no DI or mocking framework because policy, schedule, and distance
logic are pure common Kotlin. Android device-test sources currently contain only the generated
placeholder; real platform delivery is covered by the device checklist below.

The shared policy and distance tests run on Android host and iOS simulator targets:

```sh
./gradlew :location-tracker:allTests
```

They cover the 50m comparison, master policy flag, active-session start lockout, check-in/out
eligibility, missing time-window fail-closed behavior, daytime/overnight boundaries, and the
start-inclusive/end-exclusive schedule rule.

Compile the sample Android app and iOS Kotlin target with:

```sh
./gradlew :androidApp:assembleDebug :location-tracker:compileKotlinIosArm64
```

Build the Swift iOS application with:

```sh
xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /private/tmp/location-tracker-ios-build build
```

## Device verification

Automated tests cannot validate operating-system location delivery. Verify on physical Android and
iOS devices:

- foreground, background, locked screen, and normal app relaunch;
- Android Recents swipe and foreground-service recreation;
- iOS background/suspension and Always permission behavior;
- `< 50m`, exactly `50m`, and `> 50m` interval decisions;
- listener failure/retry without advancing the last-synced location;
- time-range start/end, overnight range, check-in, check-out, and master-policy disable;
- expected limitation after Android force-stop and iOS force-quit.

The debug-only Tracked Locations viewer is the primary on-device evidence for accepted, filtered,
failed, and synced locations.
