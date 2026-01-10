// app/src/main/java/com/euktop/studentalarm/data/repository/WeatherRepository.kt
package com.euktop.studentalarm.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.euktop.studentalarm.R
import com.euktop.studentalarm.weather.WeatherApiService
import com.euktop.studentalarm.weather.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(
    private val context: Context,
    private val weatherApiService: WeatherApiService
) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    private val updateTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): WeatherResult {
        return withContext(Dispatchers.IO) {
            try {
                // Если данные свежие (менее 10 минут) и не требуется обновление, возвращаем кеш
                if (!forceRefresh && isWeatherCacheValid()) {
                    val cachedWeather = loadCachedWeather()
                    if (cachedWeather != null) {
                        return@withContext WeatherResult.Success(cachedWeather)
                    }
                }

                // Делаем запрос к API с использованием suspend функции
                val weatherData = try {
                    weatherApiService.getWeatherByCoordinates(latitude, longitude)
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> context.getString(R.string.weather_api_key_invalid)
                        404 -> context.getString(R.string.weather_location_not_found)
                        429 -> context.getString(R.string.weather_too_many_requests)
                        500 -> context.getString(R.string.weather_server_error)
                        else -> "${context.getString(R.string.error)}: ${e.code()}"
                    }
                    return@withContext WeatherResult.Error(errorMessage)
                } catch (e: Exception) {
                    return@withContext WeatherResult.Error("${context.getString(R.string.weather_network_error)}: ${e.message}")
                }

                // Сохраняем в кеш
                saveWeatherToCache(weatherData, latitude, longitude)

                // Преобразуем в UI модель
                val uiWeather = WeatherUI(
                    city = weatherData.cityName,
                    temperature = "${weatherData.main.temperature.toInt()}°C",
                    description = weatherData.weather.firstOrNull()?.description
                        ?.replaceFirstChar { it.uppercase() } ?: "",
                    iconCode = weatherData.weather.firstOrNull()?.icon ?: "",
                    lastUpdate = updateTimeFormat.format(Date()),
                    latitude = latitude,
                    longitude = longitude
                )

                WeatherResult.Success(uiWeather)
            } catch (e: Exception) {
                // Пытаемся вернуть кешированные данные при ошибке сети
                val cachedWeather = loadCachedWeather()
                if (cachedWeather != null) {
                    WeatherResult.Success(cachedWeather)
                } else {
                    WeatherResult.Error("${context.getString(R.string.weather_network_error)}: ${e.message}")
                }
            }
        }
    }

    private fun isWeatherCacheValid(): Boolean {
        val lastUpdate = sharedPreferences.getLong("last_update", 0)
        val cacheDuration = 10 * 60 * 1000L // 10 минут
        return System.currentTimeMillis() - lastUpdate < cacheDuration
    }

    private fun loadCachedWeather(): WeatherUI? {
        val city = sharedPreferences.getString("weather_city", null)
        val temp = sharedPreferences.getFloat("weather_temp", -1000f)
        val desc = sharedPreferences.getString("weather_desc", "")
        val icon = sharedPreferences.getString("weather_icon", "")
        val lastUpdate = sharedPreferences.getLong("last_update", 0)
        val lat = sharedPreferences.getFloat("last_lat", 0f)
        val lon = sharedPreferences.getFloat("last_lon", 0f)

        return if (city != null && temp > -1000f) {
            WeatherUI(
                city = city,
                temperature = "${temp.toInt()}°C",
                description = desc ?: "",
                iconCode = icon ?: "",
                lastUpdate = if (lastUpdate > 0) updateTimeFormat.format(Date(lastUpdate)) else "",
                latitude = lat.toDouble(),
                longitude = lon.toDouble()
            )
        } else {
            null
        }
    }

    private fun saveWeatherToCache(weatherData: WeatherResponse, lat: Double, lon: Double) {
        sharedPreferences.edit().apply {
            putFloat("last_lat", lat.toFloat())
            putFloat("last_lon", lon.toFloat())
            putLong("last_update", System.currentTimeMillis())
            putString("weather_city", weatherData.cityName)
            putFloat("weather_temp", weatherData.main.temperature.toFloat())
            putInt("weather_humidity", weatherData.main.humidity)
            weatherData.weather.firstOrNull()?.let { weather ->
                putString("weather_desc", weather.description)
                putString("weather_icon", weather.icon)
            }
            apply()
        }
    }

    fun getLastLocation(): Pair<Double, Double>? {
        val lat = sharedPreferences.getFloat("last_lat", 0f)
        val lon = sharedPreferences.getFloat("last_lon", 0f)
        return if (lat != 0f && lon != 0f) {
            Pair(lat.toDouble(), lon.toDouble())
        } else {
            null
        }
    }
}

sealed class WeatherResult {
    data class Success(val weather: WeatherUI) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}

data class WeatherUI(
    val city: String,
    val temperature: String,
    val description: String,
    val iconCode: String,
    val lastUpdate: String,
    val latitude: Double,
    val longitude: Double
)