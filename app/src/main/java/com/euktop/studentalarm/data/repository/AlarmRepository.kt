package com.euktop.studentalarm.data.repository

import com.euktop.studentalarm.data.model.Alarm
import com.euktop.studentalarm.data.database.AlarmDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val alarmDao: AlarmDao) {

    fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms().map { entities ->
            entities.map { it.toAlarm() }
        }
    }

    suspend fun getAlarmById(alarmId: Long): Alarm? {
        return alarmDao.getAlarmById(alarmId)?.toAlarm()
    }

    suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm.toAlarmEntity())
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm.toAlarmEntity())
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm.toAlarmEntity())
    }

/*    suspend fun deleteAlarmById(alarmId: Long) {
        alarmDao.deleteAlarmById(alarmId)
    }

    suspend fun getAlarmCount(): Int {
        return alarmDao.getAlarmCount()
    }*/

    suspend fun getMissedAlarms(currentTime: Long): List<Alarm> {
        return alarmDao.getMissedAlarms(currentTime).map { it.toAlarm() }
    }

    suspend fun getAlarmsByIds(ids: List<Long>): List<Alarm> {
        return alarmDao.getAlarmsByIds(ids).map { it.toAlarm() }
    }
}