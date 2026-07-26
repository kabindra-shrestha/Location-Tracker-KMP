package com.kabindra.locationtracker.permission

import androidx.compose.runtime.Composable

/**
 * Requests and inspects location permission state.
 *
 * Both platforms require a two-step upgrade flow (foreground grant, THEN
 * background grant), so the interface mirrors that explicitly rather than
 * exposing a single "request everything" call.
 *
 * Obtain an instance via [rememberLocationPermissionController] — a @Composable
 * factory is used (rather than a plain top-level `expect fun`) because Android's
 * implementation needs to launch a system permission dialog and suspend until
 * the result comes back, which requires an Activity-scoped launcher registered
 * during composition.
 */
interface LocationPermissionController {

    /** Reads current status without prompting the user. */
    suspend fun status(): LocationPermissionStatus

    /**
     * Requests foreground ("while using the app" / "when in use") permission.
     * Suspends until the user responds to the system dialog.
     */
    suspend fun requestForeground(): LocationPermissionStatus

    /**
     * Requests background ("allow all the time" / "always") permission.
     *
     * Must be called *after* [requestForeground] has already been granted — both
     * platforms require the two-step foreground-then-background upgrade flow.
     */
    suspend fun requestBackground(): LocationPermissionStatus
}

@Composable
expect fun rememberLocationPermissionController(): LocationPermissionController
