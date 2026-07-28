package com.kabindra.locationtracker.session

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTrackingTimestamp(timestampMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
