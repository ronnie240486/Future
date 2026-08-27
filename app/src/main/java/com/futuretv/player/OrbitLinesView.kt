package com.futuretv.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

/**
 * Desenha linhas finas e brilhantes conectando um ponto central a uma lista
 * de pontos ao redor (as "bolhas" de categoria), imitando o efeito de
 * constelação/rede da referencia visual do usuario. As posicoes sao
 * calculadas em pixels reais de tela e atualizadas via setPoints().
 */
class OrbitLinesView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var center: PointF? = null
    private var points: List<PointF> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 43, 255, 176)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 43, 255, 176)
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
            canvas.drawLine(c.x, c.y, p.x, p.y, linePaint)
            // pontinhos brilhantes ao longo da linha, tipo "estrelas"
            for (t in listOf(0.3f, 0.6f)) {
                val x = c.x + (p.x - c.x) * t
                val y = c.y + (p.y - c.y) * t
                canvas.drawCircle(x, y, 2.5f, dotPaint)
            }
        }
    }
}
