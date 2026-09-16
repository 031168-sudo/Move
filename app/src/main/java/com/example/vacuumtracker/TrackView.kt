package com.example.vacuumtracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class TrackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var points: List<Pair<Float, Float>> = emptyList()
        set(value) { field = value; invalidate() }

    var roomWidthM: Float? = null
        set(value) { field = value; invalidate() }
    var roomHeightM: Float? = null
        set(value) { field = value; invalidate() }

    private val pathPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private val roomPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val startPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val endPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val arrowPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        var minX = roomWidthM?.let { -it / 2 } ?: 0f
        var maxX = roomWidthM?.let { it / 2 } ?: 0f
        var minY = roomHeightM?.let { -it / 2 } ?: 0f
        var maxY = roomHeightM?.let { it / 2 } ?: 0f

        if (points.isNotEmpty()) {
            for ((x, y) in points) {
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
            }
        }
        if (roomWidthM == null && points.isEmpty()) {
            minX = -1f; maxX = 1f; minY = -1f; maxY = 1f
        }

        val marginM = 0.3f
        minX -= marginM; maxX += marginM
        minY -= marginM; maxY += marginM

        val spanX = max(maxX - minX, 0.5f)
        val spanY = max(maxY - minY, 0.5f)
        val padding = 24f
        val scale = min((w - 2 * padding) / spanX, (h - 2 * padding) / spanY)

        fun sx(x: Float) = padding + (x - minX) * scale
        fun sy(y: Float) = h - padding - (y - minY) * scale

        if (roomWidthM != null && roomHeightM != null) {
            val rw = roomWidthM!!; val rh = roomHeightM!!
            canvas.drawRect(sx(-rw / 2), sy(rh / 2), sx(rw / 2), sy(-rh / 2), roomPaint)
        }

        if (points.size < 2) return

        val path = Path()
        path.moveTo(sx(points[0].first), sy(points[0].second))
        for (i in 1 until points.size) {
            path.lineTo(sx(points[i].first), sy(points[i].second))
        }
        canvas.drawPath(path, pathPaint)

        val step = max(points.size / 25, 1)
        var i = step
        while (i < points.size) {
            val (x0, y0) = points[i - 1]
            val (x1, y1) = points[i]
            val angle = atan2((sy(y1) - sy(y0)).toDouble(), (sx(x1) - sx(x0)).toDouble())
            drawArrowHead(canvas, sx(x1), sy(y1), angle)
            i += step
        }

        canvas.drawCircle(sx(points.first().first), sy(points.first().second), 10f, startPaint)
        canvas.drawCircle(sx(points.last().first), sy(points.last().second), 10f, endPaint)
    }

    private fun drawArrowHead(canvas: Canvas, x: Float, y: Float, angle: Double) {
        val size = 14f
        val p = Path()
        p.moveTo(x, y)
        p.lineTo(
            (x - size * cos(angle - 0.4)).toFloat(),
            (y - size * sin(angle - 0.4)).toFloat()
        )
        p.lineTo(
            (x - size * cos(angle + 0.4)).toFloat(),
            (y - size * sin(angle + 0.4)).toFloat()
        )
        p.close()
        canvas.drawPath(p, arrowPaint)
    }
}
