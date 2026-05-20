package com.localdownloader.ui.screens

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.viewinterop.AndroidView
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.WebViewCookieExporter
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
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val infoMessage = uiState.infoMessageFor(FormatMessageScope.COOKIES)
    val errorMessage = uiState.errorMessageFor(FormatMessageScope.COOKIES)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showMenu by remember { mutableStateOf(false) }
    var editorState by remember { mutableStateOf<CookieEditorState?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(CookieTextCodec.buildCombinedExport(uiState.cookieProfiles))
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all cookies") },
            text = { Text("This removes every saved cookie entry from Video Downloader.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAll = false
                        onDeleteAllCookies()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") }
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Cookies") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Cookie actions")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete All Cookies") },
                                onClick = {
                                    showMenu = false
                                    confirmDeleteAll = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.cookieUserAgentEnabled) {
                                            "User-Agent header: On"
                                        } else {
                                            "User-Agent header: Off"
                                        },
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onCookieUserAgentEnabledChanged(!uiState.cookieUserAgentEnabled)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Import from clipboard") },
                                onClick = {
                                    showMenu = false
                                    val raw = clipboardManager.getText()?.text.orEmpty()
                                    if (raw.isNotBlank()) {
                                        onImportCookieText(raw)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export to clipboard") },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(
                                        AnnotatedString(CookieTextCodec.buildCombinedExport(uiState.cookieProfiles)),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export File") },
                                onClick = {
                                    showMenu = false
                                    exportLauncher.launch("video-downloader-cookies.txt")
                                },
                            )
                        }
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
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
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
                                text = "Use Cookies",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Attach saved website cookies automatically when a matching link is analyzed or downloaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.cookiesEnabled,
                            onCheckedChange = onCookiesEnabledChanged,
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = if (uiState.cookieUserAgentEnabled) {
                                "User-Agent header is enabled for cookie-backed requests."
                            } else {
                                "Turn on the User-Agent header from the top menu if a website is strict about browser sessions."
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saved Cookies",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (uiState.cookieProfiles.isEmpty()) {
                            "Add a site cookie once, then update it whenever that session changes."
                        } else {
                            "${uiState.cookieProfiles.size} saved site${if (uiState.cookieProfiles.size == 1) "" else "s"} ready to use."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        editorState = CookieEditorState(
                            profileId = null,
                            url = "https://",
                            cookiesText = "",
                        )
                    },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("New Cookie", modifier = Modifier.padding(start = 8.dp))
                }
            }

            infoMessage?.let { message ->
                InlineFeedbackCard(
                    label = "Cookies",
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
            errorMessage?.let { message ->
                InlineFeedbackCard(
                    label = "Cookies",
                    message = message,
                    isError = true,
                    onDismiss = onDismissMessage,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }

            if (uiState.cookieProfiles.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "No cookies saved yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Tap New Cookie to paste a Netscape cookie file or use Get Cookies to capture a signed-in browser session for a specific site.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(
                                text = "For YouTube, a private/incognito session cookie usually works best for signed-in or restricted videos.",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieCaptureScreen(
    url: String,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable { mutableStateOf(url) }
    var isConfirming by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                        enabled = !isConfirming,
                        onClick = {
                            coroutineScope.launch {
                                isConfirming = true
                                val cookieText = runCatching {
                                    withContext(Dispatchers.IO) {
                                        WebViewCookieExporter.exportForUrl(context, url)
                                    }
                                }.getOrDefault("")
                                isConfirming = false
                                onConfirm(cookieText)
                            }
                        },
                    ) {
                        Text(if (isConfirming) "Saving..." else "OK")
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
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                WebView(context).apply {
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
                    loadUrl(url)
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = profile.url,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = CookieTextCodec.previewText(profile.cookiesText),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Tap to update this cookie",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
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
            Text(if (state.profileId == null) "New Cookie" else "Update Cookie")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Keep one cookie profile per website so Video Downloader can reuse it automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.cookiesText,
                    onValueChange = { onStateChanged(state.copy(cookiesText = it)) },
                    label = { Text("Cookies") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    placeholder = { Text("# Netscape HTTP Cookie File") },
                )
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { onStateChanged(state.copy(url = it)) },
                    label = { Text("URL") },
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
                                contentDescription = "Copy cookies",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(onClick = onPaste),
                        ) {
                            Text(
                                text = "Paste",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    if (state.profileId != null) {
                        TextButton(onClick = onDelete) { Text("Delete") }
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
                        Text("Get Cookies")
                    }
                    Button(
                        onClick = onSave,
                        enabled = hasValidUrl && hasCookiesText,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.profileId == null) "Save" else "Update")
                    }
                }
                Text(
                    text = "Tip: keep the URL in the form https://example.com so matching works more reliably.",
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
