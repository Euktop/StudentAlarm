package com.euktop.studentalarm.ui.alarms

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
import com.euktop.studentalarm.ui.main.MainActivity
import com.euktop.studentalarm.R
import com.euktop.studentalarm.data.model.Alarm
import com.euktop.studentalarm.utils.animation.EffectHelper
import com.euktop.studentalarm.utils.permission.PermissionManager

class AlarmRecyclerAdapter(
    private val context: Context
) : RecyclerView.Adapter<AlarmRecyclerAdapter.ViewHolder>() {

    private var alarms: List<Alarm> = emptyList()

    var onSwitchChanged: ((alarm: Alarm, isChecked: Boolean) -> Unit)? = null
    var onItemClick: ((alarm: Alarm) -> Unit)? = null
    var onAlarmSelected: ((alarmId: Long) -> Unit)? = null
    var onSelectionModeRequested: (() -> Unit)? = null

    var isSelectionMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedIds: Set<Long> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

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

        if (isSelectionMode) {
            holder.checkBoxSelect.visibility = View.VISIBLE
            holder.isEnabledAlarmSwitch.visibility = View.GONE
        } else {
            holder.checkBoxSelect.visibility = View.GONE
            holder.isEnabledAlarmSwitch.visibility = View.VISIBLE
        }

        holder.checkBoxSelect.isChecked = selectedIds.contains(alarm.id)
        holder.checkBoxSelect.setOnClickListener {
            onAlarmSelected?.invoke(alarm.id)
        }

        if (!isSelectionMode) {
            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener(null)
            holder.isEnabledAlarmSwitch.isChecked = alarm.isEnabled

            val hasAllPermissions = PermissionManager.hasAllAlarmPermissions(context)

            if (!hasAllPermissions) {
                holder.isEnabledAlarmSwitch.isEnabled = false
                holder.isEnabledAlarmSwitch.alpha = 0.5f

                if (alarm.isEnabled) {
                    holder.isEnabledAlarmSwitch.isChecked = false
                }
            } else {
                holder.isEnabledAlarmSwitch.isEnabled = true
                holder.isEnabledAlarmSwitch.alpha = 1f
            }

            holder.isEnabledAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !PermissionManager.hasAllAlarmPermissions(context)) {
                    holder.isEnabledAlarmSwitch.isChecked = false
                    showPermissionsRequiredDialog()
                    return@setOnCheckedChangeListener
                }
                onSwitchChanged?.invoke(alarm, isChecked)
            }
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                onAlarmSelected?.invoke(alarm.id)
            } else {
                onItemClick?.invoke(alarm)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                onSelectionModeRequested?.invoke()
                onAlarmSelected?.invoke(alarm.id)
                true
            } else {
                false
            }
        }

        EffectHelper.setupRippleEffect(holder.itemView)

        holder.itemView.isClickable = true
        holder.itemView.isLongClickable = true
    }

    override fun getItemCount(): Int = alarms.size

    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }

    private fun showPermissionsRequiredDialog() {
        if (context is Activity) {
            val activity = context
            if (activity is MainActivity) {
                activity.checkPermissionsAndExecute {
                }
            } else {
                PermissionManager.showAllPermissionsDialog(activity)
            }
        }
    }
}