package com.euktop.studentalarm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.euktop.studentalarm.Alarm

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "hour")
    val hour: Int,

    @ColumnInfo(name = "minute")
    val minute: Int,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,

    @ColumnInfo(name = "next_trigger_time", defaultValue = "0")
    val nextTriggerTime: Long = 0L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toAlarm(): Alarm {
        val daysList = if (daysOfWeek.isNotEmpty()) {
            daysOfWeek.split(",").map { it.toInt() }
        } else {
            emptyList()
        }

        return Alarm(
            id = id,
            hour = hour,
            minute = minute,
            description = description,
            daysOfWeek = daysList,
            isEnabled = isEnabled,
            nextTriggerTime = nextTriggerTime
        )
    }
}