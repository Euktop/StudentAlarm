package com.euktop.studentalarm

import android.app.Application
import com.euktop.studentalarm.data.database.AppDatabase
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.service.alarm.AndroidAlarmScheduler
import com.euktop.studentalarm.service.alarm.IAlarmScheduler
import com.euktop.studentalarm.service.alarm.ToastAlarmNotifier
import com.euktop.studentalarm.utils.ResourceProvider

class AlarmApplication : Application() {

    private lateinit var database: AppDatabase
    private lateinit var resourceProvider: ResourceProvider

    val alarmRepository: AlarmRepository by lazy {
        AlarmRepository(database.alarmDao())
    }

    val alarmScheduler: IAlarmScheduler by lazy {
        AndroidAlarmScheduler(
            context = this,
            repository = alarmRepository,
            notifier = ToastAlarmNotifier(this),
            resourceProvider = resourceProvider
        )
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        resourceProvider = ResourceProvider(this)
    }
}