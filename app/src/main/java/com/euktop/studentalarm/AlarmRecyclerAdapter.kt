package com.euktop.studentalarm

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.TimeTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.DescriptionTextView)
        val repetitionRateTextView: TextView = view.findViewById(R.id.RepetitionRateTextView)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
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

        holder.isEnabledAlarmSwitch.setOnCheckedChangeListener(null)
        holder.isEnabledAlarmSwitch.isChecked = alarm.isEnabled

        holder.isEnabledAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged?.invoke(alarm, isChecked)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(alarm)
        }

        setupRippleEffect(holder.itemView)
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
}