package com.lumen.researchenglish.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.ui.theme.Indigo

private const val BAIR_ARCHIVE_URL = "https://bair.berkeley.edu/blog/archive/"
private const val BAIR_HOST = "bair.berkeley.edu"

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun BairScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val translation by viewModel.translation.collectAsStateWithLifecycle()
    val speechLoadingId by viewModel.speechLoadingId.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingId.collectAsStateWithLifecycle()
    var selectedText by remember { mutableStateOf("") }
    var sourceTitle by remember { mutableStateOf("BAIR Research Blog") }
    var currentUrl by remember { mutableStateOf(BAIR_ARCHIVE_URL) }
    var translationExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    val selectionBridge = remember(viewModel) {
        BairSelectionBridge { text, title, url ->
            selectedText = text
            sourceTitle = title.ifBlank { "BAIR Research Blog" }
            currentUrl = url.ifBlank { BAIR_ARCHIVE_URL }
            translationExpanded = false
            viewModel.prepareExternalSelection(text)
        }
    }

    val webView = remember(context, selectionBridge) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.textZoom = 105
            addJavascriptInterface(selectionBridge, "LumenBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val uri = request.url
                    if (uri.scheme == "https" && uri.host.equals(BAIR_HOST, ignoreCase = true)) {
                        return false
                    }
                    openExternal(context, uri)
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    loading = true
                    selectedText = ""
                    translationExpanded = false
                    currentUrl = url
                    canGoBack = view.canGoBack()
                }

                override fun onPageFinished(view: WebView, url: String) {
                    loading = false
                    currentUrl = url
                    canGoBack = view.canGoBack()
                    view.evaluateJavascript(SELECTION_SCRIPT, null)
                }
            }
            loadUrl(BAIR_ARCHIVE_URL)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            viewModel.stopSpeech()
            webView.removeJavascriptInterface("LumenBridge")
            webView.stopLoading()
            webView.destroy()
        }
    }

    val speechId = remember(selectedText) { "bair-${selectedText.hashCode()}" }
    val speechLoading = speechLoadingId == speechId
    val speaking = speakingId == speechId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "BAIR Research Blog",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Berkeley AI Research · official archive",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
            IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous BAIR page")
            }
            IconButton(onClick = { webView.loadUrl(BAIR_ARCHIVE_URL) }) {
                Icon(Icons.Outlined.Home, "BAIR archive")
            }
            IconButton(onClick = webView::reload) {
                Icon(Icons.Outlined.Refresh, "Reload BAIR")
            }
            IconButton(onClick = { openExternal(context, Uri.parse(currentUrl)) }) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser")
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )

            if (selectedText.isNotBlank()) {
                BairSelectionCard(
                    text = selectedText,
                    translation = translation,
                    translationExpanded = translationExpanded,
                    speechLoading = speechLoading,
                    speaking = speaking,
                    onTranslate = {
                        translationExpanded = true
                        viewModel.translateSelection(selectedText)
                    },
                    onSpeak = { viewModel.speak(selectedText, speechId) },
                    onSave = {
                        viewModel.saveExternalSelection(selectedText, translation, sourceTitle)
                        selectedText = ""
                        translationExpanded = false
                        webView.evaluateJavascript("window.getSelection().removeAllRanges();", null)
                    },
                    onClose = {
                        viewModel.stopSpeech()
                        selectedText = ""
                        translationExpanded = false
                        webView.evaluateJavascript("window.getSelection().removeAllRanges();", null)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun BairSelectionCard(
    text: String,
    translation: String,
    translationExpanded: Boolean,
    speechLoading: Boolean,
    speaking: Boolean,
    onTranslate: () -> Unit,
    onSpeak: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F8FC)),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Close, "Close selection")
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onTranslate) {
                    Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Translate")
                }
                TextButton(onClick = onSpeak) {
                    when {
                        speechLoading -> CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        speaking -> Icon(
                            Icons.Outlined.StopCircle,
                            contentDescription = null,
                            tint = Indigo,
                            modifier = Modifier.size(19.dp),
                        )
                        else -> Icon(
                            Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(if (speaking) "Stop" else "Read")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onSave,
                    enabled = translation.isNotBlank(),
                ) {
                    Text("Save word")
                }
            }
            if (translationExpanded) {
                Spacer(Modifier.height(7.dp))
                Text(
                    translation.ifBlank { "Translating…" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private class BairSelectionBridge(
    private val onSelection: (String, String, String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSelectionChanged(text: String, title: String, url: String) {
        val clean = text.trim().take(4_000)
        if (clean.isBlank()) return
        mainHandler.post { onSelection(clean, title.take(300), url.take(2_000)) }
    }
}

private fun openExternal(context: Context, uri: Uri) {
    if (uri.scheme !in setOf("https", "http")) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private val SELECTION_SCRIPT = """
    (function() {
      if (window.__lumenSelectionInstalled) return;
      window.__lumenSelectionInstalled = true;
      var selectionTimer = null;
      document.addEventListener('selectionchange', function() {
        window.clearTimeout(selectionTimer);
        selectionTimer = window.setTimeout(function() {
          var selection = window.getSelection();
          var text = selection ? selection.toString().trim() : '';
          if (text && window.LumenBridge) {
            window.LumenBridge.onSelectionChanged(
              text.substring(0, 4000),
              document.title || 'BAIR Research Blog',
              window.location.href
            );
          }
        }, 160);
      }, { passive: true });
    })();
""".trimIndent()
