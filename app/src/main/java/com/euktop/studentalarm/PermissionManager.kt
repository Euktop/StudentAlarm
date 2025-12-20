package com.euktop.studentalarm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
    const val SCHEDULE_EXACT_ALARM_REQUEST_CODE = 1003

    // Проверка разрешения на показ поверх окон
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    // Проверка разрешения на уведомления (для API 33+)
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Проверка разрешения на установку точных будильников (для Android 12+)
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // Проверка всех необходимых разрешений для работы будильника
    fun hasAllAlarmPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && canScheduleExactAlarms(context)
    }

    // Запрос разрешения на показ поверх окон
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                openAppSettings(activity)
            }
        }
    }

    // Запрос разрешения на установку точных будильников
    fun requestScheduleExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            } catch (e: Exception) {
                openAppSettings(activity)
            }
        }
    }

    // Запрос разрешения на уведомления
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Показать диалог для запроса всех необходимых разрешений
    fun showAllPermissionsDialog(activity: Activity, onGranted: () -> Unit = {}) {
        val missingPermissions = mutableListOf<String>()

        if (!hasOverlayPermission(activity)) {
            missingPermissions.add("отображение поверх окон")
        }

        if (!canScheduleExactAlarms(activity)) {
            missingPermissions.add("установка точных будильников")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                missingPermissions.add("уведомления")
            }
        }

        if (missingPermissions.isEmpty()) {
            onGranted()
            return
        }

        val permissionsText = missingPermissions.joinToString(", ")

        AlertDialog.Builder(activity)
            .setTitle("Требуются разрешения")
            .setMessage("Для работы будильника необходимы разрешения:\n\n• ${missingPermissions.joinToString("\n• ")}\n\n" +
                    "Без этих разрешений будильник не сможет работать корректно.")
            .setPositiveButton("Настройки") { _, _ ->
                // Запрашиваем первое недостающее разрешение
                if (!hasOverlayPermission(activity)) {
                    requestOverlayPermission(activity)
                } else if (!canScheduleExactAlarms(activity)) {
                    requestScheduleExactAlarmPermission(activity)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(activity)) {
                    requestNotificationPermission(activity)
                }

                onGranted()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    activity,
                    "Будильник не будет работать без необходимых разрешений",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    // Показать диалог для конкретного разрешения
    fun showSpecificPermissionDialog(activity: Activity, permissionType: String, message: String, onGranted: () -> Unit = {}) {
        AlertDialog.Builder(activity)
            .setTitle("Требуется разрешение")
            .setMessage(message)
            .setPositiveButton("Настройки") { _, _ ->
                when (permissionType) {
                    "overlay" -> requestOverlayPermission(activity)
                    "exact_alarm" -> requestScheduleExactAlarmPermission(activity)
                    "notification" -> requestNotificationPermission(activity)
                }
                onGranted()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    activity,
                    "Будильник не будет работать без этого разрешения",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    // Проверка и запрос разрешений перед действием
    fun checkAllPermissionsBeforeAction(activity: Activity, action: () -> Unit) {
        if (!hasAllAlarmPermissions(activity)) {
            showAllPermissionsDialog(activity, action)
        } else {
            action()
        }
    }

    // Открыть настройки приложения
    fun openAppSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Откройте настройки приложения вручную",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}