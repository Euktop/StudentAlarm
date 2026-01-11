package com.euktop.studentalarm.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.euktop.studentalarm.data.repository.AlarmRepository
import com.euktop.studentalarm.data.repository.WeatherRepository
import com.euktop.studentalarm.service.alarm.AndroidAlarmScheduler
import com.euktop.studentalarm.service.alarm.ToastAlarmNotifier
import com.euktop.studentalarm.utils.ResourceProvider
import com.euktop.studentalarm.weather.WeatherRetrofitClient

class ViewModelFactory(
    private val context: Context,
    private val repository: AlarmRepository
) : ViewModelProvider.Factory {

    private val resourceProvider = ResourceProvider(context)
    private val alarmNotifier = ToastAlarmNotifier(context)
    private val alarmScheduler by lazy {
        AndroidAlarmScheduler(context, repository, alarmNotifier, resourceProvider)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AlarmsViewModel::class.java) -> {
                AlarmsViewModel(repository, alarmScheduler) as T
            }
            modelClass.isAssignableFrom(AlarmEditViewModel::class.java) -> {
                AlarmEditViewModel(repository, resourceProvider) as T
            }
            modelClass.isAssignableFrom(ClockViewModel::class.java) -> {
                ClockViewModel() as T
            }
            modelClass.isAssignableFrom(WeatherViewModel::class.java) -> {
                val weatherRepository = WeatherRepository(
                    context,
                    WeatherRetrofitClient.weatherApiService
                )
                WeatherViewModel(resourceProvider, weatherRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}