package com.kabindra.locationtrackerkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.kabindra.locationtracker.session.LocationTrackingListener
import com.kabindra.locationtracker.session.LocationTrackingSession

fun MainViewController(developerMode: Boolean = false) = ComposeUIViewController {
    // Demo parity with Android: acknowledge events so debug status transitions can be inspected.
    // A production iOS host must replace this listener with its authenticated backend uploader.
    LocationTrackingSession.initialize(LocationTrackingListener { true }, developerMode)
    App()
}
