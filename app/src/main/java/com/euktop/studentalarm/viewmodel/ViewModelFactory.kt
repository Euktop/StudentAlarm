// app/src/main/java/com/euktop/studentalarm/viewmodel/ViewModelFactory.kt
package com.euktop.studentalarm.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.euktop.studentalarm.data.AlarmRepository

class ViewModelFactory(
    private val repository: AlarmRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AlarmsViewModel::class.java) -> {
                AlarmsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AlarmEditViewModel::class.java) -> {
                AlarmEditViewModel(repository, context) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}