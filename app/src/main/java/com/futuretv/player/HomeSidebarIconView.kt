package com.futuretv.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/** Ícone linear de alto contraste para cada uma das cinco abas da Home. */
class HomeSidebarIconView(
    context: Context,
    private val title: String,
) : View(context) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        alpha = 255
        strokeWidth = dp(2.2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        alpha = 255
    }

    init {
        isFocusable = false
        isClickable = false
        contentDescription = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        if (width <= 0 || height <= 0) return
        val cx = width / density / 2f
        val cy = height / density / 2f
        canvas.save()
        canvas.scale(density, density)
        // Encolhe um pouco em torno do centro, dando margem de seguranca --
        // as coordenadas dos desenhos abaixo usam quase toda a largura/altura
        // disponivel, e como a linha nao recorta os filhos (clipChildren=false),
        // qualquer parte que passe um pouco do limite aparece "vazando" pra
        // fora da capsula.
        canvas.scale(0.55f, 0.55f, cx, cy)
        when {
            title.equals("DORAMAS", ignoreCase = true) -> drawMasks(canvas, cx, cy)
            title.contains("TURCAS", ignoreCase = true) -> drawCrescent(canvas, cx, cy)
            title.equals("NOVELAS", ignoreCase = true) -> drawTelevision(canvas, cx, cy)
            title.equals("REELSHORTS", ignoreCase = true) -> drawPhone(canvas, cx, cy)
            else -> drawAnimeStar(canvas, cx, cy)
        }
        canvas.restore()
    }

    private fun drawMasks(canvas: Canvas, cx: Float, cy: Float) {
        val back = Path().apply {
            moveTo(cx - 18, cy - 12)
            cubicTo(cx - 8, cy - 18, cx + 1, cy - 13, cx + 3, cy - 5)
            cubicTo(cx + 5, cy + 5, cx - 5, cy + 13, cx - 15, cy + 10)
            cubicTo(cx - 21, cy + 7, cx - 22, cy - 5, cx - 18, cy - 12)
        }
        canvas.drawPath(back, stroke)
        canvas.drawOval(RectF(cx - 14, cy - 5, cx - 7, cy + 2), stroke)
        canvas.drawLine(cx - 21, cy - 8, cx - 25, cy - 14, stroke)
        canvas.drawLine(cx - 5, cy - 10, cx - 1, cy - 14, stroke)

        val front = Path().apply {
            moveTo(cx - 1, cy - 10)
            cubicTo(cx + 10, cy - 15, cx + 19, cy - 9, cx + 18, cy)
            cubicTo(cx + 18, cy + 10, cx + 8, cy + 15, cx - 2, cy + 9)
            cubicTo(cx - 8, cy + 6, cx - 8, cy - 6, cx - 1, cy - 10)
        }
        canvas.drawPath(front, stroke)
        canvas.drawOval(RectF(cx + 3, cy - 4, cx + 10, cy + 3), stroke)
        canvas.drawLine(cx + 15, cy - 7, cx + 22, cy - 10, stroke)
    }

    private fun drawCrescent(canvas: Canvas, cx: Float, cy: Float) {
        val outer = Path().apply {
            moveTo(cx + 11, cy - 20)
            cubicTo(cx - 8, cy - 19, cx - 18, cy - 4, cx - 13, cy + 10)
            cubicTo(cx - 9, cy + 23, cx + 6, cy + 25, cx + 16, cy + 14)
            cubicTo(cx + 3, cy + 18, cx - 5, cy + 8, cx - 4, cy - 2)
            cubicTo(cx - 3, cy - 10, cx + 2, cy - 17, cx + 11, cy - 20)
        }
        canvas.drawPath(outer, stroke)
        drawStar(canvas, cx + 12, cy - 9, 4f, 1.5f)
        drawStar(canvas, cx - 15, cy + 17, 3f, 1.2f)
        canvas.drawLine(cx - 19, cy - 1, cx - 24, cy - 1, stroke)
        canvas.drawLine(cx - 21.5f, cy - 3.5f, cx - 21.5f, cy + 1.5f, stroke)
    }

    private fun drawTelevision(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawRoundRect(RectF(cx - 21, cy - 15, cx + 21, cy + 16), 5f, 5f, stroke)
        canvas.drawLine(cx - 8, cy - 15, cx - 14, cy - 22, stroke)
        canvas.drawLine(cx - 2, cy - 15, cx + 5, cy - 22, stroke)
        val triangle = Path().apply {
            moveTo(cx - 4, cy - 8)
            lineTo(cx + 10, cy)
            lineTo(cx - 4, cy + 8)
            close()
        }
        canvas.drawPath(triangle, stroke)
        canvas.drawLine(cx - 16, cy + 21, cx + 16, cy + 21, stroke)
    }

    private fun drawPhone(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawRoundRect(RectF(cx - 13, cy - 22, cx + 13, cy + 22), 4f, 4f, stroke)
        canvas.drawLine(cx - 5, cy - 17, cx + 5, cy - 17, stroke)
        val triangle = Path().apply {
            moveTo(cx - 4, cy - 8)
            lineTo(cx + 8, cy)
            lineTo(cx - 4, cy + 8)
            close()
        }
        canvas.drawPath(triangle, stroke)
        canvas.drawCircle(cx, cy + 16, 1.8f, fill)
    }

    private fun drawAnimeStar(canvas: Canvas, cx: Float, cy: Float) {
        val star = Path().apply {
            moveTo(cx, cy - 23)
            lineTo(cx + 5, cy - 6)
            lineTo(cx + 22, cy)
            lineTo(cx + 5, cy + 6)
            lineTo(cx, cy + 23)
            lineTo(cx - 5, cy + 6)
            lineTo(cx - 22, cy)
            lineTo(cx - 5, cy - 6)
            close()
        }
        canvas.drawPath(star, stroke)
        drawStar(canvas, cx + 18, cy - 17, 3.5f, 1.3f)
        drawStar(canvas, cx - 17, cy + 16, 3.5f, 1.3f)
        canvas.drawCircle(cx - 19, cy - 16, 1.5f, fill)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, inner: Float) {
        val star = Path()
        for (i in 0 until 8) {
            val angle = (-Math.PI / 2.0 + i * Math.PI / 4.0).toFloat()
            val r = if (i % 2 == 0) radius else inner
            val x = cx + kotlin.math.cos(angle) * r
            val y = cy + kotlin.math.sin(angle) * r
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        canvas.drawPath(star, stroke)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
