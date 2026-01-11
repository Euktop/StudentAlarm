package com.euktop.studentalarm.service.alarm

interface IAlarmNotifier {
    fun showAlarmScheduledToast(message: String)
    fun showPermissionError()
    fun showAlarmDisabledMessage()
}