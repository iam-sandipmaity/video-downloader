package com.localdownloader.ui.screens

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.localdownloader.domain.models.YoutubeAuthConfig
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.WebViewCookieExporter
import com.localdownloader.utils.YoutubePoTokenGenerator
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeAuthScreen(
    uiState: FormatUiState,
    onBack: () -> Unit,
    onGenerateAccess: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authConfig = uiState.youtubeAuthConfig
    val infoMessage = uiState.infoMessageFor(FormatMessageScope.YOUTUBE_ACCESS)
    val errorMessage = uiState.errorMessageFor(FormatMessageScope.YOUTUBE_ACCESS)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hasYoutubeCookie = remember(uiState.cookieProfiles) {
        CookieTextCodec.findBestMatch(uiState.cookieProfiles, "https://www.youtube.com") != null
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("YouTube Access") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Use saved YouTube access",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Applies your YouTube cookies, PO tokens, and data-sync session hints on tougher downloads.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = authConfig.enabled && authConfig.isConfigured(),
                            onCheckedChange = onEnabledChanged,
                        )
                    }
                    StatusChip(
                        title = if (authConfig.isConfigured()) "Ready for long-form retries" else "Not configured yet",
                        subtitle = when {
                            authConfig.isConfigured() && hasYoutubeCookie ->
                                "Cookies and PO tokens are both saved."
                            authConfig.isConfigured() ->
                                "PO tokens are saved, but the YouTube cookie is missing."
                            else ->
                                "Generate access once, then retry the blocked YouTube link."
                        },
                    )
                    Button(
                        onClick = onGenerateAccess,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Login & Generate Access", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        text = "This opens a dedicated YouTube sign-in page, captures the matching cookie session, and generates the Web client PO tokens that yt-dlp needs for higher-quality retries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (authConfig.isConfigured()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Saved Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        DetailRow("Client", authConfig.clientHint)
                        DetailRow("Cookie saved", if (hasYoutubeCookie) "Yes" else "No")
                        DetailRow("GVS token", previewToken(authConfig.gvsToken))
                        DetailRow("Player token", previewToken(authConfig.playerToken))
                        DetailRow("Subtitle token", previewToken(authConfig.subsToken.ifBlank { authConfig.playerToken }))
                        DetailRow("Data sync ID", previewToken(authConfig.dataSyncId))
                        DetailRow(
                            "Updated",
                            if (authConfig.updatedAtEpochMs > 0L) {
                                DateFormat.getDateTimeInstance().format(authConfig.updatedAtEpochMs)
                            } else {
                                "Unknown"
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onGenerateAccess,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Regenerate", modifier = Modifier.padding(start = 8.dp))
                            }
                            TextButton(
                                onClick = onClear,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Clear tokens")
                            }
                        }
                    }
                }
            }

            infoMessage?.let { message ->
                InlineFeedbackCard(
                    label = "YouTube access",
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                )
            }
            errorMessage?.let { message ->
                InlineFeedbackCard(
                    label = "YouTube access",
                    message = message,
                    isError = true,
                    onDismiss = onDismissMessage,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeAuthLoginScreen(
    onBack: () -> Unit,
    onConfirm: (String, YoutubeAuthConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("YouTube Login") }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val initialUrl = YoutubePoTokenGenerator.SAMPLE_VIDEO_URL

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = !isSaving,
                        onClick = {
                            val currentWebView = webView ?: return@TextButton
                            coroutineScope.launch {
                                isSaving = true
                                errorMessage = null
                                runCatching {
                                    val visitorData = currentWebView.awaitJavascriptString(
                                        """
                                        (function () {
                                            try {
                                                return (window.ytcfg && ytcfg.get && (ytcfg.get('VISITOR_DATA') || '')) ||
                                                    (window.yt && yt.config_ && yt.config_.VISITOR_DATA) ||
                                                    '';
                                            } catch (error) {
                                                return '';
                                            }
                                        })();
                                        """.trimIndent(),
                                    )
                                    check(visitorData.isNotBlank()) {
                                        "YouTube visitor data is not ready yet. Let the sample video page finish loading, then try Save Access again."
                                    }
                                    val dataSyncId = currentWebView.awaitJavascriptString(
                                        """
                                        (function () {
                                            try {
                                                return (window.ytcfg && ytcfg.get && (ytcfg.get('DATASYNC_ID') || '')) || '';
                                            } catch (error) {
                                                return '';
                                            }
                                        })();
                                        """.trimIndent(),
                                    )
                                    val cookieText = withContext(Dispatchers.IO) {
                                        WebViewCookieExporter.exportForUrl(
                                            context = currentWebView.context,
                                            url = "https://www.youtube.com",
                                        )
                                    }
                                    val generated = YoutubePoTokenGenerator.generate(
                                        context = currentWebView.context,
                                        visitorData = visitorData,
                                        dataSyncId = dataSyncId,
                                    )
                                    cookieText to YoutubeAuthConfig(
                                        enabled = true,
                                        clientHint = "web.gvs",
                                        gvsToken = generated.gvsToken,
                                        playerToken = generated.playerToken,
                                        subsToken = generated.subsToken,
                                        visitorData = generated.visitorData,
                                        dataSyncId = generated.dataSyncId,
                                    )
                                }.onSuccess { (cookieText, authConfig) ->
                                    onConfirm(cookieText, authConfig)
                                }.onFailure { error ->
                                    isSaving = false
                                    errorMessage = error.message ?: "Unable to generate YouTube access."
                                }
                            }
                        },
                    ) {
                        Text(if (isSaving) "Saving..." else "Save Access")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null)
                        Text(
                            text = "Sign in if needed, let the sample YouTube page load, then tap Save Access.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "This screen refreshes its own WebView session so the saved cookie and generated tokens belong to the same YouTube login flow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        WebStorage.getInstance().deleteAllData()
                        WebView(context).apply {
                            webView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                                    if (!pageTitle.isNullOrBlank()) {
                                        title = pageTitle
                                    }
                                }
                            }
                            loadUrl(initialUrl)
                        }
                    },
                )
            }
        }
    }
}

private suspend fun WebView.awaitJavascriptString(script: String): String {
    return suspendCancellableCoroutine { continuation ->
        evaluateJavascript(script) { raw ->
            continuation.resume(decodeJavascriptValue(raw))
        }
    }
}

private fun decodeJavascriptValue(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank() || value == "null") return ""
    return runCatching {
        Json.decodeFromString<String>(value)
    }.getOrElse {
        value.removePrefix("\"").removeSuffix("\"")
    }
}

@Composable
private fun StatusChip(
    title: String,
    subtitle: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.ifBlank { "Not set" },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun previewToken(token: String): String {
    val trimmed = token.trim()
    if (trimmed.isBlank()) return "Not set"
    return if (trimmed.length <= 26) trimmed else trimmed.take(12) + "..." + trimmed.takeLast(10)
}
