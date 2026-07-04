package com.euktop.studentalarm

import android.content.Context

class AlarmClickHandler(
    private val context: Context,
    private val permissionManager: AlarmPermissionManager
) {

    var onSwitchChanged: ((alarm: Alarm, isChecked: Boolean) -> Unit)? = null
    var onItemClick: ((alarm: Alarm) -> Unit)? = null
    var onAlarmSelected: ((alarmId: Long) -> Unit)? = null
    var onSelectionModeRequested: (() -> Unit)? = null

    fun setupClickListeners(
        holder: AlarmRecyclerAdapter.ViewHolder,
        alarm: Alarm,
        isSelectionMode: Boolean
    ) {
        // Клик на чекбокс (только в режиме выбора)
        holder.checkBoxSelect.setOnClickListener {
            onAlarmSelected?.invoke(alarm.id)
        }

        // Клик на переключатель (только в обычном режиме)
        if (!isSelectionMode) {
            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !permissionManager.hasAllPermissions()) {
                    holder.isEnabledAlarmSwitch.isChecked = false
                    permissionManager.showPermissionsDialog()
                    return@setOnCheckedChangeListener
                }
                onSwitchChanged?.invoke(alarm, isChecked)
            }
        }

        // Клик на весь элемент
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                onAlarmSelected?.invoke(alarm.id)
            } else {
                onItemClick?.invoke(alarm)
            }
        }

        // Долгий клик
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                onSelectionModeRequested?.invoke()
                onAlarmSelected?.invoke(alarm.id)
                true
            } else {
                false
            }
        }
    }
}