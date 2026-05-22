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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.localdownloader.domain.models.YoutubeAuthConfig
import com.localdownloader.R
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferencePageScaffold
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
    val hasYoutubeCookie = remember(uiState.cookieProfiles) {
        CookieTextCodec.findBestMatch(uiState.cookieProfiles, "https://www.youtube.com") != null
    }

    PreferencePageScaffold(
        title = stringResource(R.string.youtube_access_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
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
                                text = stringResource(R.string.youtube_access_use_saved),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.youtube_access_use_saved_body),
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
                        title = if (authConfig.isConfigured()) stringResource(R.string.youtube_access_ready_title) else stringResource(R.string.youtube_access_not_ready_title),
                        subtitle = when {
                            authConfig.isConfigured() && hasYoutubeCookie ->
                                stringResource(R.string.youtube_access_ready_body)
                            authConfig.isConfigured() ->
                                stringResource(R.string.youtube_access_missing_cookie_body)
                            else ->
                                stringResource(R.string.youtube_access_not_ready_body)
                        },
                    )
                    Button(
                        onClick = onGenerateAccess,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.youtube_access_login_generate), modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        text = stringResource(R.string.youtube_access_intro_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (authConfig.isConfigured()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.youtube_access_saved_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        DetailRow(stringResource(R.string.youtube_access_client), authConfig.clientHint)
                        DetailRow(stringResource(R.string.youtube_access_cookie_saved), if (hasYoutubeCookie) stringResource(R.string.common_yes) else stringResource(R.string.common_no))
                        DetailRow(stringResource(R.string.youtube_access_gvs_token), previewToken(authConfig.gvsToken))
                        DetailRow(stringResource(R.string.youtube_access_player_token), previewToken(authConfig.playerToken))
                        DetailRow(stringResource(R.string.youtube_access_subtitle_token), previewToken(authConfig.subsToken.ifBlank { authConfig.playerToken }))
                        DetailRow(stringResource(R.string.youtube_access_data_sync_id), previewToken(authConfig.dataSyncId))
                        DetailRow(
                            stringResource(R.string.youtube_access_updated),
                            if (authConfig.updatedAtEpochMs > 0L) {
                                DateFormat.getDateTimeInstance().format(authConfig.updatedAtEpochMs)
                            } else {
                                stringResource(R.string.common_unknown)
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onGenerateAccess,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.youtube_access_regenerate), modifier = Modifier.padding(start = 8.dp))
                            }
                            TextButton(
                                onClick = onClear,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.youtube_access_clear_tokens))
                            }
                        }
                    }
                }
            }
        }
        infoMessage?.let { message ->
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.youtube_access_feedback_label),
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                )
            }
        }
        errorMessage?.let { message ->
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.youtube_access_feedback_label),
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf(context.getString(R.string.youtube_access_login_title)) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val initialUrl = YoutubePoTokenGenerator.SAMPLE_VIDEO_URL

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                                        context.getString(R.string.youtube_access_login_not_ready)
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
                                    errorMessage = error.message ?: context.getString(R.string.youtube_access_login_error)
                                }
                            }
                        },
                    ) {
                        Text(if (isSaving) stringResource(R.string.youtube_access_login_saving) else stringResource(R.string.youtube_access_login_save))
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
                            text = stringResource(R.string.youtube_access_login_intro),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = stringResource(R.string.youtube_access_login_body),
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
