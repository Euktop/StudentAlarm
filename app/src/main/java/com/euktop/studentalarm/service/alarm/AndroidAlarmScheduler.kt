package com.euktop.studentalarm.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.euktop.studentalarm.data.model.Alarm
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.utils.ResourceProvider
import com.euktop.studentalarm.utils.permission.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context,
    private val repository: AlarmRepository,
    private val notifier: IAlarmNotifier,
    private val resourceProvider: ResourceProvider
) : IAlarmScheduler {

    override suspend fun scheduleAlarm(alarm: Alarm) {
        if (!PermissionManager.hasAllAlarmPermissions(context)) {
            notifier.showPermissionError()
            return
        }

        val triggerTime = getNextAlarmTime(alarm)

        repository.updateAlarm(alarm.copy(nextTriggerTime = triggerTime))

        scheduleWithAlarmManager(alarm, triggerTime)

        val timeText = AlarmTimeCalculator.getTimeToAlarmText(triggerTime, resourceProvider)
        notifier.showAlarmScheduledToast(timeText)
    }

    private fun getNextAlarmTime(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val currentTime = now.timeInMillis
        val today = getDayOfWeekInOurSystem(now.get(Calendar.DAY_OF_WEEK))

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.daysOfWeek.isEmpty()) {
            if (calendar.timeInMillis <= currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        val sortedDays = alarm.daysOfWeek.sorted()

        if (sortedDays.contains(today)) {
            if (calendar.timeInMillis > currentTime) {
                return calendar.timeInMillis
            }
        }

        for (i in 1..7) {
            val testCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, i)
            }

            val testDay = getDayOfWeekInOurSystem(testCalendar.get(Calendar.DAY_OF_WEEK))

            if (sortedDays.contains(testDay)) {
                return testCalendar.timeInMillis
            }
        }

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }

    private fun getDayOfWeekInOurSystem(androidDayOfWeek: Int): Int {
        return when (androidDayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    private fun scheduleWithAlarmManager(alarm: Alarm, triggerTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            setAction("ALARM_${alarm.id}")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, AlarmActivity::class.java).apply {
                    putExtra("alarm_id", alarm.id)
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

    override suspend fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            setAction("ALARM_${alarmId}")
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

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = repository.getAlarmById(alarmId)
            alarm?.let {
                repository.updateAlarm(it.copy(nextTriggerTime = 0L))
            }
        }
    }

    override suspend fun rescheduleAllAlarms() {
        val alarms = repository.getAllAlarms().first()
        alarms.forEach { alarm: Alarm ->
            if (alarm.isEnabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    scheduleAlarm(alarm)
                }
            }
        }
    }

    override suspend fun checkMissedAlarms() {
        val currentTime = System.currentTimeMillis()
        val missedAlarms = repository.getMissedAlarms(currentTime)

        if (missedAlarms.isNotEmpty()) {
            missedAlarms.forEach { alarm: Alarm ->
                if (alarm.daysOfWeek.isEmpty()) {
                    repository.updateAlarm(alarm.copy(isEnabled = false, nextTriggerTime = 0L))
                    cancelAlarm(alarm.id)
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        scheduleAlarm(alarm)
                    }
                }
            }
        }
    }
}