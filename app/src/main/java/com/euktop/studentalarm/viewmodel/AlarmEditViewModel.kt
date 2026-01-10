package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.ViewModel
import com.euktop.studentalarm.data.AlarmRepository

class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val context: android.content.Context
) : ViewModel() {
}