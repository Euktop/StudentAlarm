package com.euktop.studentalarm

import android.app.Application
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.data.database.AppDatabase

class AlarmApplication : Application() {

    private lateinit var database: AppDatabase

    val alarmRepository: AlarmRepository
        get() = AlarmRepository(database.alarmDao())

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }
}