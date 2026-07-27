package com.kabindra.locationtracker.schedule

/** The local wall-clock time used to evaluate a daily [ScheduleWindow]. */
data class LocalScheduleTime(val hour: Int, val minute: Int)

/** Reads the device's local time without requiring a host-side time dependency. */
expect fun currentLocalScheduleTime(): LocalScheduleTime
