package com.example.soundcheck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogEntryAdapter(
    private var entries: MutableList<LogEntry>,
    private val onItemClick: (LogEntry) -> Unit
) : RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTime: TextView = view.findViewById(R.id.logDateTime)
        val dbLevel: TextView = view.findViewById(R.id.logDbLevel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.dateTime.text = entry.dateTime
        holder.dbLevel.text = "Peaked at ${entry.peakDb} decibels"
        holder.itemView.setOnClickListener { onItemClick(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateList(newEntries: List<LogEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }
}