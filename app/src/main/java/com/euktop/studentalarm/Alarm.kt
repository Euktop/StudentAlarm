package com.euktop.studentalarm

import android.annotation.SuppressLint
import android.content.Context
import com.euktop.studentalarm.data.AlarmEntity
import java.util.Calendar

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val description: String = "",
    val daysOfWeek: List<Int> = emptyList(),
    var isEnabled: Boolean = true,
    var nextTriggerTime: Long = 0L // Новое поле
) {
    @SuppressLint("DefaultLocale")
    fun formattedTime(): String = String.format("%02d:%02d", hour, minute)

    fun timeInMinutes(): Int = hour * 60 + minute

    fun getRepetitionText(context: Context): String {
        return when {
            daysOfWeek.isEmpty() -> context.getString(R.string.RepeatOnce)
            daysOfWeek.size == 7 -> context.getString(R.string.RepeatDaily)
            daysOfWeek.size == 5 &&
                    daysOfWeek.containsAll(listOf(1, 2, 3, 4, 5)) -> context.getString(R.string.RepeatWeekdays)
            daysOfWeek.size == 2 &&
                    daysOfWeek.containsAll(listOf(6, 7)) -> context.getString(R.string.RepeatWeekends)
            else -> daysOfWeek.sorted().joinToString(", ") { day ->
                when (day) {
                    1 -> context.getString(R.string.DayOfWeekMondayShort)
                    2 -> context.getString(R.string.DayOfWeekTuesdayShort)
                    3 -> context.getString(R.string.DayOfWeekWednesdayShort)
                    4 -> context.getString(R.string.DayOfWeekThursdayShort)
                    5 -> context.getString(R.string.DayOfWeekFridayShort)
                    6 -> context.getString(R.string.DayOfWeekSaturdayShort)
                    7 -> context.getString(R.string.DayOfWeekSundayShort)
                    else -> ""
                }
            }
        }
    }

    fun toAlarmEntity(): AlarmEntity {
        return AlarmEntity(
            id = this.id,
            hour = this.hour,
            minute = this.minute,
            description = this.description,
            daysOfWeek = this.daysOfWeek.joinToString(","),
            isEnabled = this.isEnabled,
            nextTriggerTime = this.nextTriggerTime
        )
    }

    companion object {
        fun fromCalendar(calendar: Calendar): Alarm {
            return Alarm(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            )
        }

        fun fromAlarmEntity(entity: AlarmEntity): Alarm {
            val daysList = if (entity.daysOfWeek.isNotEmpty()) {
                entity.daysOfWeek.split(",").map { it.toInt() }
            } else {
                emptyList()
            }

            return Alarm(
                id = entity.id,
                hour = entity.hour,
                minute = entity.minute,
                description = entity.description,
                daysOfWeek = daysList,
                isEnabled = entity.isEnabled,
                nextTriggerTime = entity.nextTriggerTime
            )
        }

        fun createNew(): Alarm {
            val calendar = Calendar.getInstance()
            return Alarm(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            )
        }
    }
}