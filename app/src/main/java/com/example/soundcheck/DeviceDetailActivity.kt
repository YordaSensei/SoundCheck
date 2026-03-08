package com.example.soundcheck

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.database.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class LogEntry(
    var db: Float = 0f,
    var time: String = "",
    var date: String = ""
)

class DeviceDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SoundCheck_FB"
    }

    // ----- CLASS LEVEL VARIABLES -----
    private lateinit var logsRef: DatabaseReference
    private lateinit var logHistoryContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        val deviceName = intent.getStringExtra("device_name") ?: "Unknown"
        val deviceRoom = intent.getStringExtra("device_room") ?: "Room"

        // -------- SETUP VIEW REFERENCES --------
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val controlContainer = findViewById<ConstraintLayout>(R.id.controlContainer)
        logHistoryContainer = findViewById(R.id.logHistoryContainer)

        val btnControl = findViewById<Button>(R.id.btnControlPanel)
        val btnLog = findViewById<Button>(R.id.btnLogHistory)

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
        fun selectTab(selectedBtn: Button, unselectedBtn: Button) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val fraction = it.animatedFraction

                    selectedBtn.backgroundTintList = ColorStateList.valueOf(
                        ArgbEvaluator().evaluate(fraction, Color.TRANSPARENT, Color.WHITE) as Int
                    )

                    unselectedBtn.backgroundTintList = ColorStateList.valueOf(
                        ArgbEvaluator().evaluate(fraction, Color.WHITE, Color.TRANSPARENT) as Int
                    )

                    selectedBtn.setTextColor(
                        ArgbEvaluator().evaluate(
                            fraction,
                            Color.WHITE,
                            Color.parseColor("#8B00FF")
                        ) as Int
                    )

                    unselectedBtn.setTextColor(
                        ArgbEvaluator().evaluate(
                            fraction,
                            Color.parseColor("#8B00FF"),
                            Color.WHITE
                        ) as Int
                    )
                }
            }.start()
        }

        selectTab(btnControl, btnLog)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnControlPanel -> {
                        selectTab(btnControl, btnLog)
                        controlContainer.visibility = View.VISIBLE
                        logHistoryContainer.visibility = View.GONE
                    }
                    R.id.btnLogHistory -> {
                        selectTab(btnLog, btnControl)
                        controlContainer.visibility = View.GONE
                        logHistoryContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        // -------- CIRCULAR SLIDER --------
        slider.setRange(0, 119)
        slider.setValue(30)       // Default: 30 dB
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
        pickerSeconds.value = 10  // Default: 10 seconds

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

        // Default tab matches XML visibility="visible" on durationView
        selectDuration()

        btnNoiseLimit.setOnClickListener { selectNoiseLimit() }
        btnDuration.setOnClickListener   { selectDuration() }

        // -------- FIREBASE INSTANCE --------
        // Specify the database URL explicitly to ensure correct region (asia-southeast1)
        val database    = FirebaseDatabase.getInstance("https://soundcheck-db-default-rtdb.asia-southeast1.firebasedatabase.app")
        val settingsRef = database.getReference("settings")
        logsRef = database.getReference("logs")

        // -------- FIREBASE READ ON ACTIVITY START --------
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

                // Populate slider
                slider.setValue(thresholdDb)
                tvDbValue.text = thresholdDb.toString()

                // Populate pickers from milliseconds
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