package com.example.soundcheck

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        //initialization
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val controlPanelView = findViewById<View>(R.id.controlPanelContainer)
        val logHistoryView = findViewById<View>(R.id.logHistoryContainer)

        // Set default
        toggleGroup.check(R.id.btnControlPanel)
        logHistoryView.visibility = View.GONE

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnControlPanel -> {
                        controlPanelView.visibility = View.VISIBLE
                        logHistoryView.visibility = View.GONE
                    }
                    R.id.btnLogHistory -> {
                        controlPanelView.visibility = View.GONE
                        logHistoryView.visibility = View.VISIBLE
                    }
                }
            }
        }

        // --- Microphone Threshold ---
        val threshSlider = findViewById<SeekBar>(R.id.threshSeekBar)
        val threshEditText = findViewById<TextInputEditText>(R.id.threshValueInput)
        val threshSaveButton = findViewById<Button>(R.id.threshSaveBtn)
        bindSliderToInput(threshSlider, threshEditText)
        threshSaveButton.setOnClickListener {
            val value = threshSlider.progress
            //Firebase
            // database.child("settings").child("threshold").setValue(value)
            Toast.makeText(this, "Microphone threshold set to $value dB", Toast.LENGTH_SHORT).show()
        }

        // --- Timer ---
        val timerSlider = findViewById<SeekBar>(R.id.timerSeekBar)
        val timerEditText = findViewById<TextInputEditText>(R.id.timerValueInput)
        val timerSaveButton = findViewById<Button>(R.id.timerSaveBtn)
        bindSliderToInput(timerSlider, timerEditText)
        timerSaveButton.setOnClickListener {
            val value = timerSlider.progress
            //Firebase
            // database.child("settings").child("timer").setValue(value)
            Toast.makeText(this, "Timer set to $value secs", Toast.LENGTH_SHORT).show()
        }

        // --- Speaker Volume ---
        val volumeSlider = findViewById<SeekBar>(R.id.volumeSeekBar)
        val volumeEditText = findViewById<TextInputEditText>(R.id.volumeValueInput)
        val volumeSaveButton = findViewById<Button>(R.id.volumeSaveBtn)
        bindSliderToInput(volumeSlider, volumeEditText)
        volumeSaveButton.setOnClickListener {
            val value = volumeSlider.progress
            //Firebase
            // database.child("settings").child("volume").setValue(value)
            Toast.makeText(this, "Speaker volume set to $value dB", Toast.LENGTH_SHORT).show()
        }

        // --- Log/History Setup ---
        val recyclerView = findViewById<RecyclerView>(R.id.logRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

// Dummy data for now — replace with Firebase fetch later
        val logEntries = mutableListOf(
            LogEntry("02-28-2026 | 13:00:12", 67),
            LogEntry("02-28-2026 | 12:45:03", 72),
            LogEntry("02-28-2026 | 11:30:55", 58),
            LogEntry("02-27-2026 | 09:15:44", 81),
        )

        val adapter = LogEntryAdapter(logEntries) { entry ->
            // Handle selection
            Toast.makeText(this, "Tapped: ${entry.dateTime}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

// Sort toggle (newest/oldest)
        var sortedNewest = true
        findViewById<ImageButton>(R.id.btnSort).setOnClickListener {
            sortedNewest = !sortedNewest
            val sorted = if (sortedNewest)
                logEntries.sortedByDescending { it.dateTime }
            else
                logEntries.sortedBy { it.dateTime }
            adapter.updateList(sorted)
        }

// Date filter chip
        findViewById<Chip>(R.id.chipFilterDate).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show DatePickerDialog and filter by selected date
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Filter by date")
                    .build()
                datePicker.addOnPositiveButtonClickListener { selection ->
                    val sdf = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
                    val selectedDate = sdf.format(Date(selection))
                    adapter.updateList(logEntries.filter { it.dateTime.startsWith(selectedDate) })
                }
                datePicker.show(supportFragmentManager, "DATE_PICKER")
            } else {
                adapter.updateList(logEntries)
            }
        }

// dB filter chip
        findViewById<Chip>(R.id.chipFilterDb).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Filter entries above threshold, e.g. >70dB
                adapter.updateList(logEntries.filter { it.peakDb > 70 })
            } else {
                adapter.updateList(logEntries)
            }
        }
    }

    // Reusable function to sync a SeekBar and TextInputEditText together
    private fun bindSliderToInput(seekBar: SeekBar, editText: TextInputEditText) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    editText.setText(progress.toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    val value = input.toIntOrNull() ?: 0
                    if (value <= seekBar.max) {
                        seekBar.progress = value
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}