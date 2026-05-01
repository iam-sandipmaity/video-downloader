package com.localdownloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun SettingsScreen(
    uiState: FormatUiState,
    savedItemsCount: Int = 0,
    mediaInfoMessage: String? = null,
    mediaErrorMessage: String? = null,
    onDismissMediaLibraryMessage: () -> Unit = {},
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    onOutputTemplateChanged: (String) -> Unit,
    onContainerChanged: (String) -> Unit,
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onAutoRemoveMissingFilesFromLibraryChanged: (Boolean) -> Unit,
    onDeleteFromStorageWhenRemovedInAppChanged: (Boolean) -> Unit,
    onClearVideoTabEntries: () -> Unit,
    onDeleteAllSavedMedia: () -> Unit,
    onYoutubeAuthEnabledChanged: (Boolean) -> Unit,
    onYoutubePoTokenChanged: (String) -> Unit,
    onYoutubePoTokenClientHintChanged: (String) -> Unit,
    onYoutubeCookiesPathChanged: (String) -> Unit,
    onPickYoutubeCookies: () -> Unit,
    onPickYoutubeAuthBundle: () -> Unit,
    onSaveClicked: () -> Unit,
    onClearCache: () -> Unit,
    cacheSize: Long = 0L,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val showAuthSection = remember { mutableStateOf(false) }
    val showReportDialog = remember { mutableStateOf(false) }
    val showLibraryClearDialog = remember { mutableStateOf(false) }
    val showDeleteAllMediaDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    if (showReportDialog.value) {
        ReportBugDialog(
            onDismiss = { showReportDialog.value = false },
            onOpenIssues = {
                openUrl("https://github.com/iam-sandipmaity/video-downloader/issues")
                showReportDialog.value = false
            },
        )
    }

    if (showLibraryClearDialog.value) {
        AlertDialog(
            onDismissRequest = { showLibraryClearDialog.value = false },
            title = { Text("Remove items from Video tab") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This removes all saved entries from the app's Video tab only.")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Files stay in your device file manager.", style = MaterialTheme.typography.bodySmall)
                            Text("Playback history inside the app may be lost.", style = MaterialTheme.typography.bodySmall)
                            Text("This action affects $savedItemsCount saved item(s).", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearVideoTabEntries()
                        showLibraryClearDialog.value = false
                    },
                ) {
                    Text("Remove from app")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLibraryClearDialog.value = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteAllMediaDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAllMediaDialog.value = false },
            title = { Text("Delete all saved media") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This permanently deletes saved media files from both the app and your device storage.")
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "Data may be lost and cannot be recovered from inside the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Playlist folders and exported downloads may also be removed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "This action affects $savedItemsCount saved item(s).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllSavedMedia()
                        showDeleteAllMediaDialog.value = false
                    },
                ) {
                    Text("Delete permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllMediaDialog.value = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────
        SettingsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Appearance ────────────────────────────────────────────
            SettingsIconCard(
                icon = Icons.Outlined.DarkMode,
                title = "Appearance",
                subtitle = "Theme and visual preferences",
            ) {
                ToggleRow(
                    title = "Dark theme",
                    subtitle = "Switch between light and dark",
                    checked = uiState.isDarkTheme,
                    onCheckedChange = { onDarkThemeChanged?.invoke(it) },
                )
            }

            // ── Output ────────────────────────────────────────────────
            SettingsIconCard(
                icon = Icons.Outlined.Description,
                title = "Output & Metadata",
                subtitle = "Default file naming and embedded info",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.outputTemplate,
                        onValueChange = onOutputTemplateChanged,
                        label = { Text("Filename template") },
                        supportingText = { Text("yt-dlp template, e.g. %(title)s.%(ext)s") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    FormatDropdown(
                        label = "Default container",
                        options = listOf("mp4", "webm", "mkv", "mov"),
                        selected = uiState.selectedContainer.ifBlank { "mp4" },
                        onSelected = onContainerChanged,
                    )
                    HorizontalDivider()
                    ToggleRow(
                        title = "Embed metadata",
                        subtitle = "Write title, artist, album tags",
                        checked = uiState.embedMetadata,
                        onCheckedChange = onEmbedMetadataChanged,
                    )
                    ToggleRow(
                        title = "Embed thumbnail",
                        subtitle = "Attach cover art to the file",
                        checked = uiState.embedThumbnail,
                        onCheckedChange = onEmbedThumbnailChanged,
                    )
                }
            }

            SettingsIconCard(
                icon = Icons.Outlined.Settings,
                title = "Media Library",
                subtitle = "Keep saved items aligned with device storage",
            ) {
                ToggleRow(
                    title = "Auto-remove missing files",
                    subtitle = "Clean up library entries when the real file was deleted elsewhere",
                    checked = uiState.autoRemoveMissingFilesFromLibrary,
                    onCheckedChange = onAutoRemoveMissingFilesFromLibraryChanged,
                )
                ToggleRow(
                    title = "Delete from storage when removed",
                    subtitle = "When removing media in the app, also delete the real device file",
                    checked = uiState.deleteFromStorageWhenRemovedInApp,
                    onCheckedChange = onDeleteFromStorageWhenRemovedInAppChanged,
                )
                HorizontalDivider()
                Text(
                    text = "$savedItemsCount saved item(s) currently listed in the Video tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showLibraryClearDialog.value = true },
                        modifier = Modifier.weight(1f),
                        enabled = savedItemsCount > 0,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Remove from app")
                    }
                    TextButton(
                        onClick = { showDeleteAllMediaDialog.value = true },
                        modifier = Modifier.weight(1f),
                        enabled = savedItemsCount > 0,
                    ) {
                        Text("Delete all media")
                    }
                }
                Text(
                    text = "Bulk cleanup lives here so accidental taps inside the Video tab are less likely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── YouTube Auth ──────────────────────────────────────────
            SettingsIconCard(
                icon = Icons.Outlined.Visibility,
                title = "YouTube authentication",
                subtitle = "Access age-gated or private content",
            ) {
                YouTubeAuthSection(
                    authEnabled = uiState.youtubeAuthEnabled,
                    cookiesPath = uiState.youtubeCookiesPath,
                    poToken = uiState.youtubePoToken,
                    poTokenHint = uiState.youtubePoTokenClientHint,
                    expanded = showAuthSection.value,
                    onExpand = { showAuthSection.value = it },
                    onAuthEnabledChanged = onYoutubeAuthEnabledChanged,
                    onPoTokenChanged = onYoutubePoTokenChanged,
                    onPoTokenClientHintChanged = onYoutubePoTokenClientHintChanged,
                    onCookiesPathChanged = onYoutubeCookiesPathChanged,
                    onPickCookies = onPickYoutubeCookies,
                    onPickBundle = onPickYoutubeAuthBundle,
                )
            }

            // ── Save button ───────────────────────────────────────────
            Button(
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text("Save settings", style = MaterialTheme.typography.labelLarge)
            }

            uiState.infoMessage?.let { msg ->
                InfoBanner(msg, isError = false)
            }
            uiState.errorMessage?.let { msg ->
                InfoBanner(msg, isError = true)
            }
            mediaInfoMessage?.let { msg ->
                DismissibleInfoBanner(
                    message = msg,
                    isError = false,
                    onDismiss = onDismissMediaLibraryMessage,
                )
            }
            mediaErrorMessage?.let { msg ->
                DismissibleInfoBanner(
                    message = msg,
                    isError = true,
                    onDismiss = onDismissMediaLibraryMessage,
                )
            }

            // ── Support ───────────────────────────────────────────────
            SettingsIconCard(
                icon = Icons.Outlined.BugReport,
                title = "Support",
                subtitle = "Report issues or visit the website",
            ) {
                SupportLinks(onWebsite = { openUrl("https://video.sandipmaity.me") }) {
                    showReportDialog.value = true
                }
            }

            // ── Follow ────────────────────────────────────────────────
            SettingsIconCard(
                icon = Icons.Outlined.Web,
                title = "Follow the developer",
                subtitle = "Stay updated on social media",
            ) {
                FollowLinks(
                    onGitHub = { openUrl("https://github.com/iam-sandipmaity") },
                    onX = { openUrl("https://x.com/iam_sandipmaity") },
                    onInstagram = { openUrl("https://instagram.com/iam_sandipmaity") },
                    onLinkedIn = { openUrl("https://linkedin.com/in/iam-sandipmaity") },
                )
            }

            // ── Storage ───────────────────────────────────────────────────
            CacheCard(
                cacheSize = cacheSize,
                onClearCache = onClearCache,
            )

            // ── About ─────────────────────────────────────────────────
            AboutCard(onDeveloperProfile = { openUrl("https://profile.sandipmaity.me") })
        }
    }
}

// ── Shared sub-components ──────────────────────────────────────────

@Composable
private fun SettingsHeader(onBack: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Defaults for downloads and media tools",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun SettingsIconCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

// ── YouTube Auth ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YouTubeAuthSection(
    authEnabled: Boolean,
    cookiesPath: String,
    poToken: String,
    poTokenHint: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onAuthEnabledChanged: (Boolean) -> Unit,
    onPoTokenChanged: (String) -> Unit,
    onPoTokenClientHintChanged: (String) -> Unit,
    onCookiesPathChanged: (String) -> Unit,
    onPickCookies: () -> Unit,
    onPickBundle: () -> Unit,
) {
    // Quick toggle
    ToggleRow(
        title = "Enable YouTube auth",
        subtitle = "Access age-gated and private videos",
        checked = authEnabled,
        onCheckedChange = onAuthEnabledChanged,
    )

    // Status indicator
    val hasCookies = cookiesPath.isNotBlank()
    val hasPoToken = poToken.isNotBlank()
    val ready = hasCookies && hasPoToken

    Surface(
        color = if (ready) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (ready) "Ready — cookies and PO token are configured" else "Not configured — import an auth bundle below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
            modifier = Modifier.padding(12.dp),
        )
    }

    // Expandable detail panel
    AuthExpandable(
        expanded = expanded,
        onToggle = { onExpand(!expanded) },
        label = if (expanded) "Hide details" else "Show manual setup",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Bundle import
            Button(
                onClick = onPickBundle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text("Import auth bundle (recommended)")
            }
            Text(
                text = "1. Run the desktop helper on your computer\n2. Move auth_bundle.json to your phone\n3. Tap the button above to import",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            // Manual cookies entry
            Text("Manual entry", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onPickCookies, modifier = Modifier.weight(1f)) {
                    Text("Import cookies")
                }
                TextButton(onClick = { onCookiesPathChanged("") }, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
            }
            if (cookiesPath.isNotBlank()) {
                Text(
                    text = "Cookies: ${cookiesPath.substringAfterLast('/')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FormatDropdown(
                label = "PO token client",
                options = listOf("web.gvs", "mweb.gvs"),
                selected = poTokenHint.ifBlank { "web.gvs" },
                onSelected = onPoTokenClientHintChanged,
            )
            OutlinedTextField(
                value = poToken,
                onValueChange = onPoTokenChanged,
                label = { Text("PO token (paste manually)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun AuthExpandable(
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    label: String,
    content: @Composable () -> Unit,
) {
    TextButton(onClick = { onToggle(!expanded) }, modifier = Modifier.fillMaxWidth()) {
        Text(if (expanded) "Close manual setup" else label)
    }
    if (expanded) {
        content()
    }
}

// ── Support links ──────────────────────────────────────────────────────

@Composable
private fun SupportLinks(onWebsite: () -> Unit, onReportBug: () -> Unit) {
    LinkRow(icon = Icons.Outlined.Language, title = "Official Website", subtitle = "video.sandipmaity.me", onClick = onWebsite)
    HorizontalDivider()
    LinkRow(icon = Icons.Outlined.BugReport, title = "Report a Bug / Crash", subtitle = "Open a GitHub issue", onClick = onReportBug)
}

@Composable
private fun FollowLinks(
    onGitHub: () -> Unit,
    onX: () -> Unit,
    onInstagram: () -> Unit,
    onLinkedIn: () -> Unit,
) {
    LinkRow(icon = Icons.Outlined.Code, title = "GitHub", subtitle = "@iam-sandipmaity", onClick = onGitHub)
    HorizontalDivider()
    LinkRow(icon = Icons.Outlined.Web, title = "X (Twitter)", subtitle = "@iam_sandipmaity", onClick = onX)
    HorizontalDivider()
    LinkRow(icon = Icons.Outlined.Image, title = "Instagram", subtitle = "@iam_sandipmaity", onClick = onInstagram)
    HorizontalDivider()
    LinkRow(icon = Icons.Outlined.Folder, title = "LinkedIn", subtitle = "iam-sandipmaity", onClick = onLinkedIn)
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Cache ───────────────────────────────────────────────────────────────

@Composable
private fun CacheCard(
    cacheSize: Long,
    onClearCache: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Cache",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Temporary files: ${formatFileSize(cacheSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClearCache) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Clear", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

// ── About ──────────────────────────────────────────────────────────────

@Composable
private fun AboutCard(onDeveloperProfile: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Video Downloader",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Powered by yt-dlp + FFmpeg - Runs 100% on device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Made with care by",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Sandip Maity",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onDeveloperProfile() },
            )
        }
    }
}

// ── Small helpers ──────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                singleLine = true,
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportBugDialog(onDismiss: () -> Unit, onOpenIssues: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a Bug or Crash") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("To help fix the issue, please include in your GitHub report:")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ReportStep("1", "What you were doing when it crashed")
                        ReportStep("2", "A screenshot or error message")
                        ReportStep("3", "Your Android version and device model")
                        ReportStep("4", "App version from the About section")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenIssues) { Text("Open GitHub Issues") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ReportStep(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InfoBanner(message: String, isError: Boolean) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun DismissibleInfoBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}
