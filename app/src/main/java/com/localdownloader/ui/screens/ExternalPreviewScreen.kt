package com.localdownloader.ui.screens

import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.localdownloader.ui.model.ExternalOpenRequest
import java.io.File

@Composable
fun ExternalPreviewScreen(
    request: ExternalOpenRequest,
    mode: ExternalPreviewMode,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    text = request.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when (mode) {
            ExternalPreviewMode.IMAGE -> GifImagePreview(request = request, modifier = Modifier.weight(1f))
            ExternalPreviewMode.WEB -> WebArchivePreview(request = request, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GifImagePreview(
    request: ExternalOpenRequest,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = File(request.path),
            imageLoader = imageLoader,
            contentDescription = request.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun WebArchivePreview(
    request: ExternalOpenRequest,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                configurePreviewSettings(request)
                loadPreviewContent(request)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            webView.configurePreviewSettings(request)
            webView.loadPreviewContent(request)
        },
    )
}

private fun WebView.configurePreviewSettings(request: ExternalOpenRequest) {
    val isWebArchiveFile = request.path.lowercase().endsWith(".mhtml") || request.path.lowercase().endsWith(".mht")
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
    }
    settings.javaScriptEnabled = false
    settings.loadsImagesAutomatically = true
    settings.allowFileAccess = isWebArchiveFile
    settings.allowContentAccess = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        settings.safeBrowsingEnabled = true
    }
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.allowFileAccessFromFileURLs = false
    settings.allowUniversalAccessFromFileURLs = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
}

private fun WebView.loadPreviewContent(request: ExternalOpenRequest) {
    val file = File(request.path)
    val extension = file.extension.lowercase()
    if (extension == "mhtml" || extension == "mht") {
        loadUrl(file.toURI().toString())
        return
    }

    val mimeType = when {
        request.mimeType?.contains("xhtml", ignoreCase = true) == true -> "application/xhtml+xml"
        else -> "text/html"
    }
    val html = runCatching { file.readText() }.getOrElse {
        "<html><body><pre>Unable to preview this file.</pre></body></html>"
    }
    loadDataWithBaseURL(
        "about:blank",
        html,
        mimeType,
        "utf-8",
        null,
    )
}

enum class ExternalPreviewMode {
    IMAGE,
    WEB,
}
