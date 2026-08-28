package com.futuretv.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Camada decorativa da home orbital. Desenha anéis suaves, conexões curvas
 * entre o conteúdo central e as categorias, e pequenos links para satélites.
 */
class OrbitLinesView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var center: PointF? = null
    private var points: List<PointF> = emptyList()
    private var satelliteLinks: List<Pair<PointF, PointF>> = emptyList()

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(48, 112, 164, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(112, 118, 179, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
    }
    private val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(92, 205, 139, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.15f
    }
    private val satelliteLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 168, 194, 255)
        style = Paint.Style.STROKE
        strokeWidth = 0.9f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 164, 210, 255)
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(72, 95, 168, 255)
        style = Paint.Style.FILL
    }
    private val satelliteDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 220, 226, 255)
        style = Paint.Style.FILL
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        glowPaint.setShadowLayer(12f, 0f, 0f, Color.argb(150, 82, 151, 255))
    }

    fun setPoints(
        center: PointF,
        points: List<PointF>,
        satelliteLinks: List<Pair<PointF, PointF>> = emptyList(),
    ) {
        this.center = center
        this.points = points
        this.satelliteLinks = satelliteLinks
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = center ?: return
        val radius = min(width, height) * 0.43f

        canvas.drawOval(
            RectF(c.x - radius * 0.94f, c.y - radius * 0.44f, c.x + radius * 0.94f, c.y + radius * 0.44f),
            ringPaint,
        )
        canvas.drawOval(
            RectF(c.x - radius * 0.58f, c.y - radius * 0.88f, c.x + radius * 0.58f, c.y + radius * 0.88f),
            ringPaint,
        )
        canvas.drawCircle(c.x, c.y, radius * 0.64f, ringPaint)

        points.forEachIndexed { index, point ->
            drawCurvedLink(canvas, c, point, if (index % 3 == 0) accentLinePaint else linePaint, 30f * if (index % 2 == 0) 1f else -1f)
            drawLinkDots(canvas, c, point, false)
        }

        satelliteLinks.forEachIndexed { index, link ->
            val source = link.first
            val satellite = link.second
            drawCurvedLink(canvas, source, satellite, satelliteLinePaint, 10f * if (index % 2 == 0) 1f else -1f)
            val dx = satellite.x - source.x
            val dy = satellite.y - source.y
            val x = source.x + dx * 0.54f
            val y = source.y + dy * 0.54f
            canvas.drawCircle(x, y, 2.6f, satelliteDotPaint)
        }
    }

    private fun drawCurvedLink(canvas: Canvas, source: PointF, target: PointF, paint: Paint, bend: Float) {
        val dx = target.x - source.x
        val dy = target.y - source.y
        val length = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(1f)
        val normalX = -dy / length
        val normalY = dx / length
        val controlX = source.x + dx * 0.5f + normalX * bend
        val controlY = source.y + dy * 0.5f + normalY * bend
        canvas.drawPath(Path().apply {
            moveTo(source.x, source.y)
            quadTo(controlX, controlY, target.x, target.y)
        }, paint)
    }

    private fun drawLinkDots(canvas: Canvas, source: PointF, target: PointF, satellite: Boolean) {
        val dx = target.x - source.x
        val dy = target.y - source.y
        listOf(0.28f, 0.52f, 0.76f).forEach { t ->
            val x = source.x + dx * t
            val y = source.y + dy * t
            canvas.drawCircle(x, y, if (satellite) 2.6f else 5.5f, glowPaint)
            canvas.drawCircle(x, y, if (satellite) 1.2f else 1.8f, dotPaint)
        }
    }
}
