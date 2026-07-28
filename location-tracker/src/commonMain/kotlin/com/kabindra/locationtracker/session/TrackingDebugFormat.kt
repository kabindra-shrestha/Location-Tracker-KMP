package com.kabindra.locationtracker.session

/** Formats a location timestamp in the device's local date and time for the debug viewer. */
expect fun formatTrackingTimestamp(timestampMs: Long): String
