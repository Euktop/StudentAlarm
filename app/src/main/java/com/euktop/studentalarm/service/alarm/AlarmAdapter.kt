package com.euktop.studentalarm.service.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.euktop.studentalarm.data.model.Alarm
import com.euktop.studentalarm.R

class AlarmAdapter(
    private val alarms: List<Alarm>,
    private val onDismiss: (Long) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.tvAlarmDescription)
        val dismissButton: Button = view.findViewById(R.id.btnDismissAlarm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm_in_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.description.text = alarm.description
        holder.dismissButton.setOnClickListener {
            onDismiss(alarm.id)
        }
    }

    override fun getItemCount() = alarms.size
}