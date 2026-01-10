// app/src/main/java/com/euktop/studentalarm/viewmodel/ClockViewModel.kt
package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockViewModel : ViewModel() {

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> = _currentDate

    private val _currentDay = MutableLiveData<String>()
    val currentDay: LiveData<String> = _currentDay

    private val _milliseconds = MutableLiveData<String>()
    val milliseconds: LiveData<String> = _milliseconds

    private var clockJob: Job? = null

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val millisFormat = SimpleDateFormat("SSS", Locale.getDefault())

    fun startClock() {
        if (clockJob?.isActive == true) return

        clockJob = viewModelScope.launch {
            while (isActive) {
                updateClock()
                delay(10) // Обновляем каждые 10 мс для плавности миллисекунд
            }
        }
    }

    fun stopClock() {
        clockJob?.cancel()
        clockJob = null
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val date = Date(now)

        _currentTime.postValue(timeFormat.format(date))
        _currentDate.postValue(dateFormat.format(date))
        _currentDay.postValue(dayFormat.format(date))
        _milliseconds.postValue(millisFormat.format(date))
    }

    override fun onCleared() {
        super.onCleared()
        stopClock()
    }
}