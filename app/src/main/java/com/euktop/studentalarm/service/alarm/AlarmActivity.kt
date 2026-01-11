package com.euktop.studentalarm.service.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.euktop.studentalarm.AlarmApplication
import com.euktop.studentalarm.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmIds: LongArray = longArrayOf()
    private var isAlarmRunning = false
    private var hasStartedAlarm = false

    companion object {
        private var isAlarmActive = false

        fun startAlarm(context: Context, alarmIds: LongArray) {
            if (isAlarmActive) {
                return
            }

            val intent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("all_alarm_ids", alarmIds)
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            try {
                context.startActivity(intent)
                isAlarmActive = true
            } catch (e: Exception) {
                showFullScreenNotification(context, alarmIds)
            }
        }

        fun isAlarmActive(): Boolean = isAlarmActive

        private fun showFullScreenNotification(context: Context, alarmIds: LongArray) {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("all_alarm_ids", alarmIds)
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    activityIntent,
                    PendingIntent.FLAG_IMMUTABLE or
                            PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager

            val channel = NotificationChannel(
                "alarm_fullscreen_channel",
                context.getString(R.string.alarms),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm channel"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(
                context,
                "alarm_fullscreen_channel"
            )
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("${context.getString(R.string.alarms)} ⏰")
                .setContentText(context.getString(R.string.tap_to_dismiss))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            notificationManager.notify(9999, notification)
        }

        fun resetAlarmState() {
            isAlarmActive = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContentView(R.layout.activity_alarm)

        alarmIds = intent.getLongArrayExtra("all_alarm_ids") ?: longArrayOf()

        if (!hasStartedAlarm) {
            startAlarm()
            hasStartedAlarm = true
        }

        loadAlarmDescriptions()

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            dismissAllAlarms()
        }
    }

    private fun loadAlarmDescriptions() {
        CoroutineScope(Dispatchers.IO).launch {
            val app = application as AlarmApplication
            val alarms = app.alarmRepository.getAlarmsByIds(alarmIds.toList())

            val alarmText = alarms.joinToString("\n---\n") { alarm ->
                alarm.description.ifEmpty { getString(R.string.alarm_without_description, alarm.formattedTime()) }
            }

            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.tvAlarmDescription).text = alarmText
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val app = application as AlarmApplication
            alarmIds.forEach { id ->
                val alarm = app.alarmRepository.getAlarmById(id)
                alarm?.let {
                    if (it.daysOfWeek.isEmpty()) {
                        app.alarmRepository.updateAlarm(it.copy(isEnabled = false, nextTriggerTime = 0L))
                        AlarmScheduler.cancelAlarm(this@AlarmActivity, it.id)
                    }
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    private fun startAlarm() {
        try {
            if (isAlarmRunning) return

            vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                longArrayOf(0, 667, 333, 667), 0
            ))

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, alarmUri)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(audioAttributes)
                isLooping = true
                prepare()
                start()
                setOnCompletionListener {
                    if (isAlarmRunning) {
                        start()
                    }
                }
            }

            isAlarmRunning = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissAllAlarms() {
        stopAlarm()

        resetAlarmState()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.cancel(9999)

        finish()
    }

    private fun stopAlarm() {
        if (!isAlarmRunning) return

        isAlarmRunning = false

        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }

        vibrator?.let {
            try {
                it.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            vibrator = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        resetAlarmState()
    }
}