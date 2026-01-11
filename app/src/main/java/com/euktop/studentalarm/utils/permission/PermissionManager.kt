package com.euktop.studentalarm.utils.permission

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.euktop.studentalarm.R

object PermissionManager {

    const val OVERLAY_PERMISSION_REQUEST_CODE = 1001

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun hasAllAlarmPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && canScheduleExactAlarms(context)
    }

    fun requestOverlayPermission(activity: Activity) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${activity.packageName}".toUri()
            )
            activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            openAppSettings(activity)
        }
    }

    fun requestScheduleExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = "package:${activity.packageName}".toUri()
                activity.startActivity(intent)
            } catch (e: Exception) {
                openAppSettings(activity)
            }
        }
    }

    fun showAllPermissionsDialog(activity: Activity, onGranted: () -> Unit = {}) {
        val missingPermissions = mutableListOf<String>()

        if (!hasOverlayPermission(activity)) {
            missingPermissions.add(activity.getString(R.string.permission_overlay))
        }

        if (!canScheduleExactAlarms(activity)) {
            missingPermissions.add(activity.getString(R.string.permission_exact_alarm))
        }

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
                onGranted()
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
                setData("package:${activity.packageName}".toUri())
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

    fun checkAndRequestPermissions(activity: Activity, onComplete: () -> Unit = {}) {
        if (hasAllAlarmPermissions(activity)) {
            onComplete()
            return
        }

        showAllPermissionsDialog(activity, onComplete)
    }
}