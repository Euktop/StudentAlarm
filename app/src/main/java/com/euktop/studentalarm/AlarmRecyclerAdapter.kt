package com.euktop.studentalarm

import android.annotation.SuppressLint
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
    private val context: Context,
    private var alarms: List<Alarm> = emptyList()
) : RecyclerView.Adapter<AlarmRecyclerAdapter.ViewHolder>() {

    var onSwitchChanged: ((alarm: Alarm, isChecked: Boolean) -> Unit)? = null
    var onItemClick: ((alarm: Alarm) -> Unit)? = null
    var onItemLongClick: ((alarm: Alarm) -> Unit)? = null

    // Новые колбэки для режима выделения
    var onSelectionModeChanged: ((isSelectionMode: Boolean) -> Unit)? = null
    var onSelectedCountChanged: ((count: Int) -> Unit)? = null

    // Состояние выделения
    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<Long>()

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

        holder.timeTextView.text = alarm.formattedTime()
        holder.descriptionTextView.text = alarm.description
        holder.repetitionRateTextView.text = alarm.getRepetitionText(context)

        // Управляем видимостью элементов в зависимости от режима
        if (isSelectionMode) {
            holder.checkBoxSelect.visibility = View.VISIBLE
            holder.isEnabledAlarmSwitch.visibility = View.GONE
        } else {
            holder.checkBoxSelect.visibility = View.GONE
            holder.isEnabledAlarmSwitch.visibility = View.VISIBLE
        }

        // Настройка Switch только в обычном режиме
        if (!isSelectionMode) {
            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener(null)
            holder.isEnabledAlarmSwitch.isChecked = alarm.isEnabled
            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged?.invoke(alarm, isChecked)
            }
        }

        // Настройка CheckBox в режиме выделения
        holder.checkBoxSelect.isChecked = selectedIds.contains(alarm.id)
        holder.checkBoxSelect.setOnClickListener {
            toggleSelection(alarm.id)
            notifyItemChanged(position)
        }

        // Обработка кликов
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(alarm.id)
                notifyItemChanged(position)
            } else {
                onItemClick?.invoke(alarm)
            }
        }

        // Обработка долгого нажатия
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                enterSelectionMode()
                toggleSelection(alarm.id)
                notifyItemChanged(position)
                onItemLongClick?.invoke(alarm)
                true
            } else {
                false
            }
        }

        // Устанавливаем ripple эффект только в обычном режиме
        if (!isSelectionMode) {
            setupRippleEffect(holder.itemView)
        } else {
            holder.itemView.apply {
                isClickable = true
                foreground = null
            }
        }
    }

    private fun setupRippleEffect(view: View) {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = context.obtainStyledAttributes(attrs)
        val backgroundResource = typedArray.getResourceId(0, 0)
        typedArray.recycle()

        view.apply {
            isClickable = true
            foreground = ContextCompat.getDrawable(context, backgroundResource)
        }
    }

    override fun getItemCount(): Int = alarms.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }

    // Методы для управления выделением
    fun enterSelectionMode() {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedIds.clear()
            notifyDataSetChanged()
            onSelectionModeChanged?.invoke(true)
            onSelectedCountChanged?.invoke(0)
        }
    }

    fun exitSelectionMode() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedIds.clear()
            notifyDataSetChanged()
            onSelectionModeChanged?.invoke(false)
            onSelectedCountChanged?.invoke(0)
        }
    }

    fun toggleSelection(alarmId: Long) {
        if (selectedIds.contains(alarmId)) {
            selectedIds.remove(alarmId)
        } else {
            selectedIds.add(alarmId)
        }
        onSelectedCountChanged?.invoke(selectedIds.size)
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(alarms.map { it.id })
        notifyDataSetChanged()
        onSelectedCountChanged?.invoke(selectedIds.size)
    }

    fun deselectAll() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectedCountChanged?.invoke(0)
    }

    fun getSelectedAlarms(): List<Alarm> {
        return alarms.filter { selectedIds.contains(it.id) }
    }

    fun isSelectionMode(): Boolean = isSelectionMode
    fun getSelectedCount(): Int = selectedIds.size
    fun isAllSelected(): Boolean = selectedIds.size == alarms.size
}