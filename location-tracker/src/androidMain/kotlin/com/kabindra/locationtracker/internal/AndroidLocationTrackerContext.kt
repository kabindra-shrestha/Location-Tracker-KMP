package com.kabindra.locationtracker.internal

import android.content.Context

/**
 * Holds the application [Context] so [com.kabindra.locationtracker.createLocationTracker]
 * can stay a no-arg factory in commonMain (an `expect fun` can't take a Context param
 * without leaking an Android type into common code).
 *
 * Call [LocationTrackerInit.initialize] once, e.g. in `Application.onCreate()`:
 *
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         LocationTrackerInit.initialize(this)
 *     }
 * }
 * ```
 */
internal object AndroidLocationTrackerContext {

    private var appContext: Context? = null

    fun set(context: Context) {
        appContext = context.applicationContext
    }

    fun require(): Context = checkNotNull(appContext) {
        "LocationTrackerInit.initialize(context) must be called before using " +
                "LocationTracker or LocationPermissionController on Android, " +
                "typically from Application.onCreate()."
    }
}

/** Public initializer — see [AndroidLocationTrackerContext] for usage. */
object LocationTrackerInit {
    fun initialize(context: Context) {
        AndroidLocationTrackerContext.set(context)
    }
}
