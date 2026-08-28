package com.futuretv.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Fundo em formato de cápsula curva para a navegação lateral da Home. */
class SidebarGlassDrawable : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        color = Color.argb(132, 184, 214, 255)
    }
    private val topGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(54, 255, 255, 255)
    }
    private val panelPath = Path()
    private val edgePath = Path()

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return

        fillPaint.shader = LinearGradient(
            0f,
            0f,
            width,
            0f,
            intArrayOf(
                Color.argb(230, 8, 17, 40),
                Color.argb(220, 21, 35, 75),
                Color.argb(150, 76, 100, 157),
                Color.argb(34, 21, 33, 68),
            ),
            floatArrayOf(0f, 0.56f, 0.86f, 1f),
            Shader.TileMode.CLAMP,
        )

        panelPath.reset()
        panelPath.moveTo(0f, 0f)
        panelPath.lineTo(width * 0.80f, 0f)
        panelPath.cubicTo(width * 0.98f, height * 0.06f, width, height * 0.20f, width * 0.84f, height * 0.34f)
        panelPath.cubicTo(width * 0.72f, height * 0.45f, width * 0.72f, height * 0.55f, width * 0.84f, height * 0.66f)
        panelPath.cubicTo(width, height * 0.80f, width * 0.98f, height * 0.94f, width * 0.80f, height)
        panelPath.lineTo(0f, height)
        panelPath.close()
        canvas.drawPath(panelPath, fillPaint)

        edgePath.reset()
        edgePath.moveTo(width * 0.80f, 0f)
        edgePath.cubicTo(width * 0.98f, height * 0.06f, width, height * 0.20f, width * 0.84f, height * 0.34f)
        edgePath.cubicTo(width * 0.72f, height * 0.45f, width * 0.72f, height * 0.55f, width * 0.84f, height * 0.66f)
        edgePath.cubicTo(width, height * 0.80f, width * 0.98f, height * 0.94f, width * 0.80f, height)
        canvas.drawPath(edgePath, edgePaint)

        topGlowPaint.color = Color.argb(44, 232, 243, 255)
        canvas.drawLine(0f, 1f, width * 0.80f, 1f, topGlowPaint)
        canvas.drawLine(0f, height - 1f, width * 0.80f, height - 1f, topGlowPaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        edgePaint.alpha = alpha
        topGlowPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        edgePaint.colorFilter = colorFilter
        topGlowPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Drawable opacity API")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
