package com.euktop.studentalarm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.data.AlarmRepository
import kotlinx.coroutines.launch

class AlarmViewModel(private val repository: AlarmRepository) : ViewModel() {

    val allAlarms: LiveData<List<Alarm>> = repository.getAllAlarms().asLiveData()

    fun insertAlarm(alarm: Alarm) = viewModelScope.launch {
        repository.insertAlarm(alarm)
    }

    fun updateAlarm(alarm: Alarm) = viewModelScope.launch {
        repository.updateAlarm(alarm)
    }

    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch {
        repository.deleteAlarm(alarm)
    }

    suspend fun getAlarmById(alarmId: Long): Alarm? {
        return repository.getAlarmById(alarmId)
    }
}