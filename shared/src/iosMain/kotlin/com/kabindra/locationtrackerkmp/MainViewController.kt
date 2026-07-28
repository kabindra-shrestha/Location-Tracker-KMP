package com.kabindra.locationtrackerkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.kabindra.locationtracker.session.LocationTrackingSession

fun MainViewController() = ComposeUIViewController {
    // A host iOS app should register its durable backend uploader before this point.
    // With no uploader, events remain persisted until the host registers one.
    LocationTrackingSession.initialize(null)
    App()
}
