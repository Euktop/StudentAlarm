package com.euktop.studentalarm.service.alarm

import com.euktop.studentalarm.R
import com.euktop.studentalarm.utils.ResourceProvider
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AlarmTimeCalculator {

    fun getTimeToAlarmText(triggerTime: Long, resourceProvider: ResourceProvider): String {
        val diff = triggerTime - System.currentTimeMillis()

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days${resourceProvider.getString(R.string.days_short)}")
        if (hours > 0) parts.add("$hours${resourceProvider.getString(R.string.hours_short)}")
        if (minutes > 0) parts.add("$minutes${resourceProvider.getString(R.string.minutes_short)}")

        if (parts.isEmpty() && seconds > 0) {
            parts.add("$seconds${resourceProvider.getString(R.string.seconds_short)}")
        }

        return if (parts.isNotEmpty()) {
            resourceProvider.getString(R.string.alarm_in, parts.joinToString(" "))
        } else {
            resourceProvider.getString(R.string.alarm_in_few_seconds)
        }
    }
}