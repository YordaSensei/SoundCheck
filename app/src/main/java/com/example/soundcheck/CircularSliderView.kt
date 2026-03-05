package com.example.soundcheck

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CircularSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue = 0
    var maxValue = 120
    var progress = 30
        set(value) {
            field = value.coerceIn(minValue, maxValue)
            onProgressChanged?.invoke(field)
            invalidate()
        }

    var onProgressChanged: ((Int) -> Unit)? = null

    // Start angle: 135° (bottom-left), sweep: 270°
    private val startAngle = 135f
    private val sweepAngle = 270f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#333333")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8B00FF")
    }

    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }

    private val oval = RectF()
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 40f
        radius = (min(w, h) / 2f) - padding
        centerX = w / 2f
        centerY = h / 2f
        oval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw track
        canvas.drawArc(oval, startAngle, sweepAngle, false, trackPaint)

        // Draw progress arc
        val fraction = (progress - minValue).toFloat() / (maxValue - minValue)
        val progressSweep = sweepAngle * fraction
        canvas.drawArc(oval, startAngle, progressSweep, false, progressPaint)

        // Draw thumb
        val thumbAngle = Math.toRadians((startAngle + progressSweep).toDouble())
        val thumbX = centerX + radius * cos(thumbAngle).toFloat()
        val thumbY = centerY + radius * sin(thumbAngle).toFloat()
        canvas.drawCircle(thumbX, thumbY, 22f, thumbPaint)
        canvas.drawCircle(thumbX, thumbY, 22f, thumbStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val angle = Math.toDegrees(
                    atan2((event.y - centerY).toDouble(), (event.x - centerX).toDouble())
                ).toFloat()

                // Normalize angle relative to startAngle
                var normalized = angle - startAngle
                if (normalized < 0) normalized += 360f

                // Ignore touches outside the arc sweep
                if (normalized > sweepAngle) return true

                val fraction = normalized / sweepAngle
                progress = (minValue + fraction * (maxValue - minValue)).toInt()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(300)
        setMeasuredDimension(size, size)
    }
}