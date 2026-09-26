package com.localdownloader.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdownloader.BuildConfig
import com.localdownloader.R
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.support.openSupportIssue
import com.localdownloader.ui.support.shareAppLogs

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(HelpCategory.ALL) }
    var expandedTopicId by remember { mutableStateOf<String?>(null) }

    val allTopics = remember { getHelpTopics() }

    val filteredTopics = remember(searchQuery, selectedCategory, allTopics) {
        val query = searchQuery.trim().lowercase()
        allTopics.filter { topic ->
            val matchesCategory = selectedCategory == HelpCategory.ALL || topic.category == selectedCategory
            val matchesSearch = query.isBlank() ||
                topic.title.lowercase().contains(query) ||
                topic.summary.lowercase().contains(query) ||
                topic.body.lowercase().contains(query) ||
                topic.tags.any { it.lowercase().contains(query) }
            matchesCategory && matchesSearch
        }
    }

    PreferencePageScaffold(
        title = "Help & Knowledge Base",
        onBack = onBack,
        modifier = modifier,
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search topics (e.g. subtitles, 429, cookies, vault)...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        }

        // Quick Actions Grid
        item {
            PreferenceSubtitle(text = "QUICK ACTIONS")
        }
        item {
            HelpActionGrid(
                onOpenCookies = onOpenCookies,
                onOpenYoutubeAccess = onOpenYoutubeAccess,
                onExportLogs = { shareAppLogs(context) },
                onReportIssue = { openSupportIssue(context) },
            )
        }

        // Category Filter Chips - Clean Single-Row Horizontal Scroll!
        item {
            PreferenceSubtitle(text = "BROWSE BY CATEGORY")
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(HelpCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                    )
                }
            }
        }

        // Topics Accordion List
        item {
            PreferenceSubtitle(text = "GUIDES & TOPICS (${filteredTopics.size})")
        }

        if (filteredTopics.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "No matching help topics found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Try searching with different keywords like 'download', 'cookies', 'pin', or 'subtitles'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            filteredTopics.forEach { topic ->
                item(key = topic.id) {
                    HelpTopicCard(
                        topic = topic,
                        isExpanded = expandedTopicId == topic.id,
                        onToggle = {
                            expandedTopicId = if (expandedTopicId == topic.id) null else topic.id
                        },
                        onOpenCookies = onOpenCookies,
                        onOpenYoutubeAccess = onOpenYoutubeAccess,
                        onExportLogs = { shareAppLogs(context) },
                    )
                }
            }
        }

        // System Environment Diagnostics
        item {
            PreferenceSubtitle(text = "SYSTEM & RUNTIME DIAGNOSTICS")
        }
        item {
            SystemDiagnosticsCard()
        }
    }
}

@Composable
private fun HelpActionGrid(
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onExportLogs: () -> Unit,
    onReportIssue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Cookie,
                title = "Cookie Hub",
                subtitle = "Manage cookies for Instagram, TikTok & more",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenCookies,
            )
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Security,
                title = "YouTube Access",
                subtitle = "OAuth login, PoToken & bot checks",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = onOpenYoutubeAccess,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.BugReport,
                title = "Export Logs",
                subtitle = "Share app diagnostics & crash history",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onExportLogs,
            )
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.HelpOutline,
                title = "Report Issue",
                subtitle = "Open GitHub tracker with environment specs",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconTint = MaterialTheme.colorScheme.onSurface,
                onClick = onReportIssue,
            )
        }
    }
}

@Composable
private fun HelpActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconBackgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconBackgroundColor,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HelpTopicCard(
    topic: HelpTopic,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onExportLogs: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevronRotation",
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ) {
                    Icon(
                        imageVector = topic.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!isExpanded) {
                        Text(
                            text = topic.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Text(
                        text = topic.body,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (topic.tips.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Pro Tip",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                topic.tips.forEach { tip ->
                                    Text(
                                        text = "• $tip",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // Action Shortcut button if topic has a corresponding feature
                    when (topic.id) {
                        "youtube_access" -> {
                            OutlinedButton(
                                onClick = onOpenYoutubeAccess,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(imageVector = Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open YouTube Access Settings")
                            }
                        }
                        "cookies_manager" -> {
                            OutlinedButton(
                                onClick = onOpenCookies,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(imageVector = Icons.Outlined.Cookie, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Cookie Hub")
                            }
                        }
                        "troubleshooting_429" -> {
                            OutlinedButton(
                                onClick = onExportLogs,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(imageVector = Icons.Outlined.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Logs for Support")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemDiagnosticsCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Device & Engine Specs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DiagnosticsRow(label = "Android OS", value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DiagnosticsRow(label = "Device Hardware", value = "${Build.MANUFACTURER} ${Build.MODEL}")
                DiagnosticsRow(label = "Primary ABI", value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
                DiagnosticsRow(label = "App Channel", value = BuildConfig.APP_RELEASE_CHANNEL)
                DiagnosticsRow(label = "Extraction Engine", value = "yt-dlp Native Bridge")
                DiagnosticsRow(label = "Muxer & Postprocessor", value = "FFmpeg Mobile Integration")
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Medium,
        )
    }
}

enum class HelpCategory(val displayName: String) {
    ALL("All Topics"),
    STARTING("Quick Start"),
    DOWNLOADS("Downloads & Formats"),
    SUBTITLES("Subtitles & Dubs"),
    VAULT("Private Vault"),
    BATTERY("Battery & Background"),
    ACCESS("YouTube & Cookies"),
    TOOLS("Media Tools"),
    STORAGE("Storage & SD Card"),
    TROUBLESHOOTING("Troubleshooting"),
}

data class HelpTopic(
    val id: String,
    val category: HelpCategory,
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val body: String,
    val tips: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

private fun getHelpTopics(): List<HelpTopic> = listOf(
    HelpTopic(
        id = "quick_start",
        category = HelpCategory.STARTING,
        icon = Icons.Outlined.RocketLaunch,
        title = "How to Download Videos & Audio",
        summary = "Paste links from YouTube, Instagram, TikTok, Reddit, X (Twitter), Facebook, and 1000+ sites.",
        body = "1. Copy the video or playlist link from any app or browser.\n2. Open Local Downloader and paste the URL into the search box on the Home tab.\n3. Tap 'Analyze'. You will see available resolutions (4K, 1080p, 720p, etc.) and audio streams.\n4. Select your preferred format, subtitle languages, or audio tracks, and tap 'Start Download'.\n\nYou can also share links directly to Local Downloader using Android's system Share Sheet without manually copying links!",
        tips = listOf(
            "Use Quick Download presets in Settings for one-tap default quality downloading.",
            "You can analyze full playlists and download specific tracks or the entire collection.",
        ),
        tags = listOf("download", "quick", "analyze", "paste", "share", "start", "playlist"),
    ),
    HelpTopic(
        id = "playlists_and_batches",
        category = HelpCategory.STARTING,
        icon = Icons.Outlined.PlaylistPlay,
        title = "Downloading Full Playlists & Channels",
        summary = "Select specific items or download whole YouTube & SoundCloud playlists in batches.",
        body = "When analyzing a playlist link:\n1. The app displays all videos in the playlist with checkboxes.\n2. Tap 'Select All' or handpick individual songs/episodes.\n3. Configure your target quality or choose 'Audio (MP3)' for music playlists.\n4. The batch download will queue each item sequentially with multi-connection acceleration.",
        tips = listOf(
            "Adjust 'Max concurrent downloads' in Settings -> Battery & Performance to run 2 to 4 items simultaneously.",
        ),
        tags = listOf("playlist", "channel", "batch", "multiple", "all", "queue"),
    ),
    HelpTopic(
        id = "video_and_audio_formats",
        category = HelpCategory.DOWNLOADS,
        icon = Icons.Outlined.HighQuality,
        title = "Selecting Resolutions (4K, 1080p, 60fps, HDR) & Audio",
        summary = "Understand video containers (MP4, MKV, WebM) and audio codecs (MP3, M4A, FLAC, Opus).",
        body = "• Best Quality (Auto): Automatically combines the highest available video stream with the highest bitrate audio track using FFmpeg.\n• 60fps & HDR: Supported formats are indicated with badges in the format sheet.\n• MP4 vs MKV: MP4 offers maximum compatibility with gallery apps and TV players. MKV allows embedding multiple subtitle languages and dual audio dubs into one file.\n• Audio Only: Choose MP3, M4A (AAC), or Opus for podcast and music downloads with embedded album artwork.",
        tips = listOf(
            "Turn on 'Show FPS, Codec & Bitrate' in Settings -> Downloads for detailed stream specs.",
            "If your player stutters with AV1 video, select an H.264 or H.265 (HEVC) stream.",
        ),
        tags = listOf("format", "resolution", "4k", "1080p", "60fps", "hdr", "mp4", "mkv", "mp3", "opus"),
    ),
    HelpTopic(
        id = "custom_filenames",
        category = HelpCategory.DOWNLOADS,
        icon = Icons.Outlined.Transform,
        title = "Custom Output File Naming Templates",
        summary = "Customize downloaded filenames with title, uploader, upload date, resolution, and ID.",
        body = "Go to Settings -> Downloads -> Filename template.\n\nYou can use dynamic placeholders:\n• `%(title)s` : Video title\n• `%(uploader)s` : Channel or creator name\n• `%(upload_date)s` : Date published (YYYYMMDD)\n• `%(resolution)s` : Quality tag (e.g. 1080p)\n• `%(id)s` : Unique video identifier\n• `%(ext)s` : File extension (always keep this at the end!)\n\nExample: `%(uploader)s - %(title)s [%(id)s].%(ext)s`",
        tips = listOf(
            "Tap 'Quick presets' inside the filename dialog for recommended patterns.",
        ),
        tags = listOf("filename", "template", "naming", "tags", "uploader", "custom"),
    ),
    HelpTopic(
        id = "subtitles_and_audio",
        category = HelpCategory.SUBTITLES,
        icon = Icons.Outlined.ClosedCaption,
        title = "Subtitles, Multi-Dub Audio & Captions",
        summary = "Native subtitles, embedded captions, custom styling, and secondary language audio tracks.",
        body = "Local Downloader automatically discovers creator-uploaded native subtitles as well as auto-generated captions.\n\n• Automatic Filtering: Automatically excludes thousands of machine auto-translations on videos with huge caption lists to keep the picker fast and clean.\n• Subtitle Embedding: Turn on 'Auto Embed Subtitles' in Settings -> Subtitles to embed subtitles directly into MP4/MKV video containers.\n• Player Styling: Customize subtitle font size, color, background transparency, and screen position right inside the built-in video player.",
        tips = listOf(
            "If you download MKV, you can embed multiple subtitle tracks and switch between them during playback.",
            "Dual audio tracks allow you to download native and dubbed voices simultaneously.",
        ),
        tags = listOf("subtitle", "caption", "translate", "audio", "dub", "styling", "mkv", "embed"),
    ),
    HelpTopic(
        id = "private_vault",
        category = HelpCategory.VAULT,
        icon = Icons.Outlined.Lock,
        title = "Private Vault & PIN Protection",
        summary = "Encrypt and hide sensitive downloads from the public gallery and files apps.",
        body = "The Private Vault stores your media in isolated app-private storage, preventing photos and file managers from indexing them.\n\n• PIN Security: Secure your vault with a 4–8 digit PIN and enable biometric fingerprint unlock.\n• Multiple Vaults: Create separate vaults for different projects or categories.\n• Auto-Move Rules: Add URL domain rules (e.g. specific websites) so matching downloads automatically route directly into your private vault without touching public storage.\n• File Player: Play encrypted vault files directly within the app without ever exporting them to public gallery.",
        tips = listOf(
            "Files inside the vault are completely hidden from gallery apps and file browsers.",
            "You can move files between public Downloads and Private Vault at any time with one tap.",
        ),
        tags = listOf("vault", "pin", "lock", "security", "private", "biometric", "hide", "gallery"),
    ),
    HelpTopic(
        id = "youtube_access",
        category = HelpCategory.ACCESS,
        icon = Icons.Outlined.Security,
        title = "Fixing YouTube 'Sign in' & Bot Checks",
        summary = "Solve 'Sign in to confirm you’re not a bot', PoToken generator, and YouTube OAuth.",
        body = "YouTube frequently triggers automated verification or bot-detection barriers on mobile networks.\n\n• PoToken Generator: Open Settings -> YouTube Access. Local Downloader features an integrated Proof-of-Origin token generator that authenticates requests seamlessly.\n• YouTube OAuth: Sign in with your Google account via OAuth Device Code to download age-restricted, members-only, or private playlist videos.\n• Custom User-Agents: Enable randomized or modern browser user-agents in Settings to bypass anti-scraping blocks.",
        tips = listOf(
            "If YouTube downloads stall with 403 Forbidden, regenerate your PoToken or refresh cookies.",
            "OAuth tokens are stored securely in local encrypted preferences and never leave your device.",
        ),
        tags = listOf("youtube", "bot", "potoken", "oauth", "login", "403", "age-restricted", "access"),
    ),
    HelpTopic(
        id = "cookies_manager",
        category = HelpCategory.ACCESS,
        icon = Icons.Outlined.Cookie,
        title = "Cookie Hub for Instagram, TikTok & Facebook",
        summary = "Download private stories, reels, and member-only posts using Netscape cookie files.",
        body = "Many platforms (Instagram, TikTok, X, Facebook, Reddit) require active session cookies to view HD content or private user stories.\n\n• Importing Cookies: Export cookies from Chrome/Firefox using a standard browser extension (e.g. 'Get cookies.txt LOCALLY') and import the `.txt` file in Settings -> Cookie Hub.\n• Site Profiles: Assign separate cookie profiles per domain.\n• Security: Cookies are strictly stored in local sandboxed storage and used exclusively during media extraction.",
        tips = listOf(
            "Refresh your exported cookie file whenever your social media web session expires.",
            "Ensure the cookie file format is standard Netscape HTTP Cookie format.",
        ),
        tags = listOf("cookies", "instagram", "tiktok", "facebook", "reels", "stories", "login", "netscape"),
    ),
    HelpTopic(
        id = "battery_and_background",
        category = HelpCategory.BATTERY,
        icon = Icons.Outlined.BatteryChargingFull,
        title = "Background Downloads & Battery Saver",
        summary = "Keep downloads running smoothly with screen locked or configure power constraints.",
        body = "Local Downloader utilizes robust Android Foreground Services and WorkManager to keep large downloads active when you switch apps or turn off your screen.\n\n• Battery Optimization: On some OEMs (Xiaomi/MIUI, Samsung OneUI, Huawei), exclude Local Downloader from aggressive battery killers in device Settings -> Battery -> Unrestricted.\n• Charging Constraint: Enable 'Download only while charging' in Battery settings if downloading huge batches.\n• Low Battery Pause: Automatically pause downloads when battery dips below 15%.",
        tips = listOf(
            "Turn on 'Allow metered downloads' if you want to download over mobile data cellular plans.",
            "Set concurrent fragments to 4 or 8 in Settings for faster multi-connection download speeds.",
        ),
        tags = listOf("battery", "background", "pause", "charging", "foreground", "service", "power", "wifi"),
    ),
    HelpTopic(
        id = "storage_and_sdcard",
        category = HelpCategory.STORAGE,
        icon = Icons.Outlined.SdCard,
        title = "Custom Storage Folders & SD Card Support",
        summary = "Choose where downloads are saved or store files directly on an external SD card.",
        body = "By default, files are saved in `Download/LocalDownloader` with dedicated subfolders for Videos, Audio, and Files.\n\n• Changing Root Folder: Go to Settings -> Storage -> Root folder to select any device directory or SD card folder using Android Storage Access Framework (SAF).\n• Automatic Subfolder Sorting: Keep your library tidy by automatically routing MP3s to Audio and MP4s to Videos.",
        tips = listOf(
            "If saving to an external SD card, grant write permissions when prompted by the system folder picker.",
        ),
        tags = listOf("storage", "sdcard", "folder", "path", "directory", "external", "library"),
    ),
    HelpTopic(
        id = "media_tools",
        category = HelpCategory.TOOLS,
        icon = Icons.Outlined.MusicNote,
        title = "Built-in Player, Music Mode & Converter",
        summary = "Background music player, video compressor, and audio trimmer tools.",
        body = "Local Downloader is a complete media workstation:\n\n• Music Player: Seamless background audio playback with lockscreen notification controls, sleep timer, repeat/shuffle, and favorites playlist.\n• Video Compressor: Reduce huge 4K/1080p video file sizes using efficient H.265/HEVC or H.264 codecs.\n• Format Converter: Extract MP3, M4A, FLAC, or Opus audio from downloaded videos instantly.",
        tips = listOf(
            "Use the Video Player swipe gestures: left side swipes adjust brightness, right side adjusts volume, and horizontal swipe seeks.",
            "Picture-in-Picture (PiP) is supported natively during video playback.",
        ),
        tags = listOf("player", "music", "compressor", "converter", "ffmpeg", "mp3", "video", "gestures"),
    ),
    HelpTopic(
        id = "troubleshooting_429",
        category = HelpCategory.TROUBLESHOOTING,
        icon = Icons.Outlined.WarningAmber,
        title = "HTTP 429 Too Many Requests & DRM Limitations",
        summary = "How to resolve rate limits, connection timeouts, and understand DRM restrictions.",
        body = "• HTTP 429 Rate Limit: When a website temporarily limits your IP for requesting too many videos in a short span, wait 10–15 minutes, switch from Wi-Fi to Mobile data (or vice versa), or enable a VPN.\n• DRM & Protected Streams: Platforms like Netflix, Spotify, or Disney+ use Widevine DRM encryption. Content protected by strict DRM cannot be legally downloaded by open extraction tools.\n• Incomplete Downloads: If a download is interrupted, tap 'Retry' or 'Resume' on the queue item.",
        tips = listOf(
            "Check the App Log & Diagnostics screen to inspect the exact server response headers.",
            "Updating yt-dlp to the latest channel release fixes extractor breakage quickly.",
        ),
        tags = listOf("429", "error", "rate limit", "drm", "timeout", "failed", "broken", "yt-dlp"),
    ),
    HelpTopic(
        id = "updating_engines",
        category = HelpCategory.TROUBLESHOOTING,
        icon = Icons.Outlined.Sync,
        title = "Updating yt-dlp & FFmpeg Engines",
        summary = "Keep extractors up to date to support newly changed website streaming formats.",
        body = "Websites like YouTube and Instagram constantly modify their web players.\n\n• Check Updates: Go to Settings -> Updates & Engines.\n• One-Tap Update: You can update the yt-dlp extraction script independently without needing a whole new app update!\n• Auto-Update: Enable 'Check updates on startup' to always stay on the latest extraction version.",
        tips = listOf(
            "Switching yt-dlp to the 'Nightly' channel gives you bleeding-edge fixes for new site changes.",
        ),
        tags = listOf("update", "yt-dlp", "ffmpeg", "nightly", "engine", "extractor", "broken"),
    ),
)
