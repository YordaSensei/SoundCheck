package com.example.soundcheck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class DeviceAdapter(
    private val devices: MutableList<Device>,
    private val onDeviceClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.deviceCard)
        val name: TextView = view.findViewById(R.id.deviceName)
        val room: TextView = view.findViewById(R.id.deviceRoom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.name.text = device.name
        holder.room.text = device.room
        holder.card.setOnClickListener { onDeviceClick(device) }

        holder.card.strokeColor = if (device.id == selectedDeviceId)
            0xFF8B00FF.toInt() else android.graphics.Color.TRANSPARENT
    }

    override fun getItemCount() = devices.size

    private var selectedDeviceId: String? = null

    fun setSelected(deviceId: String) {
        selectedDeviceId = deviceId
        notifyDataSetChanged()
    }

    fun updateDevices(newDevices: List<Device>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }
}