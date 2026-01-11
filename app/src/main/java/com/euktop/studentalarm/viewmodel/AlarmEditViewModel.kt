// app/src/main/java/com/euktop/studentalarm/viewmodel/AlarmEditViewModel.kt
package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.R
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.data.model.Alarm
import com.euktop.studentalarm.service.alarm.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val context: android.content.Context
) : ViewModel() {

    // Состояние редактирования будильника
    private val _alarmState = MutableLiveData<AlarmEditState>()
    val alarmState: LiveData<AlarmEditState> = _alarmState

    // Состояния диалогов
    private val _showTimePicker = MutableLiveData<Boolean>(false)
    val showTimePicker: LiveData<Boolean> = _showTimePicker

    private val _showDaysDialog = MutableLiveData<Boolean>(false)
    val showDaysDialog: LiveData<Boolean> = _showDaysDialog

    private val _showDescriptionDialog = MutableLiveData<Boolean>(false)
    val showDescriptionDialog: LiveData<Boolean> = _showDescriptionDialog

    // Сообщения для UI
    private val _uiMessage = MutableLiveData<UiMessage?>()
    val uiMessage: LiveData<UiMessage?> = _uiMessage

    private var alarmId: Long = 0L

    init {
        // Инициализируем состояние по умолчанию
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 1)
        _alarmState.value = AlarmEditState(
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
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
                    // Если будильник не найден, отправляем сообщение об ошибке
                    _uiMessage.value = UiMessage.Error("Будильник не найден")
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

        // Валидация времени
        if (state.hour !in 0..23 || state.minute !in 0..59) {
            _uiMessage.value = UiMessage.Error(context.getString(R.string.invalid_time))
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
                    AlarmScheduler.scheduleAlarm(context, alarm.copy(id = newId), showToast = true)
                } else {
                    repository.updateAlarm(alarm)
                    AlarmScheduler.scheduleAlarm(context, alarm, showToast = true)
                }
                _uiMessage.value = UiMessage.Success
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Error(context.getString(R.string.error))
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

sealed class UiMessage {
    object Success : UiMessage()
    data class Error(val message: String) : UiMessage()
}