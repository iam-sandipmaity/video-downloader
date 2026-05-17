package com.localdownloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HelpHeroCard()

            HelpSectionCard(
                title = "Quick start",
                subtitle = "The shortest path from link to saved file.",
                items = listOf(
                    HelpItem(
                        icon = Icons.Outlined.Web,
                        title = "1. Paste a link",
                        body = "Use Home to paste a supported video or audio URL. Quick links are only shortcuts; any supported site link works.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.CloudDownload,
                        title = "2. Analyze and queue",
                        body = "Tap Analyze link, choose the stream type and quality you want, then queue the download. The queue keeps each item separate.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.Storage,
                        title = "3. Open it later",
                        body = "Finished files appear in Downloads automatically. From there you can play, share, rename, or remove them.",
                    ),
                ),
            )

            HelpSectionCard(
                title = "If a site will not fetch",
                subtitle = "Use this order before assuming the link is broken.",
                items = listOf(
                    HelpItem(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Retry once after a failed analysis",
                        body = "Some sites return unstable metadata on the first pass. A second analysis can succeed after yt-dlp refreshes the page data.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.Web,
                        title = "Add cookies for protected pages",
                        body = "If the site needs sign-in, age verification, or region/session access, open More > Cookies and save a matching session first.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.Info,
                        title = "Check the queue and history logs",
                        body = "If analysis or download still fails, open the queue for active output or history for the full saved log so you can see the exact yt-dlp message.",
                    ),
                ),
            )

            HelpSectionCard(
                title = "Downloads and storage",
                subtitle = "Where files go and what the app manages for you.",
                items = listOf(
                    HelpItem(
                        icon = Icons.Outlined.Storage,
                        title = "Saved folders",
                        body = "Media is grouped inside your Downloads folder using the root, audio, video, and other subfolders configured in Settings.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.Settings,
                        title = "Library cleanup",
                        body = "You can remove only the in-app entry, remove the real file too, clear temporary cache, or reset folder names from Settings.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.GraphicEq,
                        title = "Downloaded music",
                        body = "When audio files exist, Downloads shows a simple music launcher that opens the player without cluttering the library list.",
                    ),
                ),
            )

            HelpSectionCard(
                title = "Built-in tools",
                subtitle = "Extra things the app can do after a file is saved.",
                items = listOf(
                    HelpItem(
                        icon = Icons.Outlined.SwapHoriz,
                        title = "Converter",
                        body = "Turn a local media file into another format using FFmpeg. This is useful when you need a different container or audio-only export.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.Transform,
                        title = "Compressor",
                        body = "Reduce file size by lowering resolution or bitrate before sharing. Smaller targets save space but can reduce quality.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.CloudDownload,
                        title = "Queue and notifications",
                        body = "Each active item keeps its own progress notification, and completed, failed, and canceled items are separated into their own channels.",
                    ),
                ),
            )

            HelpSectionCard(
                title = "Good to know",
                subtitle = "A few practical details that help avoid confusion.",
                items = listOf(
                    HelpItem(
                        icon = Icons.Outlined.Settings,
                        title = "Settings are meant to stay out of the way",
                        body = "Theme, contrast, accent, folders, notifications, and download defaults are all grouped in Settings so the main tabs stay focused.",
                    ),
                    HelpItem(
                        icon = Icons.Outlined.Info,
                        title = "yt-dlp does the heavy lifting",
                        body = "Website support depends on the embedded yt-dlp runtime. If one site changes behavior, analysis and download reliability can change until the runtime logic is updated.",
                    ),
                ),
            )
        }
    }
}

@Composable
private fun HelpHeroCard() {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Download smarter, not harder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Use Home for links, Downloads for finished files, More for queue and tools, and Settings for folders, notifications, and app behavior.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HelpBadge("yt-dlp")
                    HelpBadge("Cookies")
                    HelpBadge("Queue")
                }
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    title: String,
    subtitle: String,
    items: List<HelpItem>,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items.forEachIndexed { index, item ->
                HelpRow(item = item)
                if (index != items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                }
            }
        }
    }
}

@Composable
private fun HelpRow(item: HelpItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(12.dp)
                    .size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelpBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class HelpItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
)
