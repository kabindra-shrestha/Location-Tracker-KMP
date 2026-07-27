package com.kabindra.locationtracker.schedule

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate

@OptIn(ExperimentalForeignApi::class)
actual fun currentLocalScheduleTime(): LocalScheduleTime {
    val components = NSCalendar.currentCalendar.components(
        unitFlags = NSCalendarUnitHour or NSCalendarUnitMinute,
        fromDate = NSDate(),
    )
    return LocalScheduleTime(hour = components.hour.toInt(), minute = components.minute.toInt())
}
