package com.futuretv.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ImageLoader {
    private val executor = Executors.newFixedThreadPool(4)
    private val memoryCache = object : LruCache<String, Bitmap>(memoryBudget()) {}

    fun load(url: String, target: ImageView, fallback: Int) {
        loadInternal(url, target, fallback, cropTransparent = false)
    }

    /** Carrega um logo removendo apenas margens transparentes do arquivo remoto. */
    fun loadCropped(url: String, target: ImageView, fallback: Int) {
        loadInternal(url, target, fallback, cropTransparent = true)
    }

    private fun loadInternal(url: String, target: ImageView, fallback: Int, cropTransparent: Boolean) {
        target.setImageResource(fallback)
        if (url.isBlank()) return
        val key = if (cropTransparent) "cropped:$url" else url.trim()
        target.tag = key
        memoryCache.get(key)?.let { bitmap ->
            target.post { if (target.tag == key) target.setImageBitmap(bitmap) }
            return
        }
        executor.execute {
            val downloaded = download(url.trim())
            val bitmap = if (cropTransparent && downloaded != null) trimTransparentMargins(downloaded) else downloaded
            if (bitmap != null) memoryCache.put(key, bitmap)
            if (bitmap != null) {
                target.post { if (target.tag == key) target.setImageBitmap(bitmap) }
            }
        }
    }

    private fun download(url: String): Bitmap? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            useCaches = true
            setRequestProperty("User-Agent", "FutureTV/1.0 AndroidTV")
            setRequestProperty("Accept", "image/avif,image/webp,image/jpeg,image/png,*/*")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun trimTransparentMargins(original: Bitmap): Bitmap {
        val bitmap = runCatching {
            if (original.config == Bitmap.Config.HARDWARE) original.copy(Bitmap.Config.ARGB_8888, false) else original
        }.getOrDefault(original)
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 12) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < left || bottom < top) return bitmap
        val pad = (maxOf(bitmap.width, bitmap.height) * 0.035f).toInt().coerceAtLeast(2)
        left = (left - pad).coerceAtLeast(0)
        top = (top - pad).coerceAtLeast(0)
        right = (right + pad).coerceAtMost(bitmap.width - 1)
        bottom = (bottom + pad).coerceAtMost(bitmap.height - 1)
        val croppedWidth = right - left + 1
        val croppedHeight = bottom - top + 1
        if (croppedWidth >= bitmap.width * 0.96f && croppedHeight >= bitmap.height * 0.96f) return bitmap
        return runCatching { Bitmap.createBitmap(bitmap, left, top, croppedWidth, croppedHeight) }.getOrDefault(bitmap)
    }

    fun shutdown() {
        executor.shutdownNow()
        memoryCache.evictAll()
    }

    private fun memoryBudget(): Int = (Runtime.getRuntime().maxMemory() / 16L).coerceAtMost(24L * 1024L * 1024L).toInt()
}
