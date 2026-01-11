package com.euktop.studentalarm.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.euktop.studentalarm.AlarmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_REBOOT,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    (context.applicationContext as? AlarmApplication)?.let { app ->
                        AlarmScheduler.rescheduleAllAlarms(context, app.alarmRepository)
                        AlarmScheduler.checkMissedAlarms(context)
                    }
                }
            }
        }
    }
}