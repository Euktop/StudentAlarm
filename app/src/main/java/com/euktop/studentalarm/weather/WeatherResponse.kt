package com.euktop.studentalarm.weather

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("name") val cityName: String,
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>
)

data class Main(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("pressure") val pressure: Int
)

data class Weather(
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)