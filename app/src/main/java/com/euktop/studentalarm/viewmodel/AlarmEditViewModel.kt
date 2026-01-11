package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.R
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.data.model.Alarm
import com.euktop.studentalarm.utils.ResourceProvider
import kotlinx.coroutines.launch

class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _alarmState = MutableLiveData<AlarmEditState>()
    val alarmState: LiveData<AlarmEditState> = _alarmState

    private val _showTimePicker = MutableLiveData<Boolean>(false)
    val showTimePicker: LiveData<Boolean> = _showTimePicker

    private val _showDaysDialog = MutableLiveData<Boolean>(false)
    val showDaysDialog: LiveData<Boolean> = _showDaysDialog

    private val _showDescriptionDialog = MutableLiveData<Boolean>(false)
    val showDescriptionDialog: LiveData<Boolean> = _showDescriptionDialog

    private val _uiMessage = MutableLiveData<AlarmEditMessage?>()
    val uiMessage: LiveData<AlarmEditMessage?> = _uiMessage

    private var alarmId: Long = 0L

    init {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MINUTE, 1)
        _alarmState.value = AlarmEditState(
            hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            minute = calendar.get(java.util.Calendar.MINUTE),
            description = "",
            daysOfWeek = emptySet(),
            isNewAlarm = true
        )
    }

    fun loadAlarm(alarmId: Long) {
        this.alarmId = alarmId
        if (alarmId > 0) {
            viewModelScope.launch {
                val alarm = repository.getAlarmById(alarmId)
                alarm?.let {
                    _alarmState.value = AlarmEditState(
                        hour = it.hour,
                        minute = it.minute,
                        description = it.description,
                        daysOfWeek = it.daysOfWeek.toSet(),
                        isNewAlarm = false
                    )
                } ?: run {
                    _uiMessage.value = AlarmEditMessage.Error(
                        resourceProvider.getString(R.string.alarm_not_found)
                    )
                }
            }
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        _alarmState.value = _alarmState.value?.copy(
            hour = hour,
            minute = minute
        )
    }

    fun updateDescription(description: String) {
        _alarmState.value = _alarmState.value?.copy(
            description = description
        )
    }

    fun toggleDayOfWeek(day: Int) {
        val currentState = _alarmState.value ?: return
        val newDays = currentState.daysOfWeek.toMutableSet()
        if (newDays.contains(day)) {
            newDays.remove(day)
        } else {
            newDays.add(day)
        }
        _alarmState.value = currentState.copy(daysOfWeek = newDays)
    }

    fun clearDays() {
        _alarmState.value = _alarmState.value?.copy(
            daysOfWeek = emptySet()
        )
    }

    fun setAllDays() {
        _alarmState.value = _alarmState.value?.copy(
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }

    fun setWeekdays() {
        _alarmState.value = _alarmState.value?.copy(
            daysOfWeek = setOf(1, 2, 3, 4, 5)
        )
    }

    fun setWeekends() {
        _alarmState.value = _alarmState.value?.copy(
            daysOfWeek = setOf(6, 7)
        )
    }

    fun showTimePicker() {
        _showTimePicker.value = true
    }

    fun hideTimePicker() {
        _showTimePicker.value = false
    }

    fun showDaysDialog() {
        _showDaysDialog.value = true
    }

    fun hideDaysDialog() {
        _showDaysDialog.value = false
    }

    fun showDescriptionDialog() {
        _showDescriptionDialog.value = true
    }

    fun hideDescriptionDialog() {
        _showDescriptionDialog.value = false
    }

    fun saveAlarm() {
        val state = _alarmState.value ?: return

        if (state.hour !in 0..23 || state.minute !in 0..59) {
            _uiMessage.value = AlarmEditMessage.Error(
                resourceProvider.getString(R.string.invalid_time)
            )
            return
        }

        val alarm = Alarm(
            id = if (state.isNewAlarm) 0L else alarmId,
            hour = state.hour,
            minute = state.minute,
            description = state.description,
            daysOfWeek = state.daysOfWeek.toList(),
            isEnabled = true
        )

        viewModelScope.launch {
            try {
                if (state.isNewAlarm) {
                    val newId = repository.insertAlarm(alarm)
                    _uiMessage.value = AlarmEditMessage.Success(
                        alarmId = newId,
                        shouldSchedule = true,
                        isNewAlarm = true
                    )
                } else {
                    repository.updateAlarm(alarm)
                    _uiMessage.value = AlarmEditMessage.Success(
                        alarmId = alarm.id,
                        shouldSchedule = true,
                        isNewAlarm = false
                    )
                }
            } catch (e: Exception) {
                _uiMessage.value = AlarmEditMessage.Error(
                    resourceProvider.getString(R.string.error)
                )
            }
        }
    }
}

data class AlarmEditState(
    val hour: Int,
    val minute: Int,
    val description: String,
    val daysOfWeek: Set<Int>,
    val isNewAlarm: Boolean
)

sealed class AlarmEditMessage {
    data class Success(
        val alarmId: Long,
        val shouldSchedule: Boolean,
        val isNewAlarm: Boolean
    ) : AlarmEditMessage()

    data class Error(val message: String) : AlarmEditMessage()
}