package com.euktop.studentalarm.service.alarm

import android.content.Context
import android.widget.Toast
import com.euktop.studentalarm.R

class ToastAlarmNotifier(private val context: Context) : IAlarmNotifier {

    override fun showAlarmScheduledToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun showPermissionError() {
        Toast.makeText(
            context,
            context.getString(R.string.permission_required),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun showAlarmDisabledMessage() {
        Toast.makeText(
            context,
            context.getString(R.string.alarm_disabled_no_permission),
            Toast.LENGTH_LONG
        ).show()
    }
}