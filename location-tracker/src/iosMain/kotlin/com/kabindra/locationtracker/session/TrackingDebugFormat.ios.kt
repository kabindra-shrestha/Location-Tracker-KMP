package com.kabindra.locationtracker.session

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle

@OptIn(ExperimentalForeignApi::class)
actual fun formatTrackingTimestamp(timestampMs: Long): String = NSDateFormatter().apply {
    dateStyle = NSDateFormatterMediumStyle
    timeStyle = NSDateFormatterMediumStyle
}.stringFromDate(
    // NSDate's Kotlin/Native constructor is measured from 2001-01-01, not Unix epoch.
    NSDate(timeIntervalSinceReferenceDate = timestampMs / 1000.0 - 978_307_200.0),
)
