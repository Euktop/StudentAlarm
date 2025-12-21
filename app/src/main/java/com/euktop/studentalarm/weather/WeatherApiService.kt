package com.euktop.studentalarm.weather

import com.euktop.studentalarm.BuildConfig
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    fun getWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = BuildConfig.WEATHER_API_KEY,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): Call<WeatherResponse>
}