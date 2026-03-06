package com.example.soundcheck

import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val controlPanelContainer = findViewById<ScrollView>(R.id.controlPanelContainer)
        //val logHistoryContainer = findViewById<LinearLayout>(R.id.logHistoryContainer)
        val btnControl = findViewById<Button>(R.id.btnControlPanel)
        val btnLog = findViewById<Button>(R.id.btnLogHistory)

        fun selectTab(selectedBtn: Button, unselectedBtn: Button) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val fraction = it.animatedFraction

                    // Use backgroundTintList instead of setting background directly
                    selectedBtn.backgroundTintList = ColorStateList.valueOf(
                        ArgbEvaluator().evaluate(fraction, Color.TRANSPARENT, Color.WHITE) as Int
                    )
                    unselectedBtn.backgroundTintList = ColorStateList.valueOf(
                        ArgbEvaluator().evaluate(fraction, Color.WHITE, Color.TRANSPARENT) as Int
                    )

                    selectedBtn.setTextColor(
                        ArgbEvaluator().evaluate(fraction, Color.WHITE, Color.parseColor("#8B00FF")) as Int
                    )
                    unselectedBtn.setTextColor(
                        ArgbEvaluator().evaluate(fraction, Color.parseColor("#8B00FF"), Color.WHITE) as Int
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
                        controlPanelContainer.visibility = View.VISIBLE
                        //logHistoryContainer.visibility = View.GONE
                    }
                    R.id.btnLogHistory -> {
                        selectTab(btnLog, btnControl)
                        controlPanelContainer.visibility = View.GONE
                        //logHistoryContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        val deviceRecycler = findViewById<RecyclerView>(R.id.roomRecycler)
        deviceRecycler.layoutManager = GridLayoutManager(this, 2)

        val devices = listOf(
            Device("1", "SoundCheck #1", "7th Floor"),
        )

        val deviceAdapter = DeviceAdapter(devices) { device ->
            val intent = Intent(this, DeviceDetailActivity::class.java).apply {
                putExtra("device_name", device.name)
                putExtra("device_room", device.room)
            }
            startActivity(intent)
        }
        deviceRecycler.adapter = deviceAdapter
    }

    private fun bindSliderToInput(seekBar: SeekBar, editText: TextInputEditText) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) editText.setText(progress.toString())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    val value = input.toIntOrNull() ?: 0
                    if (value <= seekBar.max) seekBar.progress = value
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}