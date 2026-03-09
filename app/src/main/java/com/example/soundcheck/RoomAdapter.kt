package com.example.soundcheck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class RoomAdapter(
    private val rooms: List<Room>,
    private val onClick: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.roomCard)
        val name: TextView = view.findViewById(R.id.roomName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_room_card, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]
        holder.name.text = room.name
        holder.card.setOnClickListener { onClick(room) }
    }

    override fun getItemCount() = rooms.size
}