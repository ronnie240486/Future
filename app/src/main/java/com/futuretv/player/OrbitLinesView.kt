package com.futuretv.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Desenha linhas conectando um ponto central a uma lista de pontos ao redor
 * (as "bolhas" de categoria), com efeito de brilho (varias camadas de
 * espessura/transparencia decrescente, simulando o halo de blur da
 * referencia) e um gradiente de cor ao longo de cada linha.
 */
class OrbitLinesView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var center: PointF? = null
    private var points: List<PointF> = emptyList()

    // Camadas de glow: da mais grossa/fraca pra mais fina/forte, imitando blur.
    private val glowLayers = listOf(
        14f to 18,
        9f to 35,
        5f to 60,
        2.5f to 130,
    )
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 190, 240, 255)
        style = Paint.Style.FILL
    }
    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 120, 210, 255)
        style = Paint.Style.FILL
    }

    fun setPoints(center: PointF, points: List<PointF>) {
        this.center = center
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = center ?: return
        points.forEach { p ->
            val shader = LinearGradient(
                c.x, c.y, p.x, p.y,
                Color.argb(160, 123, 97, 255),
                Color.argb(160, 43, 220, 255),
                Shader.TileMode.CLAMP,
            )
            glowLayers.forEach { (width, alpha) ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                    this.alpha = alpha
                    strokeWidth = width
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawLine(c.x, c.y, p.x, p.y, paint)
            }
            // "Estrelinhas" brilhantes ao longo da linha.
            for (t in listOf(0.22f, 0.42f, 0.62f, 0.82f)) {
                val x = c.x + (p.x - c.x) * t
                val y = c.y + (p.y - c.y) * t
                canvas.drawCircle(x, y, 6f, dotGlowPaint)
                canvas.drawCircle(x, y, 2.6f, dotPaint)
            }
        }
    }
}
