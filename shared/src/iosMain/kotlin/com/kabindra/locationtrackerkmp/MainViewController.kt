package com.kabindra.locationtrackerkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.kabindra.locationtracker.LocationTrackingEngine
import com.kabindra.locationtracker.session.LocationTrackingListener

/** Call from the iOS App initializer so the native coordinator is restored before Compose exists. */
fun initializeLocationTracking(developerMode: Boolean = false) {
    // Demo parity with Android: acknowledge events so debug status transitions can be inspected.
    // A production iOS host must replace this listener with its authenticated backend uploader.
    LocationTrackingEngine.initialize(LocationTrackingListener { true }, developerMode)
}

fun MainViewController(developerMode: Boolean = false) = ComposeUIViewController {
    // Keep this idempotent call for hosts that integrate only this factory.
    initializeLocationTracking(developerMode)
    App()
}
