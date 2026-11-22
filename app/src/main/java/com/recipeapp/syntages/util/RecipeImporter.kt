package com.recipeapp.syntages.util

import android.content.Context
import android.net.Uri
import com.recipeapp.syntages.domain.WebRecipe
import com.recipeapp.syntages.domain.WebRecipeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

/** Κατεβάζει μια σελίδα συνταγής και τη μετατρέπει σε WebRecipe. Χρειάζεται internet μόνο τη στιγμή της εισαγωγής. */
object RecipeImporter {
    private const val UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
    private const val MAX_BYTES = 6 * 1024 * 1024

    private val urlRegex = Regex("https?://[^\\s\"'<>]+")

    fun extractUrl(text: String?): String? =
        text?.let { urlRegex.find(it)?.value?.trimEnd(')', '.', ',', ';', '!', '?', '»', '"') }

    fun googleSearchUrl(query: String): String =
        "https://www.google.com/search?q=" + URLEncoder.encode("συνταγή $query", "UTF-8")

    suspend fun load(url: String): WebRecipe = withContext(Dispatchers.IO) {
        val html = fetchText(url)
        WebRecipeParser.parse(html, url) ?: throw IOException("Δεν βρέθηκε συνταγή σε αυτή τη σελίδα")
    }

    /** Κατεβάζει τη φωτογραφία και την περνά από το PhotoStore. Επιστρέφει το όνομα αρχείου. */
    suspend fun downloadImage(context: Context, url: String): String? {
        val tmp = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = fetchBytes(url).first
                File(File(context.cacheDir, "camera").apply { mkdirs() }, "web_${System.currentTimeMillis()}.img")
                    .apply { writeBytes(bytes) }
            }.getOrNull()
        } ?: return null
        return try { PhotoStore.import(context, Uri.fromFile(tmp)) } finally { tmp.delete() }
    }

    private fun fetchText(url: String): String {
        val (bytes, contentType) = fetchBytes(url)
        val headerCharset = contentType?.let { Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
        val head = String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1)
        val metaCharset = Regex("<meta[^>]+charset=[\"']?([\\w-]+)", RegexOption.IGNORE_CASE).find(head)?.groupValues?.get(1)
        val cs = listOfNotNull(headerCharset, metaCharset).firstNotNullOfOrNull { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
        return String(bytes, cs)
    }

    private fun fetchBytes(start: String): Pair<ByteArray, String?> {
        var current = start
        repeat(6) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,image/*,*/*;q=0.8")
                setRequestProperty("Accept-Language", "el-GR,el;q=0.9,en;q=0.7")
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: throw IOException("Λάθος ανακατεύθυνση")
                    current = URL(URL(current), loc).toString()
                    return@repeat
                }
                if (code !in 200..299) throw IOException("Η σελίδα απάντησε με σφάλμα $code")
                val out = ByteArrayOutputStream()
                conn.inputStream.use { input ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        if (out.size() > MAX_BYTES) break
                    }
                }
                return out.toByteArray() to conn.contentType
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Πάρα πολλές ανακατευθύνσεις")
    }
}
