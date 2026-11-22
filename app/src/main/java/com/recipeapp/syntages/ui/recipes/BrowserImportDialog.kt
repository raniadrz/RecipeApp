package com.recipeapp.syntages.ui.recipes

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.recipeapp.syntages.domain.WebRecipeParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

/**
 * Ανοίγει τη σελίδα σε ενσωματωμένο browser (σαν να την άνοιγες στο Chrome) και περιμένει
 * να εμφανιστεί η συνταγή. Χρειάζεται για σάιτ που μπλοκάρουν την απευθείας λήψη (σφάλμα 403).
 * Αν το σάιτ ζητήσει επιβεβαίωση («δεν είμαι ρομπότ»), ο χρήστης τη βλέπει και την πατάει.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserImportDialog(url: String, onResult: (html: String?, finalUrl: String?) -> Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var lastHtml by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    fun finish(html: String?) {
        if (done) return
        done = true
        onResult(html, webView?.url)
    }

    Dialog(onDismissRequest = { finish(lastHtml) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Διαβάζω τη συνταγή…", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (seconds < 12) "Περίμενε να φορτώσει η σελίδα."
                            else "Αν η σελίδα ζητάει επιβεβαίωση, πάτα την. Ή πάτα «Εισαγωγή τώρα».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
                                override fun onPageStarted(view: WebView, u: String?, favicon: Bitmap?) {}
                            }
                            loadUrl(url)
                            webView = this
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = { finish(null) }) { Text("Ακύρωση") }
                    Button(onClick = {
                        val wv = webView
                        if (wv == null) finish(lastHtml) else wv.evaluateJavascript(JS_HTML) { finish(decode(it) ?: lastHtml) }
                    }) { Text("Εισαγωγή τώρα") }
                }
            }
        }
    }

    // Κάθε δευτερόλεπτο ελέγχουμε αν η σελίδα έχει πια τη συνταγή.
    LaunchedEffect(webView) {
        val wv = webView ?: return@LaunchedEffect
        while (!done) {
            delay(1000)
            seconds++
            val html = suspendCancellableCoroutine { cont ->
                wv.evaluateJavascript(JS_HTML) { cont.resume(decode(it)) }
            }
            if (html != null) {
                lastHtml = html
                val r = WebRecipeParser.parse(html, url)
                if (r != null && r.isComplete) { finish(html); break }
            }
            if (seconds >= 60) { finish(lastHtml); break }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply { stopLoading(); destroy() }
        }
    }
}

private const val JS_HTML = "(function(){return document.documentElement.outerHTML;})()"

private fun decode(jsResult: String?): String? {
    if (jsResult.isNullOrBlank() || jsResult == "null") return null
    return runCatching { Json.decodeFromString<String>(jsResult) }.getOrNull()
}
