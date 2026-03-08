package com.example.soundcheck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogAdapter(private val logs: MutableList<LogEntry>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logDateTime: TextView = view.findViewById(R.id.logDateTime)
        val logDbLevel: TextView  = view.findViewById(R.id.logDbLevel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LogViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_log_entry, parent, false))

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.logDateTime.text = "${log.date} | ${log.time}"
        holder.logDbLevel.text  = "Peaked at ${log.db.toInt()} decibels"
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<LogEntry>) {
        logs.clear()
        logs.addAll(newLogs)
        notifyDataSetChanged()
    }
}