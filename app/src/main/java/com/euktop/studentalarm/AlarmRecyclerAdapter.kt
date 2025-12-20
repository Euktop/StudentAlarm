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
    private val context: Context,
    private var alarms: List<Alarm> = emptyList()
) : RecyclerView.Adapter<AlarmRecyclerAdapter.ViewHolder>() {

    var onSwitchChanged: ((alarm: Alarm, isChecked: Boolean) -> Unit)? = null
    var onItemClick: ((alarm: Alarm) -> Unit)? = null
    var onItemLongClick: ((alarm: Alarm) -> Unit)? = null

    var onSelectionModeChanged: ((isSelectionMode: Boolean) -> Unit)? = null
    var onSelectedCountChanged: ((count: Int) -> Unit)? = null

    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<Long>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBoxSelect: CheckBox = view.findViewById(R.id.checkBoxSelect)
        val timeTextView: TextView = view.findViewById(R.id.TimeTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.DescriptionTextView)
        val repetitionRateTextView: TextView = view.findViewById(R.id.RepetitionRateTextView)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val isEnabledAlarmSwitch: Switch = view.findViewById(R.id.IsEnabledAlarmSwitch)
        val itemLayout: View = view.findViewById(R.id.itemLayout)
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

        if (isSelectionMode) {
            holder.checkBoxSelect.visibility = View.VISIBLE
            holder.isEnabledAlarmSwitch.visibility = View.GONE
        } else {
            holder.checkBoxSelect.visibility = View.GONE
            holder.isEnabledAlarmSwitch.visibility = View.VISIBLE
        }

        if (!isSelectionMode) {
            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener(null)
            holder.isEnabledAlarmSwitch.isChecked = alarm.isEnabled

            // Проверяем все необходимые разрешения для будильника
            val hasAllPermissions = PermissionManager.hasAllAlarmPermissions(context)

            if (!hasAllPermissions) {
                // Блокируем переключатель, если нет всех разрешений
                holder.isEnabledAlarmSwitch.isEnabled = false
                holder.isEnabledAlarmSwitch.alpha = 0.5f

                // Принудительно отключаем будильник в UI, если он включен
                if (alarm.isEnabled) {
                    holder.isEnabledAlarmSwitch.isChecked = false
                }
            } else {
                holder.isEnabledAlarmSwitch.isEnabled = true
                holder.isEnabledAlarmSwitch.alpha = 1f
            }

            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Проверяем разрешения перед включением будильника
                if (isChecked && !PermissionManager.hasAllAlarmPermissions(context)) {
                    // Разрешений нет, не даем включить
                    holder.isEnabledAlarmSwitch.isChecked = false
                    showPermissionsRequiredDialog()
                    return@setOnCheckedChangeListener
                }
                onSwitchChanged?.invoke(alarm, isChecked)
            }
        }

        holder.checkBoxSelect.isChecked = selectedIds.contains(alarm.id)
        holder.checkBoxSelect.setOnClickListener {
            toggleSelection(alarm.id)
            notifyItemChanged(position)
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(alarm.id)
                notifyItemChanged(position)
            } else {
                onItemClick?.invoke(alarm)
            }
        }

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

        setupRippleEffect(holder.itemView)

        holder.itemView.isClickable = true
        holder.itemView.isLongClickable = true
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
    fun isAllSelected(): Boolean = selectedIds.size == alarms.size

    // ==================== PERMISSION DIALOG ====================
    private fun showPermissionsRequiredDialog() {
        if (context is Activity) {
            val activity = context as Activity
            if (activity is MainActivity) {
                activity.checkAlarmPermissionsAndExecute {
                    // Callback после показа диалога
                }
            } else {
                PermissionManager.showAllPermissionsDialog(activity)
            }
        }
    }
}