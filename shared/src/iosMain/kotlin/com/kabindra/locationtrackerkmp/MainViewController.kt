package com.kabindra.locationtrackerkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.kabindra.locationtracker.LocationTrackingEngine
import com.kabindra.locationtracker.session.CheckInOutListener
import com.kabindra.locationtracker.session.LocationTrackingListener
import com.kabindra.locationtracker.session.LocationTrackingSession

/** Call from the iOS App initializer so the native coordinator is restored before Compose exists. */
fun initializeLocationTracking(developerMode: Boolean = false) {
    // Demo parity with Android: acknowledge events so debug status transitions can be inspected.
    // A production iOS host must replace this listener with its authenticated backend uploader.
    LocationTrackingEngine.initialize(
        listener = LocationTrackingListener { true },
        developerMode = developerMode,
        checkInOutListener = CheckInOutListener { action ->
            DemoTrackingBackend.performCheckInOut(action)
        },
    )
    DemoTrackingBackend.restorePersistedPolicy(LocationTrackingSession.state.value.activePolicy)
}

fun MainViewController(developerMode: Boolean = false) = ComposeUIViewController {
    // Keep this idempotent call for hosts that integrate only this factory.
    initializeLocationTracking(developerMode)
    App()
}
