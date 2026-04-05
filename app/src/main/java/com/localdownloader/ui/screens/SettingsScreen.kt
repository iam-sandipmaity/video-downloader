package com.localdownloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun SettingsScreen(
    uiState: FormatUiState,
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    onOutputTemplateChanged: (String) -> Unit,
    onContainerChanged: (String) -> Unit,
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onYoutubeAuthEnabledChanged: (Boolean) -> Unit,
    onYoutubePoTokenChanged: (String) -> Unit,
    onYoutubePoTokenClientHintChanged: (String) -> Unit,
    onYoutubeCookiesPathChanged: (String) -> Unit,
    onPickYoutubeCookies: () -> Unit,
    onPickYoutubeAuthBundle: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containers = listOf("mp4", "webm", "mkv", "mov")
    val poTokenHints = listOf("web.gvs", "mweb.gvs")
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    val hasCookies = uiState.youtubeCookiesPath.isNotBlank()
    val hasPoToken = uiState.youtubePoToken.isNotBlank()
    val authConfigured = hasCookies && hasPoToken

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report a Bug or Crash") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "To help fix the issue, please raise a GitHub issue with the following information:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ReportStep("1", "Open the GitHub Issues page (button below)")
                            ReportStep("2", "Click \u201cNew issue\u201d")
                            ReportStep("3", "Describe what you were doing when it crashed")
                            ReportStep("4", "Attach a screenshot of the error or debug trace")
                            ReportStep("5", "Include your Android version and device model")
                            ReportStep("6", "Submit — we'll respond as soon as possible")
                        }
                    }
                    Text(
                        text = "github.com/iam-sandipmaity/video-downloader/issues",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            openUrl("https://github.com/iam-sandipmaity/video-downloader/issues")
                        },
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    openUrl("https://github.com/iam-sandipmaity/video-downloader/issues/new")
                    showReportDialog = false
                }) {
                    Text("Open GitHub Issues")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Close") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Defaults applied to every new download",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Appearance section ────────────────────────────────────
            SettingsSectionLabel("Appearance")
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingsToggleRow(
                    title = "Dark theme",
                    subtitle = "Switch between light and dark appearance",
                    checked = uiState.isDarkTheme,
                    onCheckedChange = { onDarkThemeChanged?.invoke(it) },
                )
            }

            // ── Output section ────────────────────────────────────────
            SettingsSectionLabel("Output")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.outputTemplate,
                        onValueChange = onOutputTemplateChanged,
                        label = { Text("Filename template") },
                        supportingText = { Text("yt-dlp output template, e.g. %(title)s.%(ext)s") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    SettingsDropdownRow(
                        label = "Default container",
                        subtitle = "Video merge format",
                        options = containers,
                        selectedIndex = containers.indexOf(uiState.selectedContainer).coerceAtLeast(0),
                        onSelected = { onContainerChanged(containers[it]) },
                    )
                }
            }

            // ── Metadata section ──────────────────────────────────────
            SettingsSectionLabel("Metadata")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    SettingsToggleRow(
                        title = "Embed metadata",
                        subtitle = "Write title, artist, album tags into the file",
                        checked = uiState.embedMetadata,
                        onCheckedChange = onEmbedMetadataChanged,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    SettingsToggleRow(
                        title = "Embed thumbnail",
                        subtitle = "Attach cover art to the downloaded file",
                        checked = uiState.embedThumbnail,
                        onCheckedChange = onEmbedThumbnailChanged,
                    )
                }
            }

            SettingsSectionLabel("YouTube Auth")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Recommended setup",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "1. Run the desktop helper on your computer\n2. Move auth_bundle.json to your phone\n3. Import it here and the app fills everything automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = onPickYoutubeAuthBundle,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Import auth bundle")
                            }
                            Text(
                                text = "Manual cookies + PO token entry is still available below if you need it.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (authConfigured) "Status: ready for YouTube auth fallback" else "Status: setup incomplete",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = if (authConfigured) {
                                    "Cookies and a PO token are saved. The app can retry blocked YouTube formats with auth."
                                } else {
                                    "Import auth_bundle.json for the easiest setup, or add both a cookies file and a PO token manually."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    SettingsToggleRow(
                        title = "Enable YouTube auth fallback",
                        subtitle = "Retry blocked YouTube videos with imported auth data when normal download attempts get blocked",
                        checked = uiState.youtubeAuthEnabled,
                        onCheckedChange = onYoutubeAuthEnabledChanged,
                    )
                    Text(
                        text = "Manual setup",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = uiState.youtubeCookiesPath,
                        onValueChange = onYoutubeCookiesPathChanged,
                        label = { Text("Cookies file path") },
                        supportingText = { Text("Imported Netscape cookies file stored inside the app") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onPickYoutubeCookies,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Import cookies")
                        }
                        TextButton(
                            onClick = { onYoutubeCookiesPathChanged("") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Clear cookies")
                        }
                    }
                    SettingsDropdownRow(
                        label = "PO token client",
                        subtitle = "Use the same token type the helper or browser session produced",
                        options = poTokenHints,
                        selectedIndex = poTokenHints.indexOf(uiState.youtubePoTokenClientHint).coerceAtLeast(0),
                        onSelected = { onYoutubePoTokenClientHintChanged(poTokenHints[it]) },
                    )
                    OutlinedTextField(
                        value = uiState.youtubePoToken,
                        onValueChange = onYoutubePoTokenChanged,
                        label = { Text("PO token") },
                        supportingText = { Text("Paste the PO token only if you are not importing auth_bundle.json") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(
                        text = if (authConfigured) {
                            "Manual auth fields are complete."
                        } else {
                            "Manual auth fields are incomplete."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            onYoutubeAuthEnabledChanged(false)
                            onYoutubeCookiesPathChanged("")
                            onYoutubePoTokenChanged("")
                            onYoutubePoTokenClientHintChanged("web.gvs")
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Clear all auth data")
                    }
                }
            }

            // ── Save button ───────────────────────────────────────────
            Button(
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text("Save settings", style = MaterialTheme.typography.labelLarge)
            }

            // Success / error feedback
            uiState.infoMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
            uiState.errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            // ── Support section ───────────────────────────────────────
            SettingsSectionLabel("Support")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    // Website
                    SettingsLinkRow(
                        icon = {
                            SvgIcon(
                                // Globe / website icon
                                svgPath = "M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2zm0 2c1.07 0 2.37.84 3.43 2.9A13.7 13.7 0 0 1 16.94 11H7.06a13.7 13.7 0 0 1 1.51-4.1C9.63 4.84 10.93 4 12 4zm-5.85 1.32A11.9 11.9 0 0 0 5.07 11H4.1A8.02 8.02 0 0 1 8 5.07c-.66.36-1.25.8-1.85 1.25zm11.7 0c-.6-.45-1.19-.89-1.85-1.25A8.02 8.02 0 0 1 19.9 11h-.97a11.9 11.9 0 0 0-1.08-4.68zM4.1 13h1a11.9 11.9 0 0 0 1.08 4.68A8.02 8.02 0 0 1 4.1 13zm3.35 5.93C8.76 20.4 10.27 20 12 20s3.24.4 4.55.93c.95-.54 1.78-1.25 2.47-2.07A13.7 13.7 0 0 1 7.06 13h9.88a13.7 13.7 0 0 1-1.94 5.86c-.69.82-1.52 1.53-2.47 2.07z",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = "Official Website",
                        subtitle = "video.sandipmaity.me",
                        onClick = { openUrl("https://video.sandipmaity.me") },
                    )
                    HorizontalDivider()
                    // Report issue
                    SettingsLinkRow(
                        icon = {
                            SvgIcon(
                                // Bug / issue icon
                                svgPath = "M20 8h-2.81a5.985 5.985 0 0 0-1.82-1.96L17 4.41 15.59 3l-2.17 2.17a6 6 0 0 0-2.84 0L8.41 3 7 4.41l1.62 1.63A5.985 5.985 0 0 0 6.81 8H4v2h2.09A6.01 6.01 0 0 0 6 11v1H4v2h2v1c0 .34.03.67.07 1H4v2h2.29A6 6 0 0 0 18 17v-1h2v-2h-2v-1c0-.34-.03-.67-.07-1H20V8zm-6 8h-4v-2h4v2zm0-4h-4v-2h4v2z",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        title = "Report a Bug / Crash",
                        subtitle = "Open a GitHub issue with details",
                        onClick = { showReportDialog = true },
                    )
                }
            }

            // ── Follow section ────────────────────────────────────────
            SettingsSectionLabel("Follow the Developer")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    // GitHub
                    SettingsLinkRow(
                        icon = {
                            SvgIcon(
                                svgPath = "M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0 1 12 6.844a9.59 9.59 0 0 1 2.504.337c1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.02 10.02 0 0 0 22 12.017C22 6.484 17.522 2 12 2z",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        title = "GitHub",
                        subtitle = "@iam-sandipmaity",
                        onClick = { openUrl("https://github.com/iam-sandipmaity") },
                    )
                    HorizontalDivider()
                    // X / Twitter
                    SettingsLinkRow(
                        icon = {
                            SvgIcon(
                                svgPath = "M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-4.714-6.231-5.401 6.231H2.744l7.737-8.856L1.64 2.25H8.28l4.253 5.622 5.71-5.622zm-1.161 17.52h1.833L7.084 4.126H5.117z",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        title = "X (Twitter)",
                        subtitle = "@iam_sandipmaity",
                        onClick = { openUrl("https://x.com/iam_sandipmaity") },
                    )
                    HorizontalDivider()
                    // Instagram
                    SettingsLinkRow(
                        icon = {
                            SvgIcon(
                                svgPath = "M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838a6.162 6.162 0 1 0 0 12.324 6.162 6.162 0 0 0 0-12.324zM12 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm6.406-11.845a1.44 1.44 0 1 0 0 2.881 1.44 1.44 0 0 0 0-2.881z",
                                tint = Color(0xFFE1306C),
                            )
                        },
                        title = "Instagram",
                        subtitle = "@iam_sandipmaity",
                        onClick = { openUrl("https://instagram.com/iam_sandipmaity") },
                    )
                    HorizontalDivider()
                    // LinkedIn
                    SettingsLinkRow(
                        icon = {
                            SvgIcon(
                                svgPath = "M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 0 1-2.063-2.065 2.064 2.064 0 1 1 2.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z",
                                tint = Color(0xFF0A66C2),
                            )
                        },
                        title = "LinkedIn",
                        subtitle = "iam-sandipmaity",
                        onClick = { openUrl("https://linkedin.com/in/iam-sandipmaity") },
                    )
                }
            }

            // ── About section ─────────────────────────────────────────
            SettingsSectionLabel("About")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Video Downloader",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Powered by yt-dlp + FFmpeg · Runs 100% on device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("Made with ❤️ by ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                                append("Sandip Maity")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable { openUrl("https://profile.sandipmaity.me") },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Private helpers ────────────────────────────────────────────────────────

@Composable
private fun ReportStep(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(50),
        ) {
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
private fun SettingsLinkRow(
    icon: @Composable () -> Unit,
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
        icon()
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

@Composable
private fun SvgIcon(
    svgPath: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val path = remember(svgPath) { PathParser().parsePathString(svgPath).toPath() }
    Canvas(modifier = modifier.size(22.dp)) {
        scale(
            scaleX = size.width / 24f,
            scaleY = size.height / 24f,
            pivot = Offset.Zero,
        ) {
            drawPath(path = path, color = tint)
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
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
private fun SettingsDropdownRow(
    label: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = options.getOrElse(selectedIndex) { options.first() },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
