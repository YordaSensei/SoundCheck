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
import com.google.android.material.button.MaterialButtonToggleGroup
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

        val slider            = findViewById<CircularSliderView>(R.id.circularSlider)
        val tvDbValue         = findViewById<TextView>(R.id.tvDbValue)
        val pickerHours       = findViewById<NumberPicker>(R.id.pickerHours)
        val pickerMinutes     = findViewById<NumberPicker>(R.id.pickerMinutes)
        val pickerSeconds     = findViewById<NumberPicker>(R.id.pickerSeconds)
        val tvSelectedDuration = findViewById<TextView>(R.id.tvSelectedDuration)
        val noiseLimitView    = findViewById<ConstraintLayout>(R.id.noiseLimitView)
        val durationView      = findViewById<ConstraintLayout>(R.id.durationView)
        val btnNoiseLimit     = findViewById<LinearLayout>(R.id.btnNoiseLimit)
        val btnDuration       = findViewById<LinearLayout>(R.id.btnDuration)
        val btnSaveSettings   = findViewById<Button>(R.id.btnSaveSettings)

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

                        loadLogs()
                    }
                }
            }
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

        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59
        pickerMinutes.setFormatter { String.format("%02d", it) }

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

        pickerHours.setOnValueChangedListener { _, _, _ -> updateDurationDisplay() }
        pickerMinutes.setOnValueChangedListener { _, _, _ -> updateDurationDisplay() }
        pickerSeconds.setOnValueChangedListener { _, _, _ -> updateDurationDisplay() }

        // -------- TAB TOGGLE --------
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

        selectDuration()

        btnNoiseLimit.setOnClickListener { selectNoiseLimit() }
        btnDuration.setOnClickListener { selectDuration() }

        // -------- FIREBASE INSTANCE --------
        val database = FirebaseDatabase.getInstance(
            "https://soundcheck-db-default-rtdb.asia-southeast1.firebasedatabase.app"
        )

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

                slider.setValue(thresholdDb)
                tvDbValue.text = thresholdDb.toString()

                val totalSeconds = (alarmMs / 1000L).toInt()

                pickerHours.value = totalSeconds / 3600
                pickerMinutes.value = (totalSeconds % 3600) / 60
                pickerSeconds.value = totalSeconds % 60

                updateDurationDisplay()
            }
    }

    // -------- LOAD LOGS FUNCTION --------
    private fun loadLogs() {

        Log.d(TAG, "Reading logs from Firebase...")

        logsRef.limitToLast(50)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    logHistoryContainer.removeAllViews()

                    val logs = snapshot.children.reversed()

                    for (logSnap in logs) {

                        try {

                            val log = logSnap.getValue(LogEntry::class.java)

                            if (log != null) {

                                val logText = TextView(this@DeviceDetailActivity)

                                logText.text = String.format(
                                    "%.1f dB • %s • %s",
                                    log.db,
                                    log.date,
                                    log.time
                                )

                                logText.textSize = 16f
                                logText.setPadding(16,16,16,16)

                                logHistoryContainer.addView(logText)
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing log: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to read logs: ${error.message}")
                }
            })
    }
}