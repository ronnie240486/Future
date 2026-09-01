package com.futuretv.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class ImageLoader(context: Context) {
    // Antes: sem cache em disco, cache em memória de só ~24MB, e cada
    // imagem era decodificada em RESOLUÇÃO ORIGINAL (um pôster do TMDB
    // pode ter vários MB decodificado). Com centenas de séries, isso fazia
    // o app baixar E decodificar imagem gigante uma por uma (só 4 threads
    // ao mesmo tempo), o cache de 24MB lotava quase na hora, e ao rolar a
    // lista pra cima e pra baixo a mesma capinha era baixada e decodificada
    // de novo repetidas vezes -- daí os minutos de espera. Agora: baixa em
    // tamanho reduzido (adequado pra uma capinha de grade) e guarda em
    // disco, então na segunda vez (inclusive depois de fechar o app) é
    // leitura local, não rede.
    // Imagens agora saem bem menores (decodificadas em ate ~480px em vez de
    // resolucao original), entao cada download/decodificacao e mais leve.
    // Um numero mais alto aqui (tentei 6 antes) deixava o sistema travando
    // numa TV box fraca com varias decodificacoes simultaneas -- 5 e um
    // meio termo mais seguro.
    private val executor = Executors.newFixedThreadPool(5)
    private val memoryCache = object : LruCache<String, Bitmap>(memoryBudget()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }
    private val diskCacheDir = File(context.cacheDir, "covers").apply { mkdirs() }
    // Tamanho alvo pra decodificação -- generoso o bastante pra qualquer
    // card de grade nesse app, mas bem menor que os 1000x1500+ que o TMDB
    // costuma servir.
    private val maxDecodeDimension = 480

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
            val diskFile = diskCacheFile(key)
            val downloaded = if (diskFile.exists()) {
                runCatching { BitmapFactory.decodeFile(diskFile.absolutePath) }.getOrNull()
            } else {
                val fresh = download(url.trim())
                if (fresh != null) runCatching { saveToDisk(fresh, diskFile) }
                fresh
            }
            val bitmap = if (cropTransparent && downloaded != null) trimTransparentMargins(downloaded) else downloaded
            if (bitmap != null) memoryCache.put(key, bitmap)
            if (bitmap != null) {
                target.post { if (target.tag == key) target.setImageBitmap(bitmap) }
            }
        }
    }

    private fun diskCacheFile(key: String): File {
        val digest = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        return File(diskCacheDir, "$name.jpg")
    }

    private fun saveToDisk(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
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
            val bytes = connection.inputStream.use { it.readBytes() }
            decodeDownsampled(bytes)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    // Lê só as dimensões primeiro (sem alocar a imagem inteira), calcula
    // quanto reduzir, e só então decodifica de fato -- em vez de sempre
    // carregar o arquivo em resolução original pra memória.
    private fun decodeDownsampled(bytes: ByteArray): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        var sampleSize = 1
        while (boundsOptions.outWidth / (sampleSize * 2) >= maxDecodeDimension ||
            boundsOptions.outHeight / (sampleSize * 2) >= maxDecodeDimension
        ) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

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

    // Com imagens bem menores agora (decodificadas em até ~480px), o mesmo
    // orçamento de memória guarda muito mais capinhas ao mesmo tempo do que
    // antes guardava com imagens em resolução original.
    private fun memoryBudget(): Int = (Runtime.getRuntime().maxMemory() / 1024L / 8L).coerceAtMost(64L * 1024L).toInt()
}
