package com.example.soundcheck

data class Device(
    val id: String,
    val name: String,
    val room: String,
    val isOnline: Boolean = true
)