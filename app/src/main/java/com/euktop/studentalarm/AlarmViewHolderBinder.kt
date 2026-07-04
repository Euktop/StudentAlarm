package com.euktop.studentalarm

import android.content.Context
import android.view.View

class AlarmViewHolderBinder(private val context: Context) {

    fun bind(
        holder: AlarmRecyclerAdapter.ViewHolder,
        alarm: Alarm,
        isSelectionMode: Boolean,
        isSelected: Boolean
    ) {
        holder.timeTextView.text = alarm.formattedTime()
        holder.descriptionTextView.text = alarm.description
        holder.repetitionRateTextView.text = alarm.getRepetitionText(context)

        // Управление видимостью элементов
        if (isSelectionMode) {
            holder.checkBoxSelect.visibility = View.VISIBLE
            holder.isEnabledAlarmSwitch.visibility = View.GONE
        } else {
            holder.checkBoxSelect.visibility = View.GONE
            holder.isEnabledAlarmSwitch.visibility = View.VISIBLE
        }

        // Состояние чекбокса
        holder.checkBoxSelect.isChecked = isSelected

        // Состояние переключателя
        holder.isEnabledAlarmSwitch.isChecked = alarm.isEnabled

        // Настройка анимации
        EffectHelper.setupRippleEffect(holder.itemView)

        holder.itemView.isClickable = true
        holder.itemView.isLongClickable = true
    }
}