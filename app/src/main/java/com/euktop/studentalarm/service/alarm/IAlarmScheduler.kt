// app/src/main/java/com/euktop/studentalarm/service/alarm/IAlarmScheduler.kt
package com.euktop.studentalarm.service.alarm

import com.euktop.studentalarm.data.model.Alarm

interface IAlarmScheduler {
    suspend fun scheduleAlarm(alarm: Alarm)
    suspend fun cancelAlarm(alarmId: Long)
    suspend fun rescheduleAllAlarms()
    suspend fun checkMissedAlarms()
}