package com.euktop.studentalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.euktop.studentalarm.data.AlarmRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AlarmScheduler {
    const val ALARM_ID_EXTRA = "alarm_id"
    const val ALARM_DESCRIPTION_EXTRA = "alarm_description"
    suspend fun rescheduleAllAlarms(context: Context, repository: AlarmRepository) {
        val alarmsFlow = repository.getAllAlarms()
        val alarms = alarmsFlow.first()
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleAlarm(context, alarm)
            }
        }
    }
    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = android.net.Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_description", alarm.description)
            action = "ALARM_${alarm.id}"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = getNextAlarmTime(alarm)
        val triggerTime = calendar.timeInMillis
        val currentTime = System.currentTimeMillis()

        // Показываем Toast с временем до срабатывания
        showTimeToAlarmToast(context, currentTime, triggerTime)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, AlarmActivity::class.java).apply {
                    putExtra("alarm_id", alarm.id)
                    putExtra("alarm_description", alarm.description)
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    alarm.id.toInt() + 1000,
                    showIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun showTimeToAlarmToast(context: Context, currentTime: Long, triggerTime: Long) {
        val diff = triggerTime - currentTime

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days ${pluralize(days, "день", "дня", "дней")}")
        if (hours > 0) parts.add("$hours ${pluralize(hours, "час", "часа", "часов")}")
        if (minutes > 0) parts.add("$minutes ${pluralize(minutes, "минута", "минуты", "минут")}")

        if (parts.isEmpty()) {
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            parts.add("$seconds ${pluralize(seconds, "секунда", "секунды", "секунд")}")
        }

        val message = "Будильник сработает через ${parts.joinToString(", ")}."
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun pluralize(n: Long, one: String, few: String, many: String): String {
        val n10 = n % 10
        val n100 = n % 100

        return when {
            n10 == 1L && n100 != 11L -> one
            n10 in 2..4 && n100 !in 12..14 -> few
            else -> many
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            action = "ALARM_${alarmId}"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun getNextAlarmTime(alarm: Alarm): Calendar {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (alarm.daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val daysUntilNext = findNextDay(alarm.daysOfWeek, today)
            if (daysUntilNext > 0) calendar.add(Calendar.DAY_OF_YEAR, daysUntilNext)
        }

        return calendar
    }

    private fun findNextDay(alarmDays: List<Int>, currentAndroidDay: Int): Int {
        val currentDayInOurSystem = when (currentAndroidDay) {
            Calendar.SUNDAY -> 7
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }

        val sortedDays = alarmDays.sorted()
        for (day in sortedDays) {
            if (day > currentDayInOurSystem) return day - currentDayInOurSystem
        }
        return (sortedDays.first() + 7) - currentDayInOurSystem
    }
}