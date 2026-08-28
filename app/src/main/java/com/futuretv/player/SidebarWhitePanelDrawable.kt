package com.futuretv.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Painel branco da sidebar exclusiva da Home.
 *
 * O fundo é desenhado nativamente para não depender de uma captura estática:
 * os cinco controles ficam por cima e continuam sendo Views individuais.
 */
class SidebarWhitePanelDrawable : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        color = Color.argb(190, 216, 222, 232)
    }
    private val innerEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(150, 255, 255, 255)
    }
    private val panelPath = Path()
    private val edgePath = Path()
    private val innerPath = Path()

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
                Color.rgb(255, 255, 255),
                Color.rgb(252, 253, 255),
                Color.rgb(244, 247, 252),
                Color.rgb(235, 240, 248),
            ),
            floatArrayOf(0f, 0.48f, 0.82f, 1f),
            Shader.TileMode.CLAMP,
        )

        panelPath.reset()
        panelPath.moveTo(0f, 0f)
        panelPath.lineTo(width * 0.75f, 0f)
        panelPath.cubicTo(
            width * 0.93f,
            height * 0.06f,
            width,
            height * 0.19f,
            width * 0.83f,
            height * 0.34f,
        )
        panelPath.cubicTo(
            width * 0.70f,
            height * 0.46f,
            width * 0.70f,
            height * 0.54f,
            width * 0.83f,
            height * 0.66f,
        )
        panelPath.cubicTo(
            width,
            height * 0.81f,
            width * 0.93f,
            height * 0.94f,
            width * 0.75f,
            height,
        )
        panelPath.lineTo(0f, height)
        panelPath.close()
        // O branco preenche o quadro inteiro; os cinco quadros azul-marinho
        // são desenhados pelas Views nativas posicionadas por cima.
        canvas.drawPath(panelPath, fillPaint)

        edgePath.reset()
        edgePath.moveTo(width * 0.75f, 0f)
        edgePath.cubicTo(
            width * 0.93f,
            height * 0.06f,
            width,
            height * 0.19f,
            width * 0.83f,
            height * 0.34f,
        )
        edgePath.cubicTo(
            width * 0.70f,
            height * 0.46f,
            width * 0.70f,
            height * 0.54f,
            width * 0.83f,
            height * 0.66f,
        )
        edgePath.cubicTo(
            width,
            height * 0.81f,
            width * 0.93f,
            height * 0.94f,
            width * 0.75f,
            height,
        )
        canvas.drawPath(edgePath, edgePaint)

        innerPath.reset()
        innerPath.moveTo(width * 0.72f, 4f)
        innerPath.cubicTo(
            width * 0.85f,
            height * 0.09f,
            width * 0.91f,
            height * 0.20f,
            width * 0.77f,
            height * 0.34f,
        )
        innerPath.cubicTo(
            width * 0.65f,
            height * 0.46f,
            width * 0.65f,
            height * 0.54f,
            width * 0.77f,
            height * 0.66f,
        )
        innerPath.cubicTo(
            width * 0.91f,
            height * 0.80f,
            width * 0.85f,
            height * 0.91f,
            width * 0.72f,
            height - 4f,
        )
        canvas.drawPath(innerPath, innerEdgePaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        edgePaint.alpha = alpha
        innerEdgePaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        edgePaint.colorFilter = colorFilter
        innerEdgePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Drawable opacity API")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
