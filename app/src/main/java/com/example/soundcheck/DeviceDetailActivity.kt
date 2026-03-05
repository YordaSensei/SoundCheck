package com.example.soundcheck

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class DeviceDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        val deviceName = intent.getStringExtra("device_name") ?: "Unknown"
        val deviceRoom = intent.getStringExtra("device_room") ?: "Room"

        findViewById<TextView>(R.id.detailDeviceName).text = deviceName
        findViewById<TextView>(R.id.detailDeviceRoom).text = deviceRoom
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // --- Noise Limit ---
        val slider = findViewById<CircularSliderView>(R.id.circularSlider)
        val tvDbValue = findViewById<TextView>(R.id.tvDbValue)
        slider.onProgressChanged = { value -> tvDbValue.text = value.toString() }

        // --- Duration pickers ---
        val pickerHours = findViewById<NumberPicker>(R.id.pickerHours)
        val pickerMinutes = findViewById<NumberPicker>(R.id.pickerMinutes)
        val pickerSeconds = findViewById<NumberPicker>(R.id.pickerSeconds)
        val tvSelectedDuration = findViewById<TextView>(R.id.tvSelectedDuration)

        pickerHours.minValue = 0
        pickerHours.maxValue = 99
        pickerHours.setFormatter { String.format("%02d", it) }

        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59
        pickerMinutes.setFormatter { String.format("%02d", it) }

        pickerSeconds.minValue = 0
        pickerSeconds.maxValue = 59
        pickerSeconds.setFormatter { String.format("%02d", it) }

        val updateDuration = {
            tvSelectedDuration.text = String.format(
                "%02d:%02d:%02d",
                pickerHours.value,
                pickerMinutes.value,
                pickerSeconds.value
            )
        }
        pickerHours.setOnValueChangedListener { _, _, _ -> updateDuration() }
        pickerMinutes.setOnValueChangedListener { _, _, _ -> updateDuration() }
        pickerSeconds.setOnValueChangedListener { _, _, _ -> updateDuration() }

        // --- Tab switching ---
        val noiseLimitView = findViewById<ConstraintLayout>(R.id.noiseLimitView)
        val durationView = findViewById<ConstraintLayout>(R.id.durationView)
        val btnNoiseLimit = findViewById<LinearLayout>(R.id.btnNoiseLimit)
        val btnDuration = findViewById<LinearLayout>(R.id.btnDuration)

        fun selectNoiseLimit() {
            noiseLimitView.visibility = View.VISIBLE
            durationView.visibility = View.GONE
            btnNoiseLimit.alpha = 1f
            btnDuration.alpha = 0.5f
        }

        fun selectDuration() {
            noiseLimitView.visibility = View.GONE
            durationView.visibility = View.VISIBLE
            btnNoiseLimit.alpha = 0.5f
            btnDuration.alpha = 1f
        }

        selectNoiseLimit()
        btnNoiseLimit.setOnClickListener { selectNoiseLimit() }
        btnDuration.setOnClickListener { selectDuration() }

        // --- Save ---
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val noiseLimit = slider.progress
            val totalSeconds = (pickerHours.value * 3600) +
                    (pickerMinutes.value * 60) +
                    pickerSeconds.value
            // firestore.collection("devices").document(deviceId)
            //     .update(mapOf("noiseLimit" to noiseLimit, "duration" to totalSeconds))
            finish()
        }
    }
}