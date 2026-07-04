package com.euktop.studentalarm

import android.app.Activity
import android.content.Context

class AlarmPermissionManager(private val context: Context) {

    fun hasAllPermissions(): Boolean {
        return PermissionManager.hasAllAlarmPermissions(context)
    }

    fun showPermissionsDialog() {
        if (context is Activity) {
            if (context is MainActivity) {
                context.checkPermissionsAndExecute { /* do nothing */ }
            } else {
                PermissionManager.showAllPermissionsDialog(context)
            }
        }
    }
}