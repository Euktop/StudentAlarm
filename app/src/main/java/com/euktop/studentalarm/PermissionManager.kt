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

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        /*
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        */
        return true
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun hasAllAlarmPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && canScheduleExactAlarms(context)
    }

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

    fun requestNotificationPermission(activity: Activity) {
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
        */
    }

    fun showAllPermissionsDialog(activity: Activity, onGranted: () -> Unit = {}) {
        val missingPermissions = mutableListOf<String>()

        if (!hasOverlayPermission(activity)) {
            missingPermissions.add(activity.getString(R.string.permission_overlay))
        }

        if (!canScheduleExactAlarms(activity)) {
            missingPermissions.add(activity.getString(R.string.permission_exact_alarm))
        }

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                missingPermissions.add(activity.getString(R.string.permission_notification))
            }
        }
        */

        if (missingPermissions.isEmpty()) {
            onGranted()
            return
        }

        val permissionsText = missingPermissions.joinToString("\n• ")

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_required))
            .setMessage(activity.getString(R.string.permissions_dialog_message, permissionsText))
            .setPositiveButton(activity.getString(R.string.settings)) { _, _ ->
                if (!hasOverlayPermission(activity)) {
                    requestOverlayPermission(activity)
                } else if (!canScheduleExactAlarms(activity)) {
                    requestScheduleExactAlarmPermission(activity)
                }

                onGranted()
            }
            .setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    activity,
                    activity.getString(R.string.alarm_disabled_no_permission),
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    fun showSpecificPermissionDialog(activity: Activity, permissionType: String, message: String, onGranted: () -> Unit = {}) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permission_required))
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.settings)) { _, _ ->
                when (permissionType) {
                    "overlay" -> requestOverlayPermission(activity)
                    "exact_alarm" -> requestScheduleExactAlarmPermission(activity)
                    "notification" -> requestNotificationPermission(activity)
                }
                onGranted()
            }
            .setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    activity,
                    activity.getString(R.string.alarm_disabled_no_permission),
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    fun checkAllPermissionsBeforeAction(activity: Activity, action: () -> Unit) {
        if (!hasAllAlarmPermissions(activity)) {
            showAllPermissionsDialog(activity, action)
        } else {
            action()
        }
    }

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
                activity.getString(R.string.open_app_settings_manually),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}