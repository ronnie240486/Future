package com.futuretv.player

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.sin

class ConstellationTransitionView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val stars = listOf(
        PointF(0.17f, 0.25f), PointF(0.29f, 0.14f), PointF(0.42f, 0.21f),
        PointF(0.54f, 0.10f), PointF(0.68f, 0.22f), PointF(0.82f, 0.16f),
        PointF(0.22f, 0.58f), PointF(0.36f, 0.72f), PointF(0.51f, 0.62f),
        PointF(0.66f, 0.76f), PointF(0.80f, 0.58f), PointF(0.91f, 0.72f),
        PointF(0.12f, 0.42f), PointF(0.31f, 0.42f), PointF(0.47f, 0.38f),
        PointF(0.61f, 0.48f), PointF(0.74f, 0.38f), PointF(0.88f, 0.46f),
    )
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var playToken = 0

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = false
        isFocusable = false
        visibility = GONE
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun play() {
        animator?.cancel()
        playToken++
        val token = playToken
        visibility = VISIBLE
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 760L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        postDelayed({
            if (token == playToken) {
                animator = null
                visibility = GONE
                invalidate()
            }
        }, 980L)
    }

    fun stop() {
        playToken++
        animator?.cancel()
        animator = null
        progress = 0f
        visibility = GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0f) return
        val baseSize = (width.coerceAtMost(height) * 0.009f).coerceAtLeast(3f)
        stars.forEachIndexed { index, point ->
            val appear = ((progress * 1.55f) - index * 0.035f).coerceIn(0f, 1f)
            if (appear <= 0f) return@forEachIndexed
            val pulse = (0.5f + 0.5f * sin((progress * 5.2f + index * 0.72f) * PI).toFloat())
            val x = width * point.x
            val y = height * point.y
            val radius = baseSize * (0.65f + pulse * 1.35f)
            val alpha = (220f * appear * (1f - progress * 0.26f)).toInt().coerceIn(0, 255)

            paint.style = Paint.Style.FILL
            paint.color = Color.argb((alpha * 0.36f).toInt(), 94, 196, 255)
            paint.setShadowLayer(radius * 5f, 0f, 0f, Color.argb(alpha, 85, 190, 255))
            canvas.drawCircle(x, y, radius * 1.8f, paint)
            paint.clearShadowLayer()

            paint.color = Color.argb(alpha, 237, 251, 255)
            canvas.drawCircle(x, y, radius * 0.52f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (radius * 0.34f).coerceAtLeast(1f)
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.argb((alpha * 0.86f).toInt(), 196, 239, 255)
            canvas.drawPath(Path().apply {
                moveTo(x - radius * 2.2f, y)
                lineTo(x + radius * 2.2f, y)
                moveTo(x, y - radius * 2.2f)
                lineTo(x, y + radius * 2.2f)
            }, paint)
            paint.strokeCap = Paint.Cap.BUTT
        }
    }
}
