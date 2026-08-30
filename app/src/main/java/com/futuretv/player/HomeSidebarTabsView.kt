package com.futuretv.player

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class HomeSidebarTabsView(context: Context) : FrameLayout(context) {
    data class Tab(val title: String, val iconRes: Int, val action: () -> Unit)

    private val rows = mutableListOf<LinearLayout>()
    private var focusedIndex = 0
    private var lastRowHeight = 0
    private val visualPanel = View(context).apply {
        // Fundo branco desenhado no Android, não uma captura estática.
        background = SidebarWhitePanelDrawable()
        isFocusable = false
        isClickable = false
        contentDescription = null
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.TOP
        // A geometria é recalculada proporcionalmente ao painel, porque o
        // asset gerado tem cinco cápsulas distribuídas por toda a altura.
        setPadding(dp(10), 0, dp(28), 0)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    init {
        // O fundo é nativo; cada linha abaixo continua sendo um botão real.
        background = null
        clipChildren = false
        clipToPadding = false
        isFocusable = false
        isClickable = false
        addView(visualPanel)
        addView(content)
    }

    fun setTabs(newTabs: List<Tab>) {
        rows.clear()
        content.removeAllViews()
        newTabs.forEachIndexed { index, tab ->
            val row = LinearLayout(context).apply {
                id = View.generateViewId()
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isFocusable = true
                isClickable = true
                clipChildren = false
                clipToPadding = false
                setPadding(dp(8), 0, dp(16), 0)
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(20)).apply {
                    bottomMargin = dp(5)
                }
                contentDescription = tab.title
                background = tabBackground(index == focusedIndex, hasFocus = false)
                setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        focusedIndex = index
                        updateRows()
                    } else {
                        view.background = tabBackground(index == focusedIndex, hasFocus = false)
                    }
                }
                setOnClickListener {
                    focusedIndex = index
                    updateRows()
                    tab.action()
                }
            }
            if (index > 0) row.nextFocusUpId = rows[index - 1].id
            if (rows.isNotEmpty()) rows[index - 1].nextFocusDownId = row.id
            row.addView(ImageView(context).apply {
                setImageResource(tab.iconRes)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    topMargin = dp(4)
                    bottomMargin = dp(4)
                }
            })
            row.addView(TextView(context).apply {
                text = tab.title
                setTextColor(Color.rgb(248, 250, 255))
                textSize = 12f
                textScaleX = 0.70f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER_VERTICAL
                maxLines = 1
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.END
                includeFontPadding = false
                letterSpacing = 0.005f
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    marginStart = dp(4)
                }
            })
            row.addView(TextView(context).apply {
                text = "›"
                setTextColor(Color.WHITE)
                textSize = 28f
                gravity = Gravity.CENTER
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(dp(26), LayoutParams.MATCH_PARENT)
            })
            rows += row
            content.addView(row)
        }
        post { updateRows() }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return
        // A posição corresponde à referência: cabeçalho no topo e as cinco
        // cápsulas começam abaixo dele, dentro do painel branco. O
        // cabeçalho (avatar 64dp + nome do perfil embaixo) ocupa bem mais
        // espaço que antes -- 14% deixava a primeira cápsula (ex.: DORAMAS)
        // nascer embaixo do texto do nome do perfil, sobrepondo os dois.
        val top = (height * 0.205f).toInt()
        val rowHeight = (height * 0.14f).toInt()
        lastRowHeight = rowHeight
        val gap = (height * 0.025f).toInt()
        val leftInset = (width * 0.055f).toInt()
        // O ponto mais estreito do painel branco (SidebarWhitePanelDrawable)
        // fica em width*0.70 (a "cintura" da forma ondulada) -- a margem
        // direita precisa ser maior que isso pra capsula nao vazar pra fora
        // do branco. Antes era so 5.5%, bem menor que os ~30% necessarios.
        val rightInset = (width * 0.32f).toInt()
        content.setPadding(leftInset, top, rightInset, 0)
        rows.forEach { row ->
            (row.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                params.width = LinearLayout.LayoutParams.MATCH_PARENT
                params.height = rowHeight
                params.bottomMargin = gap
                row.layoutParams = params
            }
        }
        updateRows()
    }

    fun firstTabId(): Int = rows.firstOrNull()?.id ?: View.NO_ID

    fun setRightFocusTargetId(targetId: Int) {
        rows.forEach { it.nextFocusRightId = targetId }
    }

    fun ownsFocus(view: View): Boolean {
        var current: View? = view
        while (current != null && current !== this) current = current.parent as? View
        return current === this
    }

    fun focusedTabIndex(): Int = focusedIndex

    fun focusFirstTab() {
        focusedIndex = 0
        rows.firstOrNull()?.requestFocus()
        updateRows()
    }

    fun moveFocus(direction: Int): Boolean {
        if (rows.isEmpty()) return false
        val next = (focusedIndex + direction).coerceIn(0, rows.lastIndex)
        if (next == focusedIndex) return true
        focusedIndex = next
        rows[next].requestFocus()
        updateRows()
        return true
    }

    private fun updateRows() {
        rows.forEachIndexed { index, row ->
            row.background = tabBackground(index == focusedIndex, row.hasFocus())
        }
    }

    private fun tabBackground(selected: Boolean, hasFocus: Boolean): GradientDrawable = GradientDrawable().apply {
        // Metade da altura real da linha = formato "comprimido" (estádio)
        // de verdade, nao so um leve arredondado nos cantos -- um raio fixo
        // pequeno demais pra altura da capsula deixava as pontas quase retas.
        setColor(if (hasFocus) Color.rgb(38, 70, 122) else Color.rgb(9, 25, 60))
        cornerRadius = if (lastRowHeight > 0) lastRowHeight / 2f else dp(26).toFloat()
        setStroke(if (hasFocus) dp(2) else dp(1), Color.WHITE)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
