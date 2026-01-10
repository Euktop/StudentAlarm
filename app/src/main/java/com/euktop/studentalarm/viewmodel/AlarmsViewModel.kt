package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.ViewModel
import com.euktop.studentalarm.data.AlarmRepository

class AlarmsViewModel(
    private val repository: AlarmRepository
) : ViewModel() {
}