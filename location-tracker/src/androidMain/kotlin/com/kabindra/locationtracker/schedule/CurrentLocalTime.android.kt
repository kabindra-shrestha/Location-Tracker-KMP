package com.kabindra.locationtracker.schedule

import java.util.Calendar

actual fun currentLocalScheduleTime(): LocalScheduleTime = Calendar.getInstance().let { calendar ->
    LocalScheduleTime(
        hour = calendar.get(Calendar.HOUR_OF_DAY),
        minute = calendar.get(Calendar.MINUTE),
    )
}
