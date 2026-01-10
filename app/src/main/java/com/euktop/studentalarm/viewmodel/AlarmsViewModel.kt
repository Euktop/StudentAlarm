// app/src/main/java/com/euktop/studentalarm/viewmodel/AlarmsViewModel.kt
package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.data.AlarmRepository
import kotlinx.coroutines.launch

class AlarmsViewModel(
    private val repository: AlarmRepository
) : ViewModel() {

    // LiveData для списка будильников
    val alarms = repository.getAllAlarms().asLiveData()

    // Состояние UI (режим выбора)
    private val _uiState = MutableLiveData<AlarmsUiState>(AlarmsUiState.Normal)
    val uiState: LiveData<AlarmsUiState> = _uiState

    // Выбранные будильники
    private val _selectedAlarms = MutableLiveData<Set<Long>>(emptySet())
    val selectedAlarms: LiveData<Set<Long>> = _selectedAlarms

    // Методы для управления будильниками

    fun toggleAlarmEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            alarm?.let {
                repository.updateAlarm(it.copy(isEnabled = enabled))
            }
        }
    }

    // Методы для режима выбора

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

        // Если ничего не выбрано - выходим из режима выбора
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
                alarm?.let { repository.deleteAlarm(it) }
            }
            exitSelectionMode()
        }
    }

    fun isAllSelected(allAlarmIds: List<Long>): Boolean {
        val selected = _selectedAlarms.value ?: emptySet()
        return selected.size == allAlarmIds.size && allAlarmIds.isNotEmpty()
    }
}

// Выносим sealed class отдельно или оставляем внутри, но делаем public
sealed class AlarmsUiState {
    object Normal : AlarmsUiState()
    object Selection : AlarmsUiState()
}