package com.localdownloader.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.localdownloader.R
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.ui.components.PreferencesHintCard
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.WebViewCookieExporter
import com.localdownloader.utils.WebViewSessionSanitizer
import com.localdownloader.utils.configureRestrictedJavascriptSession
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesScreen(
    uiState: FormatUiState,
    onBack: () -> Unit,
    onCookiesEnabledChanged: (Boolean) -> Unit,
    onCookieUserAgentEnabledChanged: (Boolean) -> Unit,
    onSaveCookie: (String?, String, String) -> Unit,
    onDeleteCookie: (String) -> Unit,
    onDeleteAllCookies: () -> Unit,
    onImportCookieText: (String) -> Unit,
    onOpenCookieCapture: (String, String?) -> Unit,
    onCookieExportResult: (Boolean, String?) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val infoMessage = uiState.infoMessageFor(FormatMessageScope.COOKIES)
    val errorMessage = uiState.errorMessageFor(FormatMessageScope.COOKIES)
    var showMenu by remember { mutableStateOf(false) }
    var editorState by remember { mutableStateOf<CookieEditorState?>(null) }
    var showQuickCaptureDialog by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(CookieTextCodec.buildCombinedExport(uiState.cookieProfiles))
            } ?: error("Unable to open the selected export file.")
        }.onSuccess {
            onCookieExportResult(true, null)
        }.onFailure { error ->
            onCookieExportResult(false, error.message)
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.cookies_delete_all)) },
            text = { Text(stringResource(R.string.cookies_delete_all_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAll = false
                        onDeleteAllCookies()
                    },
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showQuickCaptureDialog) {
        QuickCookieCaptureDialog(
            onDismiss = { showQuickCaptureDialog = false },
            onConfirm = { targetUrl ->
                showQuickCaptureDialog = false
                onOpenCookieCapture(targetUrl, null)
            },
        )
    }

    editorState?.let { editor ->
        CookieEditorDialog(
            state = editor,
            onDismiss = { editorState = null },
            onStateChanged = { editorState = it },
            onCopy = {
                clipboardManager.setText(AnnotatedString(editor.cookiesText))
            },
            onSave = {
                onSaveCookie(editor.profileId, editor.url, editor.cookiesText)
                editorState = null
            },
            onDelete = {
                editor.profileId?.let(onDeleteCookie)
                editorState = null
            },
            onPaste = {
                val pasted = clipboardManager.getText()?.text.orEmpty()
                editorState = editor.copy(cookiesText = pasted)
            },
            onGetCookies = {
                onOpenCookieCapture(editor.url, editor.profileId)
                editorState = null
            },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.cookies_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cookies_actions))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cookies_delete_all)) },
                        onClick = {
                            showMenu = false
                            confirmDeleteAll = true
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (uiState.cookieUserAgentEnabled) stringResource(R.string.cookies_user_agent_on)
                                else stringResource(R.string.cookies_user_agent_off),
                            )
                        },
                        onClick = {
                            showMenu = false
                            onCookieUserAgentEnabledChanged(!uiState.cookieUserAgentEnabled)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cookies_import_clipboard)) },
                        onClick = {
                            showMenu = false
                            val raw = clipboardManager.getText()?.text.orEmpty()
                            if (raw.isNotBlank()) {
                                onImportCookieText(raw)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cookies_export_clipboard)) },
                        onClick = {
                            showMenu = false
                            clipboardManager.setText(
                                AnnotatedString(CookieTextCodec.buildCombinedExport(uiState.cookieProfiles)),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cookies_export_file)) },
                        onClick = {
                            showMenu = false
                            exportLauncher.launch("video-downloader-cookies.txt")
                        },
                    )
                }
            }
        },
    ) {
        infoMessage?.let { message ->
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.cookies_feedback_label),
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                )
            }
        }
        errorMessage?.let { message ->
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.cookies_feedback_label),
                    message = message,
                    isError = true,
                    onDismiss = onDismissMessage,
                )
            }
        }

        item {
            PreferenceSubtitle(text = "GENERAL")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Cookie,
                title = stringResource(R.string.cookies_use_title),
                description = stringResource(R.string.cookies_use_body),
                isChecked = uiState.cookiesEnabled,
                onClick = { onCookiesEnabledChanged(!uiState.cookiesEnabled) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Security,
                title = "Send Browser User-Agent",
                description = if (uiState.cookieUserAgentEnabled) {
                    stringResource(R.string.cookies_user_agent_enabled)
                } else {
                    stringResource(R.string.cookies_user_agent_disabled)
                },
                isChecked = uiState.cookieUserAgentEnabled,
                onClick = { onCookieUserAgentEnabledChanged(!uiState.cookieUserAgentEnabled) },
            )
        }

        item {
            PreferenceSubtitle(text = "ADD COOKIE")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Language,
                title = "Log in with browser to capture",
                description = "Open website in browser, log in, and auto-capture session cookies",
                onClick = { showQuickCaptureDialog = true },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.ContentPaste,
                title = stringResource(R.string.cookies_import_clipboard),
                description = "Import Netscape cookie text from system clipboard",
                onClick = {
                    val raw = clipboardManager.getText()?.text.orEmpty()
                    if (raw.isNotBlank()) {
                        onImportCookieText(raw)
                    }
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Add,
                title = "Manual cookie editor",
                description = "Manually enter URL and paste Netscape cookie lines",
                onClick = {
                    editorState = CookieEditorState(
                        profileId = null,
                        url = "https://",
                        cookiesText = "",
                    )
                },
            )
        }

        item {
            val count = uiState.cookieProfiles.size
            PreferenceSubtitle(
                text = if (count == 0) "SAVED SITES" else "SAVED SITES ($count)",
            )
        }
        if (uiState.cookieProfiles.isEmpty()) {
            item {
                PreferencesHintCard(
                    title = stringResource(R.string.cookies_empty_title),
                    description = stringResource(R.string.cookies_empty_body),
                    icon = Icons.Rounded.Cookie,
                )
            }
        } else {
            items(uiState.cookieProfiles, key = { it.id }) { profile ->
                CookieProfileCard(
                    profile = profile,
                    onClick = {
                        editorState = CookieEditorState(
                            profileId = profile.id,
                            url = profile.url,
                            cookiesText = profile.cookiesText,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCookieCaptureDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var urlText by rememberSaveable { mutableStateOf("https://") }
    val presets = remember {
        listOf(
            "YouTube" to "https://www.youtube.com",
            "Instagram" to "https://www.instagram.com",
            "Reddit" to "https://www.reddit.com",
            "X" to "https://x.com",
            "TikTok" to "https://www.tiktok.com",
            "Facebook" to "https://www.facebook.com",
        )
    }
    val isValidUrl = !CookieTextCodec.normalizeUrl(urlText).isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capture Website Cookies") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Pick a site or enter a URL. The in-app browser will open so you can log in and save cookies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Website URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Popular services:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { (name, presetUrl) ->
                        FilterChip(
                            selected = urlText.equals(presetUrl, ignoreCase = true),
                            onClick = { urlText = presetUrl },
                            label = { Text(name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(urlText) },
                enabled = isValidUrl,
            ) {
                Text("Open Browser")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieCaptureScreen(
    url: String,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val secureCaptureUrl = remember(url) { CookieTextCodec.normalizeUrl(url) ?: url }
    var title by rememberSaveable(secureCaptureUrl) { mutableStateOf(secureCaptureUrl) }
    var isConfirming by rememberSaveable { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            WebViewSessionSanitizer.clearAndDestroy(webView)
            webView = null
        }
    }

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
                    IconButton(
                        onClick = {
                            WebViewSessionSanitizer.clearAndDestroy(webView)
                            webView = null
                            onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    TextButton(
                        enabled = !isConfirming,
                        onClick = {
                            coroutineScope.launch {
                                isConfirming = true
                                val cookieText = runCatching {
                                    withContext(Dispatchers.IO) {
                                        WebViewCookieExporter.exportForUrl(context, secureCaptureUrl)
                                    }
                                }.getOrDefault("")
                                isConfirming = false
                                WebViewSessionSanitizer.clearAndDestroy(webView)
                                webView = null
                                onConfirm(cookieText)
                            }
                        },
                    ) {
                        Text(if (isConfirming) stringResource(R.string.youtube_access_login_saving) else stringResource(R.string.cookies_capture_confirm))
                    }
                },
            )
        },
    ) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { context ->
                WebViewSessionSanitizer.resetSession()
                WebView(context).apply {
                    webView = this
                    configureRestrictedJavascriptSession(
                        enableCookies = true,
                        enableThirdPartyCookies = true,
                    )
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val scheme = request?.url?.scheme.orEmpty()
                            return scheme.isNotBlank() &&
                                !scheme.equals("https", ignoreCase = true) &&
                                !scheme.equals("about", ignoreCase = true)
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                            if (!pageTitle.isNullOrBlank()) {
                                title = pageTitle
                            }
                        }
                    }
                    loadUrl(secureCaptureUrl)
                }
            },
        )
    }
}

@Composable
private fun CookieProfileCard(
    profile: CookieProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = remember(profile.cookiesText) {
        CookieTextCodec.countCookies(profile.cookiesText)
    }
    PreferenceItem(
        icon = Icons.Rounded.Cookie,
        title = profile.url,
        description = if (count > 0) "$count cookies stored • Tap to edit or export" else "No cookies stored",
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun CookieEditorDialog(
    state: CookieEditorState,
    onDismiss: () -> Unit,
    onStateChanged: (CookieEditorState) -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPaste: () -> Unit,
    onGetCookies: () -> Unit,
) {
    val hasValidUrl = !CookieTextCodec.normalizeUrl(state.url).isNullOrBlank()
    val hasCookiesText = state.cookiesText.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (state.profileId == null) stringResource(R.string.cookies_new) else stringResource(R.string.common_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.cookies_editor_keep_one),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.cookiesText,
                    onValueChange = { onStateChanged(state.copy(cookiesText = it)) },
                    label = { Text(stringResource(R.string.cookies_editor_text)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    placeholder = { Text("# Netscape HTTP Cookie File") },
                )
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { onStateChanged(state.copy(url = it)) },
                    label = { Text(stringResource(R.string.cookies_editor_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://reddit.com") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable(onClick = onCopy),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.cookies_copy_cookies),
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(onClick = onPaste),
                        ) {
                            Text(
                                text = stringResource(R.string.cookies_editor_paste),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    if (state.profileId != null) {
                        TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
                    } else {
                        Spacer(modifier = Modifier)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onGetCookies,
                        enabled = hasValidUrl,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.cookies_editor_get))
                    }
                    Button(
                        onClick = onSave,
                        enabled = hasValidUrl && hasCookiesText,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.profileId == null) stringResource(R.string.common_save) else stringResource(R.string.common_edit))
                    }
                }
                Text(
                    text = stringResource(R.string.cookies_editor_url_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

private data class CookieEditorState(
    val profileId: String?,
    val url: String,
    val cookiesText: String,
)
