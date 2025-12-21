package com.euktop.studentalarm.weather

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GeocoderHelper {

    suspend fun getLocationName(context: Context, latitude: Double, longitude: Double): String {
        return try {
            withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                addresses?.firstOrNull()?.let { address ->
                    when {
                        address.locality != null -> address.locality!!
                        address.subLocality != null -> address.subLocality!!
                        address.featureName != null -> address.featureName!!
                        address.adminArea != null -> address.adminArea!!
                        else -> "Текущее местоположение"
                    }
                } ?: "Текущее местоположение"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Текущее местоположение"
        }
    }
}