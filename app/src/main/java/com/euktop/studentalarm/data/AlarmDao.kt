package com.euktop.studentalarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: Long): AlarmEntity?

    @Insert
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Long)

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getAlarmCount(): Int

    // Новый метод для поиска пропущенных будильников
    @Query("SELECT * FROM alarms WHERE is_enabled = 1 AND next_trigger_time > 0 AND next_trigger_time <= :currentTime")
    suspend fun getMissedAlarms(currentTime: Long): List<AlarmEntity>
}