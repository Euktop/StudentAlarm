package com.euktop.studentalarm

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private var alarmIds: LongArray = longArrayOf()

    companion object {
        private var isAlarmActive = false

        fun startAlarm(context: Context, alarmIds: LongArray) {
            // Проверяем, не запущена ли уже активность
            if (isAlarmActive) return

            val intent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("all_alarm_ids", alarmIds)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // Пытаемся запустить активность
            try {
                context.startActivity(intent)
                isAlarmActive = true
            } catch (e: Exception) {
                // Если не получилось, используем FullScreenIntent
                showFullScreenNotification(context, alarmIds)
            }
        }

        private fun showFullScreenNotification(context: Context, alarmIds: LongArray) {
            // Создаем PendingIntent для активности
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("all_alarm_ids", alarmIds)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    activityIntent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                android.app.PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    activityIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Создаем уведомление с FullScreenIntent
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager

            // Создаем канал для Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "alarm_fullscreen_channel",
                    "Будильник",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Канал для будильников"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = androidx.core.app.NotificationCompat.Builder(
                context,
                "alarm_fullscreen_channel"
            )
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Будильник ⏰")
                .setContentText("Нажмите, чтобы отключить")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            notificationManager.notify(9999, notification)
        }

        fun resetAlarmState() {
            isAlarmActive = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем флаги ДО setContentView
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContentView(R.layout.activity_alarm)

        // Получаем ID будильников
        alarmIds = intent.getLongArrayExtra("all_alarm_ids") ?: longArrayOf()

        // Загружаем информацию о будильниках
        CoroutineScope(Dispatchers.IO).launch {
            val app = application as AlarmApplication
            var alarmText = ""

            alarmIds.forEachIndexed { index, id ->
                val alarm = app.alarmRepository.getAlarmById(id)
                alarm?.let {
                    val desc = if (it.description.isNotEmpty()) it.description
                    else "Будильник ${it.formattedTime()}"
                    alarmText += if (index == 0) desc else "\n---\n$desc"
                }
            }

            runOnUiThread {
                val tvDescription = findViewById<TextView>(R.id.tvAlarmDescription)
                tvDescription.text = alarmText

                // Запускаем звук и вибрацию
                startAlarm()
            }
        }

        // Кнопка "Отключить все"
        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            dismissAllAlarms()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    private fun startAlarm() {
        // Вибрация
        vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 667, 333, 667), 0
            ))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 667, 333, 667), 0)
        }

        // Звук будильника через MediaPlayer
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
        }
    }

    private fun dismissAllAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            val app = application as AlarmApplication
            alarmIds.forEach { id ->
                val alarm = app.alarmRepository.getAlarmById(id)
                alarm?.let {
                    app.alarmRepository.updateAlarm(it.copy(isEnabled = false))
                    AlarmScheduler.cancelAlarm(this@AlarmActivity, id)
                }
            }
        }

        // Останавливаем звук и вибрацию
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()

        AlarmActivity.resetAlarmState()

        // Закрываем уведомление, если есть
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        notificationManager.cancel(9999)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
        AlarmActivity.resetAlarmState()
    }
}