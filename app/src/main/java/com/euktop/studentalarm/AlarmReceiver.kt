package com.euktop.studentalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", 0)

        // Просто запускаем активность - она сама позаботится о показе
        val allAlarmIds = longArrayOf(alarmId)
        AlarmActivity.startAlarm(context, allAlarmIds)
    }
}