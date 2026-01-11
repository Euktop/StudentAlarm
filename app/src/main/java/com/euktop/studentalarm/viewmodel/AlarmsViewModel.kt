package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.service.alarm.IAlarmScheduler
import kotlinx.coroutines.launch

class AlarmsViewModel(
    private val repository: AlarmRepository,
    private val alarmScheduler: IAlarmScheduler
) : ViewModel() {

    val alarms = repository.getAllAlarms().asLiveData()

    private val _uiState = MutableLiveData<AlarmsUiState>(AlarmsUiState.Normal)
    val uiState: LiveData<AlarmsUiState> = _uiState

    private val _selectedAlarms = MutableLiveData<Set<Long>>(emptySet())
    val selectedAlarms: LiveData<Set<Long>> = _selectedAlarms

    fun toggleAlarmEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            alarm?.let {
                val updatedAlarm = it.copy(isEnabled = enabled)
                repository.updateAlarm(updatedAlarm)

                if (enabled) {
                    alarmScheduler.scheduleAlarm(updatedAlarm)
                } else {
                    alarmScheduler.cancelAlarm(alarmId)
                }
            }
        }
    }

    fun enterSelectionMode() {
        _uiState.value = AlarmsUiState.Selection
        _selectedAlarms.value = emptySet()
    }

    fun exitSelectionMode() {
        _uiState.value = AlarmsUiState.Normal
        _selectedAlarms.value = emptySet()
    }

    fun toggleAlarmSelection(alarmId: Long) {
        val currentSelection = _selectedAlarms.value?.toMutableSet() ?: mutableSetOf()
        if (currentSelection.contains(alarmId)) {
            currentSelection.remove(alarmId)
        } else {
            currentSelection.add(alarmId)
        }
        _selectedAlarms.value = currentSelection

        if (currentSelection.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun selectAll(alarmIds: List<Long>) {
        _selectedAlarms.value = alarmIds.toSet()
    }

    fun clearSelection() {
        _selectedAlarms.value = emptySet()
    }

    fun deleteSelectedAlarms() {
        viewModelScope.launch {
            val selectedIds = _selectedAlarms.value ?: emptySet()
            selectedIds.forEach { alarmId ->
                val alarm = repository.getAlarmById(alarmId)
                alarm?.let {
                    repository.deleteAlarm(it)
                    alarmScheduler.cancelAlarm(alarmId)
                }
            }
            exitSelectionMode()
        }
    }

    fun isAllSelected(allAlarmIds: List<Long>): Boolean {
        val selected = _selectedAlarms.value ?: emptySet()
        return selected.size == allAlarmIds.size && allAlarmIds.isNotEmpty()
    }
}

sealed class AlarmsUiState {
    object Normal : AlarmsUiState()
    object Selection : AlarmsUiState()
}