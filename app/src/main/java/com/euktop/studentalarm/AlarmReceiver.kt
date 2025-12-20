package com.euktop.studentalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", 0)

        // ОБНОВЛЯЕМ СОСТОЯНИЕ БУДИЛЬНИКА ПРИ СРАБАТЫВАНИИ
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as AlarmApplication
            val alarm = app.alarmRepository.getAlarmById(alarmId)
            alarm?.let {
                if (it.daysOfWeek.isEmpty()) {
                    // Для неповторяющихся - помечаем как неактивные
                    app.alarmRepository.updateAlarm(it.copy(isEnabled = false, nextTriggerTime = 0L))
                } else {
                    // Для повторяющихся - планируем следующее срабатывание
                    AlarmScheduler.scheduleAlarm(context, it)
                }
            }
        }

        // Запускаем активность будильника
        val allAlarmIds = longArrayOf(alarmId)
        AlarmActivity.startAlarm(context, allAlarmIds)
    }
}