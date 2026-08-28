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

        val card = FrameLayout(context).apply {
            isFocusable = true
            isClickable = true
            clipChildren = true
            clipToPadding = false
            setPadding(0, 0, 0, 0)
            layoutParams = RecyclerView.LayoutParams(-1, dp(96)).apply {
                setMargins(0, dp(5), 0, dp(5))
            }
        }
        val logo = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            contentDescription = null
        }
        val scrim = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00000000, 0xE600081A.toInt()),
            )
        }
        val textBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(dp(14), 0, dp(14), dp(12))
            layoutParams = FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM)
        }
        val title = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(context).apply {
            setTextColor(Color.rgb(178, 212, 226))
            textSize = 10f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, dp(17)).apply { topMargin = dp(3) }
        }
        val number = TextView(context).apply {
            setTextColor(Color.rgb(220, 242, 255))
            textSize = 12f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = rounded(0x66213A5C, 11f * density)
            layoutParams = FrameLayout.LayoutParams(dp(36), dp(30), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(10)
                rightMargin = dp(10)
            }
        }
        val badge = TextView(context).apply {
            setTextColor(Color.rgb(154, 255, 224))
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = rounded(0x6635A99A, 9f * density)
            layoutParams = FrameLayout.LayoutParams(-2, dp(25), Gravity.TOP or Gravity.START).apply {
                topMargin = dp(10)
                leftMargin = dp(10)
            }
        }
        textBlock.addView(title)
        textBlock.addView(subtitle)
        card.addView(logo)
        card.addView(scrim)
        card.addView(textBlock)
        card.addView(number)
        card.addView(badge)
        return Holder(card, logo, number, title, subtitle, badge)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val density = holder.row.context.resources.displayMetrics.density
        val cardHeight = if (item.kind == MediaKind.LIVE) 88 else 96
        holder.row.layoutParams = RecyclerView.LayoutParams(-1, (cardHeight * density).roundToInt()).apply {
            setMargins(0, (5 * density).roundToInt(), 0, (5 * density).roundToInt())
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
        imageLoader.load(item.backdropUrl.ifBlank { item.logoUrl }, holder.logo, fallbackLogo(item))
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
