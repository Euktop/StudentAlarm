package com.euktop.studentalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.euktop.studentalarm.R
import com.euktop.studentalarm.data.repository.WeatherRepository
import com.euktop.studentalarm.data.repository.WeatherResult
import com.euktop.studentalarm.data.repository.WeatherUI
import com.euktop.studentalarm.utils.ResourceProvider
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val resourceProvider: ResourceProvider,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>(WeatherState.Loading)
    val weatherState: LiveData<WeatherState> = _weatherState

    private val _permissionState = MutableLiveData<PermissionState>(PermissionState.NotDetermined)
    val permissionState: LiveData<PermissionState> = _permissionState

    private val _locationState = MutableLiveData<LocationState>(LocationState.Idle)
    val locationState: LiveData<LocationState> = _locationState

    fun loadWeather(forceRefresh: Boolean = false) {
        _weatherState.value = WeatherState.Loading

        viewModelScope.launch {
            try {
                val lastLocation = weatherRepository.getLastLocation()

                if (lastLocation != null && !forceRefresh) {
                    fetchWeather(lastLocation.first, lastLocation.second, false)
                } else {
                    requestCurrentLocation()
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(
                    resourceProvider.getString(R.string.weather_network_error, e.message ?: "")
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
        _weatherState.value = WeatherState.Error(
            resourceProvider.getString(R.string.location_permission_required)
        )
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

        viewModelScope.launch {
            val lastLocation = weatherRepository.getLastLocation()
            if (lastLocation != null) {
                fetchWeather(lastLocation.first, lastLocation.second, false)
            } else {
                _weatherState.value = WeatherState.Error(
                    resourceProvider.getString(R.string.location_permission_denied)
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