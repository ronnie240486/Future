package com.futuretv.player

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class CatalogAdapter(
    private val imageLoader: ImageLoader,
    private val fallbackLogo: (CatalogEntry) -> Int,
    private val onSelected: (CatalogEntry) -> Unit,
    private val onClicked: (CatalogEntry) -> Unit,
    private val onLongClicked: (CatalogEntry) -> Unit,
    // Retorna a capa "oficial" ja conhecida pra série/filme (buscada uma
    // vez, na TMDB, e reaproveitada em todos os episódios do mesmo
    // seriesGroup) -- null se ainda não foi buscada.
    private val posterResolver: (CatalogEntry) -> String? = { null },
    // Chamado quando um item visível ainda não tem capa oficial em cache,
    // pra disparar a busca em segundo plano (uma vez só por série).
    private val onNeedsPoster: (CatalogEntry) -> Unit = {},
) : RecyclerView.Adapter<CatalogAdapter.Holder>() {
    private var items: List<CatalogEntry> = emptyList()
    private var selectedKey: String? = null
    private var attachedRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (attachedRecyclerView === recyclerView) attachedRecyclerView = null
    }

    fun submit(items: List<CatalogEntry>, selectedKey: String?) {
        this.items = items
        this.selectedKey = selectedKey
        notifyDataSetChanged()
    }

    fun positionOf(key: String?): Int = if (key == null) -1 else items.indexOfFirst { it.key == key }

    // Atualiza só as linhas visíveis que passam no filtro (ex.: mesma série),
    // em vez de notifyDataSetChanged() -- que força o RecyclerView a tratar
    // a lista inteira como potencialmente mudada, causando um re-layout
    // disruptivo bem no meio da navegação com o D-pad (o que a barra de
    // categorias/colunas "empilhando estranho" que o usuário via era isso).
    fun refreshItemsMatching(predicate: (CatalogEntry) -> Boolean) {
        val recyclerView = attachedRecyclerView ?: return
        items.forEachIndexed { index, entry ->
            if (predicate(entry) && recyclerView.findViewHolderForAdapterPosition(index) != null) {
                notifyItemChanged(index)
            }
        }
    }

    fun setSelectedKey(key: String?) {
        if (key == selectedKey) return
        val oldPosition = positionOf(selectedKey)
        selectedKey = key
        val recyclerView = attachedRecyclerView
        if (oldPosition >= 0 && recyclerView?.findViewHolderForAdapterPosition(oldPosition) != null) notifyItemChanged(oldPosition)
        val newPosition = positionOf(key)
        if (newPosition >= 0 && recyclerView?.findViewHolderForAdapterPosition(newPosition) != null) notifyItemChanged(newPosition)
    }

    fun append(items: List<CatalogEntry>) {
        if (items.isEmpty()) return
        val start = this.items.size
        this.items = this.items + items
        notifyItemRangeInserted(start, items.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).roundToInt()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            setPadding(dp(10), 0, dp(12), 0)
            layoutParams = RecyclerView.LayoutParams(-1, dp(64)).apply {
                setMargins(0, dp(3), 0, dp(3))
            }
        }
        val logoBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(84), dp(50))
            background = rounded(0xFF0B1424, 8f * density)
            clipChildren = true
        }
        val logo = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -1, Gravity.CENTER).apply {
                val pad = dp(6)
                setMargins(pad, pad, pad, pad)
            }
            contentDescription = null
        }
        logoBox.addView(logo)
        val number = TextView(context).apply {
            setTextColor(Color.rgb(150, 170, 200))
            textSize = 11f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dp(28), -1)
        }
        val textBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }
        val title = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(context).apply {
            setTextColor(Color.rgb(150, 170, 200))
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) }
        }
        val badge = TextView(context).apply {
            setTextColor(Color.rgb(154, 255, 224))
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = rounded(0x6635A99A, 9f * density)
            layoutParams = LinearLayout.LayoutParams(-2, -2)
        }
        textBlock.addView(title)
        textBlock.addView(subtitle)
        card.addView(number)
        card.addView(logoBox)
        card.addView(textBlock)
        card.addView(badge)
        return Holder(card, logo, number, title, subtitle, badge)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val density = holder.row.context.resources.displayMetrics.density
        holder.row.layoutParams = RecyclerView.LayoutParams(-1, (64 * density).roundToInt()).apply {
            setMargins(0, (3 * density).roundToInt(), 0, (3 * density).roundToInt())
        }
        holder.number.text = String.format("%02d", position + 1)
        holder.title.text = if (item.kind == MediaKind.SERIES && item.seriesGroup.isNotBlank()) item.seriesGroup else item.name
        holder.subtitle.text = when (item.kind) {
            MediaKind.LIVE -> item.groupTitle.ifBlank { "Canal ao vivo" }
            MediaKind.MOVIE -> listOf(item.year, item.groupTitle).filter { it.isNotBlank() }.joinToString("  •  ").ifBlank { "Filme" }
            MediaKind.SERIES -> item.groupTitle.ifBlank { "Série" }
        }
        holder.badge.text = item.quality.ifBlank {
            when (item.kind) {
                MediaKind.LIVE -> "NO AR"
                MediaKind.MOVIE -> "FILME"
                MediaKind.SERIES -> "SÉRIE"
            }
        }
        holder.row.tag = item.key
        val officialPoster = if (item.kind == MediaKind.MOVIE || item.kind == MediaKind.SERIES) posterResolver(item) else null
        if (officialPoster != null) {
            imageLoader.load(officialPoster, holder.logo, fallbackLogo(item))
        } else {
            imageLoader.load(item.backdropUrl.ifBlank { item.logoUrl }, holder.logo, fallbackLogo(item))
            if (item.kind == MediaKind.MOVIE || item.kind == MediaKind.SERIES) onNeedsPoster(item)
        }
        fun paint(focused: Boolean) {
            val isSelected = item.key == selectedKey
            holder.row.background = glassCard(focused, isSelected, holder.row.context.resources.displayMetrics.density)
            holder.badge.background = rounded(
                if (item.kind == MediaKind.LIVE) 0x6635E8B5 else 0x663A86FF,
                9f * holder.row.context.resources.displayMetrics.density,
            )
        }
        paint(holder.row.hasFocus())
        holder.row.setOnFocusChangeListener { _, hasFocus ->
            paint(hasFocus)
            if (hasFocus) onSelected(item)
        }
        holder.row.setOnClickListener { onClicked(item) }
        holder.row.isLongClickable = true
        holder.row.setOnLongClickListener { onLongClicked(item); true }
    }

    override fun getItemCount(): Int = items.size

    class Holder(
        val row: View,
        val logo: ImageView,
        val number: TextView,
        val title: TextView,
        val subtitle: TextView,
        val badge: TextView,
    ) : RecyclerView.ViewHolder(row)

    private fun rounded(color: Long, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(Color.argb((color shr 24 and 0xFF).toInt(), (color shr 16 and 0xFF).toInt(), (color shr 8 and 0xFF).toInt(), (color and 0xFF).toInt()))
        cornerRadius = radius
    }

    private fun glassCard(focused: Boolean, selected: Boolean, density: Float): GradientDrawable = GradientDrawable().apply {
        val fill = when {
            selected -> 0x403FE7EF
            focused -> 0x30366C9B
            else -> 0x2615213D
        }
        setColor(Color.argb((fill shr 24 and 0xFF).toInt(), (fill shr 16 and 0xFF).toInt(), (fill shr 8 and 0xFF).toInt(), (fill and 0xFF).toInt()))
        cornerRadius = 16f * density
        when {
            selected -> setStroke((2.2f * density).roundToInt(), Color.rgb(126, 244, 255))
            focused -> setStroke((1.5f * density).roundToInt(), Color.rgb(118, 207, 255))
        }
    }
}
