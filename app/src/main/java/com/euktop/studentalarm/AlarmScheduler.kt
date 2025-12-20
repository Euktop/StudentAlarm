package com.euktop.studentalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.euktop.studentalarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AlarmScheduler {
    const val ALARM_ID_EXTRA = "alarm_id"
    const val ALARM_DESCRIPTION_EXTRA = "alarm_description"

    suspend fun rescheduleAllAlarms(context: Context, repository: AlarmRepository) {
        if (!PermissionManager.hasAllAlarmPermissions(context)) {
            return
        }

        val alarmsFlow = repository.getAllAlarms()
        val alarms = alarmsFlow.first()
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleAlarm(context, alarm, showToast = false) // Не показывать Toast
            }
        }
    }

    // Добавляем параметр showToast, по умолчанию false
    fun scheduleAlarm(context: Context, alarm: Alarm, showToast: Boolean = false) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        if (!PermissionManager.hasAllAlarmPermissions(context)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                PermissionManager.requestScheduleExactAlarmPermission(context as android.app.Activity)
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

        // Сохраняем время следующего срабатывания
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as AlarmApplication
            app.alarmRepository.updateAlarm(alarm.copy(nextTriggerTime = triggerTime))
        }

        val currentTime = System.currentTimeMillis()

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

            // Показываем Toast ТОЛЬКО если явно запрошено (при ручном создании/изменении)
            if (showToast) {
                Handler(Looper.getMainLooper()).post {
                    showTimeToAlarmToast(context, currentTime, triggerTime)
                }
            }

        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    suspend fun checkMissedAlarms(context: Context, repository: AlarmRepository) {
        val currentTime = System.currentTimeMillis()
        val app = context.applicationContext as AlarmApplication

        val missedAlarms = app.alarmRepository.getMissedAlarms(currentTime)

        if (missedAlarms.isNotEmpty()) {
            // Обрабатываем каждый пропущенный будильник
            missedAlarms.forEach { alarm ->
                if (alarm.daysOfWeek.isEmpty()) {
                    // Неповторяющийся: отключаем
                    app.alarmRepository.updateAlarm(alarm.copy(isEnabled = false, nextTriggerTime = 0L))
                    cancelAlarm(context, alarm.id)
                } else {
                    // Повторяющийся: перепланируем на следующее время (без Toast)
                    scheduleAlarm(context, alarm, showToast = false)
                }
            }

            // Запускаем активность, чтобы показать пользователю
            if (!AlarmActivity.isAlarmActive()) {
                val alarmIds = missedAlarms.map { it.id }.toLongArray()
                AlarmActivity.startAlarm(context, alarmIds)
            }
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

        // Обнуляем время следующего срабатывания в базе
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as AlarmApplication
            val alarm = app.alarmRepository.getAlarmById(alarmId)
            alarm?.let {
                app.alarmRepository.updateAlarm(it.copy(nextTriggerTime = 0L))
            }
        }
    }

    private fun getNextAlarmTime(alarm: Alarm): Calendar {
        val now = Calendar.getInstance()
        val currentTime = now.timeInMillis
        val today = getDayOfWeekInOurSystem(now.get(Calendar.DAY_OF_WEEK))
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.daysOfWeek.isEmpty()) {
            // Неповторяющийся будильник
            if (calendar.timeInMillis <= currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar
        }

        // Повторяющийся будильник
        val sortedDays = alarm.daysOfWeek.sorted()

        // Проверяем, подходит ли сегодняшний день
        if (sortedDays.contains(today)) {
            // Если время сегодня еще не наступило
            if (calendar.timeInMillis > currentTime) {
                return calendar
            }
        }

        // Ищем следующий подходящий день
        for (i in 1..7) { // Проверяем следующие 7 дней
            val testCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, i)
            }

            val testDay = getDayOfWeekInOurSystem(testCalendar.get(Calendar.DAY_OF_WEEK))

            if (sortedDays.contains(testDay)) {
                return testCalendar
            }
        }

        // Если ничего не нашли (невозможно), возвращаем завтра
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar
    }

    private fun getDayOfWeekInOurSystem(androidDayOfWeek: Int): Int {
        return when (androidDayOfWeek) {
            Calendar.SUNDAY -> 7
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }
    }

    private fun showTimeToAlarmToast(context: Context, currentTime: Long, triggerTime: Long) {
        val diff = triggerTime - currentTime

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}д")
        if (hours > 0) parts.add("${hours}ч")
        if (minutes > 0) parts.add("${minutes}мин")

        if (parts.isEmpty() && seconds > 0) {
            parts.add("${seconds}сек")
        }

        val message = if (parts.isNotEmpty()) {
            "Будильник сработает через ${parts.joinToString(" ")}"
        } else {
            "Будильник сработает через несколько секунд"
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}