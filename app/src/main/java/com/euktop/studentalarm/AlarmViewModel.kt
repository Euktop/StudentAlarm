package com.euktop.studentalarm

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.data.AlarmRepository
import kotlinx.coroutines.launch

class AlarmViewModel(private val repository: AlarmRepository, private val context: Context) : ViewModel() {

    val allAlarms: LiveData<List<Alarm>> = repository.getAllAlarms().asLiveData()

    fun insertAlarm(alarm: Alarm) = viewModelScope.launch {
        val id = repository.insertAlarm(alarm)
        val insertedAlarm = alarm.copy(id = id)
        AlarmScheduler.scheduleAlarm(context, insertedAlarm, showToast = true)
    }

    fun updateAlarm(alarm: Alarm) = viewModelScope.launch {
        repository.updateAlarm(alarm)
        AlarmScheduler.scheduleAlarm(context, alarm, showToast = true)
    }

    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch {
        repository.deleteAlarm(alarm)
        AlarmScheduler.cancelAlarm(context, alarm.id)
    }

    suspend fun getAlarmById(alarmId: Long): Alarm? {
        return repository.getAlarmById(alarmId)
    }
}