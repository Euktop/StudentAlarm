package com.euktop.studentalarm.weather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object WeatherIconStorage {

    private const val WEATHER_ICONS_DIR = "weather_icons"

    suspend fun saveIcon(context: Context, iconCode: String, bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val iconsDir = File(context.filesDir, WEATHER_ICONS_DIR)
                if (!iconsDir.exists()) {
                    iconsDir.mkdirs()
                }

                val iconFile = File(iconsDir, "$iconCode.png")
                val outputStream = FileOutputStream(iconFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun loadIcon(context: Context, iconCode: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val iconFile = File(context.filesDir, "$WEATHER_ICONS_DIR/$iconCode.png")
                if (iconFile.exists()) {
                    BitmapFactory.decodeFile(iconFile.absolutePath)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun loadOrDownloadIcon(context: Context, iconCode: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val cachedIcon = loadIcon(context, iconCode)
                if (cachedIcon != null) {
                    return@withContext cachedIcon
                }

                val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                val futureTarget = Glide.with(context)
                    .asBitmap()
                    .load(iconUrl)
                    .submit()

                val bitmap = futureTarget.get()

                saveIcon(context, iconCode, bitmap)

                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clearCache(context: Context) {
        try {
            val iconsDir = File(context.filesDir, WEATHER_ICONS_DIR)
            if (iconsDir.exists()) {
                iconsDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCacheSize(context: Context): Long {
        return try {
            val iconsDir = File(context.filesDir, WEATHER_ICONS_DIR)
            if (iconsDir.exists()) {
                iconsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}