// app/src/main/java/com/euktop/studentalarm/viewmodel/WeatherViewModel.kt
package com.euktop.studentalarm.viewmodel

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.R
import com.euktop.studentalarm.data.repository.WeatherRepository
import com.euktop.studentalarm.data.repository.WeatherResult
import com.euktop.studentalarm.data.repository.WeatherUI
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WeatherViewModel(
    private val context: Context,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>(WeatherState.Loading)
    val weatherState: LiveData<WeatherState> = _weatherState

    private val _permissionState = MutableLiveData<PermissionState>(PermissionState.NotDetermined)
    val permissionState: LiveData<PermissionState> = _permissionState

    private val _locationState = MutableLiveData<LocationState>(LocationState.Idle)
    val locationState: LiveData<LocationState> = _locationState

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun loadWeather(forceRefresh: Boolean = false) {
        _weatherState.value = WeatherState.Loading

        viewModelScope.launch {
            try {
                // Пытаемся использовать последнее известное местоположение
                val lastLocation = weatherRepository.getLastLocation()

                if (lastLocation != null && !forceRefresh) {
                    fetchWeather(lastLocation.first, lastLocation.second, false)
                } else {
                    // Получаем новое местоположение
                    requestCurrentLocation()
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(
                    "${context.getString(R.string.weather_network_error)}: ${e.message}"
                )
            }
        }
    }

    private suspend fun fetchWeather(latitude: Double, longitude: Double, forceRefresh: Boolean) {
        when (val result = weatherRepository.getCurrentWeather(latitude, longitude, forceRefresh)) {
            is WeatherResult.Success -> {
                _weatherState.value = WeatherState.Success(result.weather)
                _locationState.value = LocationState.Success
            }
            is WeatherResult.Error -> {
                _weatherState.value = WeatherState.Error(result.message)
                _locationState.value = LocationState.Error(result.message)
            }
        }
    }

    private fun requestCurrentLocation() {
        _locationState.value = LocationState.Requesting

        viewModelScope.launch {
            try {
                val location = getCurrentLocation()
                if (location != null) {
                    fetchWeather(location.latitude, location.longitude, true)
                } else {
                    _weatherState.value = WeatherState.Error(
                        context.getString(R.string.location_not_determined)
                    )
                    _locationState.value = LocationState.Error(
                        context.getString(R.string.location_not_determined)
                    )
                }
            } catch (e: SecurityException) {
                _weatherState.value = WeatherState.Error(
                    context.getString(R.string.location_permission_denied)
                )
                _locationState.value = LocationState.Error(
                    context.getString(R.string.location_permission_denied)
                )
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(
                    "${context.getString(R.string.error)}: ${e.message}"
                )
                _locationState.value = LocationState.Error(
                    "${context.getString(R.string.error)}: ${e.message}"
                )
            }
        }
    }

    private suspend fun getCurrentLocation(): Location? {
        return try {
            val priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).await()
        } catch (e: SecurityException) {
            null
        }
    }

    fun refreshWeather() {
        loadWeather(forceRefresh = true)
    }

    fun onPermissionGranted() {
        _permissionState.value = PermissionState.Granted
        loadWeather(forceRefresh = true)
    }

    fun onPermissionDenied() {
        _permissionState.value = PermissionState.Denied

        // Пытаемся использовать кешированные данные
        viewModelScope.launch {
            val lastLocation = weatherRepository.getLastLocation()
            if (lastLocation != null) {
                fetchWeather(lastLocation.first, lastLocation.second, false)
            } else {
                _weatherState.value = WeatherState.Error(
                    context.getString(R.string.location_permission_denied)
                )
            }
        }
    }
}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val weather: WeatherUI) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

sealed class PermissionState {
    object NotDetermined : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
}

sealed class LocationState {
    object Idle : LocationState()
    object Requesting : LocationState()
    object Success : LocationState()
    data class Error(val message: String) : LocationState()
}