package com.euktop.studentalarm

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AlarmRecyclerAdapter(
    private val context: Context
) : RecyclerView.Adapter<AlarmRecyclerAdapter.ViewHolder>() {

    private var alarms: List<Alarm> = emptyList()
    private val viewHolderBinder = AlarmViewHolderBinder(context)
    private val clickHandler = AlarmClickHandler(context, AlarmPermissionManager(context))
    private val modeManager = AlarmModeManager()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBoxSelect: CheckBox = view.findViewById(R.id.checkBoxSelect)
        val timeTextView: TextView = view.findViewById(R.id.TimeTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.DescriptionTextView)
        val repetitionRateTextView: TextView = view.findViewById(R.id.RepetitionRateTextView)
        val isEnabledAlarmSwitch: Switch = view.findViewById(R.id.IsEnabledAlarmSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_alarm, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]

        // 1. Биндинг данных (только отображение)
        viewHolderBinder.bind(
            holder,
            alarm,
            modeManager.isSelectionMode,
            modeManager.isSelected(alarm.id)
        )

        // 2. Настройка обработчиков кликов
        clickHandler.setupClickListeners(holder, alarm, modeManager.isSelectionMode)
    }

    override fun getItemCount(): Int = alarms.size

    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }

    // Публичные методы для делегирования
    fun enableSelectionMode() = modeManager.enableSelectionMode()
    fun disableSelectionMode() = modeManager.disableSelectionMode()
    fun toggleSelection(alarmId: Long) = modeManager.toggleSelection(alarmId)
    fun clearSelection() = modeManager.clearSelection()

    // Публичные коллбэки
    fun setOnSwitchChanged(listener: (Alarm, Boolean) -> Unit) {
        clickHandler.onSwitchChanged = listener
    }

    fun setOnItemClick(listener: (Alarm) -> Unit) {
        clickHandler.onItemClick = listener
    }

    fun setOnAlarmSelected(listener: (Long) -> Unit) {
        clickHandler.onAlarmSelected = listener
    }

    fun setOnSelectionModeRequested(listener: () -> Unit) {
        clickHandler.onSelectionModeRequested = listener
    }
}