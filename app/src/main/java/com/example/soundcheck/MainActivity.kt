package com.example.soundcheck

import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        // -------- VIEW REFERENCES --------
        val homeTab        = findViewById<ConstraintLayout>(R.id.homeTab)
        val profileTab     = findViewById<ConstraintLayout>(R.id.profileTab)
        val navHome        = findViewById<LinearLayout>(R.id.navHome)
        val navProfile     = findViewById<LinearLayout>(R.id.navProfile)
        val navHomeIcon    = findViewById<ImageView>(R.id.navHomeIcon)
        val navProfileIcon = findViewById<ImageView>(R.id.navProfileIcon)

        val toggleGroup    = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val btnRooms       = findViewById<Button>(R.id.btnRooms)
        val btnDevices     = findViewById<Button>(R.id.btnDevices)
        val roomRecycler   = findViewById<RecyclerView>(R.id.roomRecycler)
        val deviceRecycler = findViewById<RecyclerView>(R.id.deviceRecycler)

        // -------- BOTTOM NAV --------
        fun showHome() {
            homeTab.visibility    = View.VISIBLE
            profileTab.visibility = View.GONE
            navHomeIcon.imageTintList    = ColorStateList.valueOf(Color.WHITE)
            navProfileIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#8B00FF"))
        }

        fun showProfile() {
            homeTab.visibility    = View.GONE
            profileTab.visibility = View.VISIBLE
            navHomeIcon.imageTintList    = ColorStateList.valueOf(Color.parseColor("#8B00FF"))
            navProfileIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }

        navHome.setOnClickListener    { showHome() }
        navProfile.setOnClickListener { showProfile() }
        showHome()

        // -------- PROFILE MENU --------
        val menuAccountInfo  = findViewById<LinearLayout>(R.id.menuAccountInfo)
        val menuManageRooms  = findViewById<LinearLayout>(R.id.menuManageRooms)
        val menuHelp         = findViewById<LinearLayout>(R.id.menuHelp)

        menuAccountInfo.setOnClickListener {
            // TODO: navigate to Account Information screen
        }

        menuManageRooms.setOnClickListener {
            // TODO: navigate to Manage Rooms & Devices screen
        }

        menuHelp.setOnClickListener {
            startActivity(Intent(this, HelpFeaturesActivity::class.java))
        }

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
                        ArgbEvaluator().evaluate(fraction, Color.WHITE, Color.parseColor("#8B00FF")) as Int
                    )
                    unselectedBtn.setTextColor(
                        ArgbEvaluator().evaluate(fraction, Color.parseColor("#8B00FF"), Color.WHITE) as Int
                    )
                }
            }.start()
        }

        selectTab(btnRooms, btnDevices)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnRooms -> {
                        selectTab(btnRooms, btnDevices)
                        roomRecycler.visibility   = View.VISIBLE
                        deviceRecycler.visibility = View.GONE
                    }
                    R.id.btnDevices -> {
                        selectTab(btnDevices, btnRooms)
                        roomRecycler.visibility   = View.GONE
                        deviceRecycler.visibility = View.VISIBLE
                    }
                }
            }
        }

        // -------- ROOMS RECYCLER --------
        roomRecycler.layoutManager = GridLayoutManager(this, 2)

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
        roomRecycler.adapter = deviceAdapter

        // -------- DEVICES RECYCLER --------
        deviceRecycler.layoutManager = GridLayoutManager(this, 2)
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