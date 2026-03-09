package com.example.soundcheck

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
            toggleExpand(binding.tvNoiseLimitDesc)
        }

        binding.cardDuration.setOnClickListener {
            toggleExpand(binding.tvDurationDesc)
        }

        binding.cardLog.setOnClickListener {
            toggleExpand(binding.tvLogDesc)
        }
    }

    private fun toggleExpand(descView: TextView) {
        if (descView.visibility == View.GONE) {
            expandView(descView)
        } else {
            collapseView(descView)
        }
    }

    private fun expandView(view: TextView) {
        // Use parent width for measurement — view.width is 0 before first layout
        val parentWidth = (view.parent as View).width
        view.visibility = View.VISIBLE
        view.measure(
            View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight

        // Start from 0
        view.layoutParams.height = 0
        view.requestLayout()

        ValueAnimator.ofInt(0, targetHeight).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                view.layoutParams.height = anim.animatedValue as Int
                view.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    view.requestLayout()
                }
            })
            start()
        }
    }

    private fun collapseView(view: TextView) {
        val initialHeight = view.measuredHeight

        ValueAnimator.ofInt(initialHeight, 0).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                view.layoutParams.height = anim.animatedValue as Int
                view.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    view.requestLayout()
                }
            })
            start()
        }
    }
}