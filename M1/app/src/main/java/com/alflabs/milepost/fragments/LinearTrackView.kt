package com.alflabs.milepost.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.max
import androidx.core.graphics.withScale
import com.alflabs.milepost.R
import kotlin.math.min


/**
 * The "linear track" view is a Subway style view of the map that displays stations
 * at equidistances, irrelevant of their MP values.
 *
 * Each station is associated with an MP value. All stations must be sorted by increasing MP.
 * There must be at least 2 stations to define a start and an end MP value.
 *
 * This is then used to display the current position of the train on the line as well as the
 * relative positions of the audio segments.
 */
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
    private var directionTowardsStart: Boolean? = null

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_line)
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val stationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_station)
        style = Paint.Style.FILL
    }

    private val textStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_text)
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }

    private val textMiddlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_text)
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val textEndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_text)
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }

    private val trainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

    private val segmentDefaultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_segment)
        strokeWidth = 16f
        style = Paint.Style.STROKE
    }

    private val segmentCurrentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_segment_curr)
        strokeWidth = 16f
        style = Paint.Style.STROKE
    }

    private val segmentNextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.linear_segment_next)
        strokeWidth = 16f
        style = Paint.Style.STROKE
    }


    /** Train position in 0..1 range. Only displayed once we know where it is. */
    private var trainPositionPct: Float? = null

    private val stations = mutableListOf<LinearStation>()
    private val segments = mutableListOf<LinearSegment>()


    // Unicode:
    // 0x1F682 = 0xD83D 0xDE82 = 🚂
    // 0x1F686 = 0xD83D 0xDE86 = 🚆
    private val trainSymbol = "\uD83D\uDE82"

    /**
     * Sets the stations on the linear track view.
     *
     * Requirements:
     * - The Pairs denote <MP value, station label>. The label should be empty if missing.
     * - It is implied there should be at least 2 points (first and last).
     * - The first and last denote the "MP direction".
     * - All intermediate stations should be ordered in the same MP direction.
     */
    fun setStations(newStations: List<Pair<Float, String>>) {
        stations.clear()
        val maxIndex = newStations.lastIndex
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

    /**
     * Sets the segments on the linear track view.
     *
     * This MUST be called after stations have been defined, otherwise we cannot interpret
     * the relative MP start and end values.
     */
    fun setSegments(newSegments: List<Triple<Long, Float, Float>>) {
        segments.clear()

        if (stations.size < 2) {
            return
        }

        val mpFirst = stations.first().mp
        val mpLast = stations.last().mp
        segments.addAll(
            newSegments.map { triple ->
                LinearSegment(
                    id = triple.first,
                    pctStart = convertMpToPct(triple.second),
                    pctEnd = convertMpToPct(triple.third),
                    paint = segmentDefaultPaint,
                )
            })
        invalidate()
    }

    fun setCurrentSegment(segmentId: Long) {
        segments.forEach { segment ->
            if (segment.id == segmentId) {
                segment.paint = segmentCurrentPaint
            } else if (segment.paint == segmentCurrentPaint) {
                segment.paint = segmentDefaultPaint
            }
        }
        invalidate()
    }

    fun setNextSegment(segmentId: Long) {
        segments.forEach { segment ->
            if (segment.id == segmentId) {
                segment.paint = segmentNextPaint
            } else if (segment.paint == segmentNextPaint) {
                segment.paint = segmentDefaultPaint
            }
        }
        invalidate()
    }

    fun setCurrentMp(mp: Float) {
        if (stations.size < 2) {
            return
        }
        trainPositionPct = convertMpToPct(mp)
        invalidate()
    }

    private fun convertMpToPct(mp: Float): Float {
        if (stations.size < 2) {
            return 0f
        }

        var p = -1f
        // Handle out of bounds gracefully.
        val mpFirst = stations.first().mp
        val mpLast = stations.last().mp
        if (mpLast > mpFirst) {
            if (mp < mpFirst) {
                p = 0f
            } else if (mp > mpLast) {
                p = 1f
            }
        } else {
            if (mp > mpFirst) {
                p = 0f
            } else if (mp < mpLast) {
                p = 1f
            }
        }
        if (p >= 0f) {
            return p
        }

        // Linear search each interval.
        // The mp direction can be either increasing or decreasing. Handle both.
        // Note that the pct progress is not related to the MP distance but the
        // *index* in the list since all station points are drawn equidistant.
        val epsilon = 0.001f
        var previousMp = mpFirst
        stations.forEachIndexed { index, station ->
            if (index == 0) return@forEachIndexed // skip first one
            val deltaMp = station.mp - previousMp
            if (kotlin.math.abs(deltaMp) > epsilon) {
                val mpMin = minOf(mpFirst, station.mp)
                val mpMax = maxOf(mpFirst, station.mp)
                if (mp in mpMin..mpMax) {
                    p = ((index - 1) + (mp - previousMp) / deltaMp) / stations.lastIndex
                    return min(1f, max(0f, p))
                }
            }
            previousMp = station.mp
        }

        // If we haven't found anything, use the end position.
        return 1f
    }

    fun setDirectionTowardsStart(dir: Boolean) {
        directionTowardsStart = dir
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = paddingLeft.toFloat()
        val paddingRight = paddingRight.toFloat()
        val paddingTop = paddingTop.toFloat()

        val lineY = paddingTop + trainLineHeightPx + stationRadiusPx
        val startX = paddingLeft + paddingPx
        val endX = this.width - paddingRight - paddingPx
        val lenX = endX - startX

        // Draw horizontal line
        canvas.drawLine(startX, lineY, endX, lineY, linePaint)

        // Draw segments
        for (segment in segments) {
            canvas.drawLine(
                startX + lenX * segment.pctStart,
                lineY,
                startX + lenX * segment.pctEnd,
                lineY,
                segment.paint)
        }

        // Draw stations markers
        val textY = lineY + stationRadiusPx + textLineHeightPx - textDescentPx
        for (station in stations) {
            val x = startX + lenX * station.pct
            canvas.drawCircle(x, lineY, stationRadiusPx, stationPaint)
            canvas.drawText(String.format("%.2f", station.mp), x, textY, station.paint)
            canvas.drawText(station.title, x, textY + textLineHeightPx, station.paint)
        }

        // Draw train position
        trainPositionPct?.let { pct ->
            val trainX = startX + lenX * pct
            val trainY = lineY - stationRadiusPx - trainDescentPx
            val tickY = lineY - stationRadiusPx

            // Draw a tick
            canvas.drawLine(trainX, lineY, trainX, tickY, linePaint)

            // Draw the train text symbol
            if (directionTowardsStart ?: false) {
                // Unicode symbol normally points towards the "start" station.
                canvas.drawText(trainSymbol, trainX, trainY, trainPaint)
            } else {
                // Mirror horizontally for the "Unicode symbol to point towards the "end" station.
                canvas.withScale(-1f, 1f, trainX, trainY) {
                    drawText(trainSymbol, trainX, trainY, trainPaint)
                }
            }
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

private data class LinearSegment(
    val id: Long,
    val pctStart: Float,
    val pctEnd: Float,
    var paint: Paint,
)
