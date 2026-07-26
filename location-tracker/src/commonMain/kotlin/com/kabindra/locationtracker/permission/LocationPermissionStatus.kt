package com.kabindra.locationtracker.permission

enum class LocationPermissionStatus {
    /** No location permission granted at all. */
    Denied,

    /** Foreground-only access (Android "While using the app" / iOS "When In Use"). */
    GrantedForeground,

    /** Full background access (Android ACCESS_BACKGROUND_LOCATION / iOS "Always"). */
    GrantedAlways,
}
