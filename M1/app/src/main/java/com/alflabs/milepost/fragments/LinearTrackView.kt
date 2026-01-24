package com.alflabs.milepost.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LinearTrackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val stationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val trainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var startStationName: String = "Start"
        set(value) {
            field = value
            invalidate()
        }

    var midStationName: String = "Middle"
        set(value) {
            field = value
            invalidate()
        }

    var endStationName: String = "End"
        set(value) {
            field = value
            invalidate()
        }

    private val trainSymbol = "\uD83D\uDE86" // 🚆

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = paddingLeft.toFloat()
        val paddingRight = paddingRight.toFloat()
        val paddingTop = paddingTop.toFloat()
        val paddingBottom = paddingBottom.toFloat()

        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom

        val centerY = paddingTop + height / 2f
        val startX = paddingLeft + 40f
        val endX = width + paddingLeft - 40f
        val midX = (startX + endX) / 2f

        // Draw horizontal line
        canvas.drawLine(startX, centerY, endX, centerY, linePaint)

        // Draw station markers
        val stationRadius = 10f
        canvas.drawCircle(startX, centerY, stationRadius, stationPaint)
        canvas.drawCircle(midX, centerY, stationRadius, stationPaint)
        canvas.drawCircle(endX, centerY, stationRadius, stationPaint)

        // Draw station names
        val textOffsetY = 40f
        canvas.drawText(startStationName, startX, centerY + textOffsetY, textPaint)
        canvas.drawText(midStationName, midX, centerY + textOffsetY, textPaint)
        canvas.drawText(endStationName, endX, centerY + textOffsetY, textPaint)

        // Draw train position
        val trainX = startX + (endX - startX) * progress
        canvas.drawText(trainSymbol, trainX, centerY - 10f, trainPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minHeight = 150
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(minHeight, heightSize)
            else -> minHeight
        }

        setMeasuredDimension(width, height)
    }
}
