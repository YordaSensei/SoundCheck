package com.example.soundcheck

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

data class LogEntry(
    var db: Float = 0f,
    var time: String = "",
    var date: String = ""
)

class DeviceDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SoundCheck_FB"
    }

    private lateinit var logsRef: DatabaseReference
    private lateinit var logHistoryContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        val deviceName = intent.getStringExtra("device_name") ?: "Unknown"
        val deviceRoom = intent.getStringExtra("device_room") ?: "Room"

        // -------- SETUP VIEW REFERENCES --------
        val controlContainer   = findViewById<ConstraintLayout>(R.id.controlContainer)
        logHistoryContainer    = findViewById(R.id.logHistoryContainer)

        val btnControlPanel    = findViewById<LinearLayout>(R.id.btnControlPanel)
        val btnLogHistory      = findViewById<LinearLayout>(R.id.btnLogHistory)
        val controlTxt         = findViewById<TextView>(R.id.controlTxt)
        val txtLog             = findViewById<TextView>(R.id.txtLog)

        findViewById<TextView>(R.id.detailDeviceName).text = deviceName
        findViewById<TextView>(R.id.detailDeviceRoom).text = deviceRoom
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val slider             = findViewById<CircularSliderView>(R.id.circularSlider)
        val tvDbValue          = findViewById<TextView>(R.id.tvDbValue)
        val pickerHours        = findViewById<NumberPicker>(R.id.pickerHours)
        val pickerMinutes      = findViewById<NumberPicker>(R.id.pickerMinutes)
        val pickerSeconds      = findViewById<NumberPicker>(R.id.pickerSeconds)
        val tvSelectedDuration = findViewById<TextView>(R.id.tvSelectedDuration)
        val noiseLimitView     = findViewById<ConstraintLayout>(R.id.noiseLimitView)
        val durationView       = findViewById<ConstraintLayout>(R.id.durationView)
        val btnNoiseLimit      = findViewById<LinearLayout>(R.id.btnNoiseLimit)
        val btnDuration        = findViewById<LinearLayout>(R.id.btnDuration)
        val btnSaveSettings    = findViewById<Button>(R.id.btnSaveSettings)

        // -------- LOG SETUP --------
        val logRecyclerView = findViewById<RecyclerView>(R.id.logRecyclerView)
        val tvEmptyLogs     = findViewById<TextView>(R.id.tvEmptyLogs)
        val logAdapter      = LogAdapter(mutableListOf())

        logRecyclerView.layoutManager = LinearLayoutManager(this)
        logRecyclerView.adapter = logAdapter

        // -------- TAB TOGGLE --------
        fun selectTab(
            selectedLayout: LinearLayout, selectedText: TextView,
            unselectedLayout: LinearLayout, unselectedText: TextView
        ) {
            selectedLayout.setBackgroundResource(R.drawable.white_semi_rounded)
            selectedText.setTextColor(Color.parseColor("#67058A"))

            unselectedLayout.setBackgroundResource(android.R.color.transparent)
            unselectedText.setTextColor(Color.WHITE)
        }

        // Default: Control tab selected
        selectTab(btnControlPanel, controlTxt, btnLogHistory, txtLog)

        btnControlPanel.setOnClickListener {
            selectTab(btnControlPanel, controlTxt, btnLogHistory, txtLog)
            controlContainer.visibility    = View.VISIBLE
            logHistoryContainer.visibility = View.GONE
        }

        btnLogHistory.setOnClickListener {
            selectTab(btnLogHistory, txtLog, btnControlPanel, controlTxt)
            controlContainer.visibility    = View.GONE
            logHistoryContainer.visibility = View.VISIBLE
        }

        // -------- CIRCULAR SLIDER --------
        slider.setRange(0, 119)
        slider.setValue(30)
        tvDbValue.text = "30"

        slider.onProgressChanged = { value ->
            tvDbValue.text = value.toString()
        }

        // -------- INITIALIZE NUMBER PICKERS --------
        pickerHours.minValue = 0
        pickerHours.maxValue = 23
        pickerHours.setFormatter { String.format("%02d", it) }
        pickerHours.value = 0

        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59
        pickerMinutes.setFormatter { String.format("%02d", it) }
        pickerMinutes.value = 0

        pickerSeconds.minValue = 0
        pickerSeconds.maxValue = 59
        pickerSeconds.setFormatter { String.format("%02d", it) }
        pickerSeconds.value = 10

        // -------- UPDATE DURATION DISPLAY --------
        val updateDurationDisplay = {
            tvSelectedDuration.text = String.format(
                "%02d:%02d:%02d",
                pickerHours.value,
                pickerMinutes.value,
                pickerSeconds.value
            )
        }

        updateDurationDisplay()

        pickerHours.setOnValueChangedListener   { _, _, _ -> updateDurationDisplay() }
        pickerMinutes.setOnValueChangedListener { _, _, _ -> updateDurationDisplay() }
        pickerSeconds.setOnValueChangedListener { _, _, _ -> updateDurationDisplay() }

        // -------- CONTROL PANEL TAB TOGGLE --------
        fun selectNoiseLimit() {
            noiseLimitView.visibility = View.VISIBLE
            durationView.visibility   = View.GONE
            btnNoiseLimit.alpha = 1f
            btnDuration.alpha   = 0.5f
        }

        fun selectDuration() {
            noiseLimitView.visibility = View.GONE
            durationView.visibility   = View.VISIBLE
            btnNoiseLimit.alpha = 0.5f
            btnDuration.alpha   = 1f
        }

        selectNoiseLimit()

        btnNoiseLimit.setOnClickListener { selectNoiseLimit() }
        btnDuration.setOnClickListener   { selectDuration() }

        // -------- FIREBASE INSTANCE --------
        val database    = FirebaseDatabase.getInstance("https://soundcheck-db-default-rtdb.asia-southeast1.firebasedatabase.app")
        val settingsRef = database.getReference("settings")
        logsRef         = database.getReference("logs")

        // -------- FIREBASE READ SETTINGS --------
        Log.d(TAG, "Reading settings from Firebase...")

        settingsRef.get()
            .addOnSuccessListener { snapshot ->
                val thresholdDb = when (val value = snapshot.child("threshold_db").getValue()) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 30
                    else -> 30
                }
                val alarmMs = when (val value = snapshot.child("alarm_duration_ms").getValue()) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 10000L
                    else -> 10000L
                }

                Log.d(TAG, "Settings loaded → threshold_db=$thresholdDb | alarm_duration_ms=$alarmMs")

                slider.setValue(thresholdDb)
                tvDbValue.text = thresholdDb.toString()

                val totalSeconds = (alarmMs / 1000L).toInt()
                pickerHours.value   = totalSeconds / 3600
                pickerMinutes.value = (totalSeconds % 3600) / 60
                pickerSeconds.value = totalSeconds % 60

                updateDurationDisplay()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load settings: ${e.message}")
            }

        // -------- FIREBASE WRITE ON SAVE --------
        btnSaveSettings.setOnClickListener {
            val thresholdDb     = slider.getValue()
            val h               = pickerHours.value
            val m               = pickerMinutes.value
            val s               = pickerSeconds.value
            val alarmDurationMs = ((h * 3600L) + (m * 60L) + s) * 1000L

            Log.d(TAG, "Saving → threshold_db=$thresholdDb | alarm_duration_ms=$alarmDurationMs")

            val updates = mapOf(
                "settings/threshold_db"      to thresholdDb,
                "settings/alarm_duration_ms" to alarmDurationMs
            )

            database.reference.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Save successful! threshold_db=$thresholdDb | alarm_duration_ms=$alarmDurationMs")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Save FAILED: ${e.message}")
                }
        }

        // -------- FIREBASE READ LOGS --------
        fun loadLogs() {
            Log.d(TAG, "Reading logs from Firebase...")

            logsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logs = snapshot.children
                        .mapNotNull { it.getValue(LogEntry::class.java) }
                        .reversed()

                    Log.d(TAG, "Logs loaded: ${logs.size} entries")

                    if (logs.isEmpty()) {
                        tvEmptyLogs.visibility     = View.VISIBLE
                        logRecyclerView.visibility = View.GONE
                    } else {
                        tvEmptyLogs.visibility     = View.GONE
                        logRecyclerView.visibility = View.VISIBLE
                        logAdapter.updateLogs(logs)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to read logs: ${error.message}")
                }
            })
        }

        loadLogs()
    }
}