package com.alflabs.milepost.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

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

    private val textStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }

    private val textMiddlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val textEndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }

    private val trainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

    private var progress = 0f

    private val stations = mutableListOf<LinearStation>()

    // Unicode:
    // 0x1F682 = 0xD83D 0xDE82 = 🚂
    // 0x1F686 = 0xD83D 0xDE86 = 🚆
    private val trainSymbol = "\uD83D\uDE82"

    fun setStations(newStations: List<Pair<Float, String>>) {
        stations.clear()
        val maxIndex = newStations.size - 1
        val cntSegments = max(1f, maxIndex.toFloat())
        newStations.forEachIndexed { index, pair ->
            val pct = index.toFloat() / cntSegments
            stations.add(LinearStation(
                pair.first,
                pair.second,
                pct,
                when (index) {
                    0 -> textStartPaint
                    maxIndex -> textEndPaint
                    else -> textMiddlePaint
                }
            ))
        }
        invalidate()
    }

    fun setCurrentMp(mp: Float) {
        var p = 0f
        var last : LinearStation? = null
        for (station in stations) {
            if (last == null) {
                last = station
            } else {
                val deltaMp = station.mp - last.mp
                if (deltaMp > 0) {
                    if (mp >= last.mp && mp < station.mp) {
                        p = (mp - last.mp) / deltaMp
                        progress = p
                        invalidate()
                        return
                    }
                    last = station
                }
            }
        }
    }

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

        // Draw horizontal line
        canvas.drawLine(startX, centerY, endX, centerY, linePaint)

        // Draw stations markers
        val stationRadius = 10f
        val textOffsetY = 40f
        for (station in stations) {
            val x = startX + (endX - startX) * station.pct
            canvas.drawCircle(x, centerY, stationRadius, stationPaint)
            canvas.drawText(String.format("%.2f", station.mp), x, centerY + textOffsetY, station.paint)
            canvas.drawText(station.title, x, centerY + 2 * textOffsetY, station.paint)
        }

        // Draw train position
        val trainX = startX + (endX - startX) * progress
        canvas.drawText(trainSymbol, trainX, centerY - 20f, trainPaint)
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

private data class LinearStation(
    val mp: Float,
    val title: String,
    val pct: Float,
    val paint: Paint,
)
