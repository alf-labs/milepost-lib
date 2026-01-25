package com.alflabs.milepost.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.max


class LinearTrackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = LinearTrackView::class.java.simpleName
        private const val stationRadius = 10f
    }

    private var stationRadiusPx = 10f
    private val paddingPx = 40f
    private var textLineHeightPx = 30
    private var textDescentPx = 0
    private var trainLineHeightPx = 40
    private var trainDescentPx = 0
    private var directionToStart = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val stationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
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

        val lineY = paddingTop + trainLineHeightPx + stationRadiusPx
        val startX = paddingLeft + paddingPx
        val endX = width + paddingLeft - paddingPx

        // Draw horizontal line
        canvas.drawLine(startX, lineY, endX, lineY, linePaint)

        // Draw stations markers
        val textY = lineY + stationRadiusPx + textLineHeightPx - textDescentPx
        for (station in stations) {
            val x = startX + (endX - startX) * station.pct
            canvas.drawCircle(x, lineY, stationRadiusPx, stationPaint)
            canvas.drawText(String.format("%.2f", station.mp), x, textY, station.paint)
            canvas.drawText(station.title, x, textY + textLineHeightPx, station.paint)
        }

        // Draw train position
        val trainX = startX + (endX - startX) * progress
        val trainY = lineY - stationRadiusPx - trainDescentPx
        if (directionToStart) {
            // Unicode symbol normally points towards the "start" station.
            canvas.drawText(trainSymbol, trainX, trainY, trainPaint)
        } else {
            // Mirror horizontally for the "Unicode symbol to point towards the "end" station.
            canvas.save()
            canvas.scale(-1f, 1f, trainX, trainY)
            canvas.drawText(trainSymbol, trainX, trainY, trainPaint)
            canvas.restore()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val metrics = textMiddlePaint.fontMetrics
        val trainMetrics = trainPaint.fontMetrics
        // (descent is positive, ascent is negative, so this result is positive)
        textLineHeightPx = (metrics.descent - metrics.ascent).toInt()
        trainLineHeightPx = (trainMetrics.descent - trainMetrics.ascent).toInt()
        textDescentPx = metrics.descent.toInt()
        trainDescentPx = trainMetrics.descent.toInt()
        Log.d(TAG, "@@ textLineHeightPx = $textLineHeightPx")
        Log.d(TAG, "@@ trainLineHeightPx = $trainLineHeightPx")

        // convert the dpi-neutral stationRadius into a pixel value
        stationRadiusPx = (stationRadius * resources.displayMetrics.density)
        Log.d(TAG, "@@ stationRadiusPx = $stationRadiusPx")

        val minHeight = (2 * stationRadiusPx + 2 * textLineHeightPx + trainLineHeightPx).toInt()
        Log.d(TAG, "@@ minHeight = $minHeight")


        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(minHeight, heightSize)
            else -> minHeight
        }

        Log.d(TAG, "@@ LinearTrackView onMeasure: width = $width, height = $height")
        setMeasuredDimension(width, height)
    }
}

private data class LinearStation(
    val mp: Float,
    val title: String,
    val pct: Float,
    val paint: Paint,
)
