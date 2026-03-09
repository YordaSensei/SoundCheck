package com.example.soundcheck

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.soundcheck.databinding.ActivityHelpFeaturesBinding

class HelpFeaturesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpFeaturesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpFeaturesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cardNoiseLimit.setOnClickListener {
            // Navigate to Noise Limit screen
        }

        binding.cardDuration.setOnClickListener {
            // Navigate to Duration screen
        }

        binding.cardLog.setOnClickListener {
            // Navigate to Log screen
        }
    }
}