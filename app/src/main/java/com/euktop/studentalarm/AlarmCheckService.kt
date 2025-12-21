package com.euktop.studentalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmCheckService : Service() {

    private val channelId = "AlarmCheckServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            val app = applicationContext as AlarmApplication
            AlarmScheduler.checkMissedAlarms(applicationContext)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId,
            "Alarm Check Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, channelId)
            .setContentTitle("Alarm")
            .setContentText("Checking missed alarms")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
    }
}