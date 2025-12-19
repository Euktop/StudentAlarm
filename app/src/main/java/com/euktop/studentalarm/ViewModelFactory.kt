package com.euktop.studentalarm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.euktop.studentalarm.data.AlarmRepository

class ViewModelFactory(
    private val repository: AlarmRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}