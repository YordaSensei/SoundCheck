package com.example.soundcheck

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
        val btnLogout = findViewById<ImageView>(R.id.btnLogout)

        val btnRooms       = findViewById<LinearLayout>(R.id.btnRooms)
        val btnDevices     = findViewById<LinearLayout>(R.id.btnDevices)
        val tvRooms        = findViewById<TextView>(R.id.tvRooms)
        val tvDevices      = findViewById<TextView>(R.id.tvDevices)
        val roomRecycler   = findViewById<RecyclerView>(R.id.roomRecycler)
        val deviceRecycler = findViewById<RecyclerView>(R.id.deviceRecycler)

        // -------- BOTTOM NAV --------
        fun showHome() {
            homeTab.visibility    = View.VISIBLE
            profileTab.visibility = View.GONE
            navHome.setBackgroundResource(R.drawable.white_semi_rounded)
            navHomeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#67058A"))
            navProfile.setBackgroundResource(android.R.color.transparent)
            navProfileIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }

        fun showProfile() {
            homeTab.visibility    = View.GONE
            profileTab.visibility = View.VISIBLE
            navHome.setBackgroundResource(android.R.color.transparent)
            navHomeIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
            navProfile.setBackgroundResource(R.drawable.white_semi_rounded)
            navProfileIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#67058A"))
        }

        navHome.setOnClickListener    { showHome() }
        navProfile.setOnClickListener { showProfile() }
        showHome()

        // -------- LOAD USERNAME --------
        val tvUsername = findViewById<TextView>(R.id.tvUsername)

        val database   = FirebaseDatabase.getInstance("https://soundcheck-db-default-rtdb.asia-southeast1.firebasedatabase.app")
        val accountRef = database.getReference("account")

        accountRef.child("username").get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.getValue(String::class.java) ?: "Username"
                tvUsername.text = username
            }
            .addOnFailureListener {
                tvUsername.text = "Username"
            }

        // -------- PROFILE MENU --------
        val menuAccountInfo = findViewById<LinearLayout>(R.id.menuAccountInfo)
        val menuManageRooms = findViewById<LinearLayout>(R.id.menuManageRooms)
        val menuHelp        = findViewById<LinearLayout>(R.id.menuHelp)

        menuAccountInfo.setOnClickListener {
            startActivity(Intent(this, AccInformationActivity::class.java))
        }

        menuManageRooms.setOnClickListener {
            // Soon
        }

        menuHelp.setOnClickListener {
            startActivity(Intent(this, HelpFeaturesActivity::class.java))
        }

        btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

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

        selectTab(btnRooms, tvRooms, btnDevices, tvDevices)

        // -------- DATA --------
        val allDevices = mutableListOf(
            Device("1", "SoundCheck #1", "7th Floor")
        )

        val rooms = listOf(
            Room("1", "7th Floor")
        )

        // -------- DEVICE ADAPTER --------
        val deviceAdapter = DeviceAdapter(allDevices) { device ->
            val intent = Intent(this, DeviceDetailActivity::class.java).apply {
                putExtra("device_name", device.name)
                putExtra("device_room", device.room)
            }
            startActivity(intent)
        }

        // -------- ROOM ADAPTER --------
        val roomAdapter = RoomAdapter(rooms) { room ->
            val filtered = allDevices.filter { it.room == room.name }
            deviceAdapter.updateDevices(filtered)
            roomRecycler.visibility   = View.GONE
            deviceRecycler.visibility = View.VISIBLE
        }

        // -------- TAB CLICKS --------
        btnRooms.setOnClickListener {
            selectTab(btnRooms, tvRooms, btnDevices, tvDevices)
            roomRecycler.visibility   = View.VISIBLE
            deviceRecycler.visibility = View.GONE
        }

        btnDevices.setOnClickListener {
            selectTab(btnDevices, tvDevices, btnRooms, tvRooms)
            roomRecycler.visibility   = View.GONE
            deviceRecycler.visibility = View.VISIBLE
        }

        // -------- RECYCLERS --------
        roomRecycler.layoutManager = GridLayoutManager(this, 2)
        roomRecycler.adapter = roomAdapter

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