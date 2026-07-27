package com.kabindra.locationtrackerkmp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kabindra.locationtracker.internal.LocationTrackerInit

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Required once before createLocationTracker()/rememberLocationPermissionController()
        // are used anywhere in the app.
        LocationTrackerInit.initialize(this)
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