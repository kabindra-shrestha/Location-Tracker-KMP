package com.kabindra.locationtrackerkmp

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kabindra.locationtracker.internal.LocationTrackerInit
import com.kabindra.locationtracker.session.LocationTrackingListener
import com.kabindra.locationtracker.session.LocationTrackingSession

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Required once before createLocationTracker()/rememberLocationPermissionController()
        // are used anywhere in the app.
        LocationTrackerInit.initialize(this)
        // Replace this sample listener with the host application's authenticated API call.
        // It is registered before any foreground service restoration can deliver an event.
        LocationTrackingSession.initialize(LocationTrackingListener { event ->
            android.util.Log.i("LocationTracker", "Backend event: $event")
            true
        }, developerMode = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
