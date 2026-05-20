package com.localdownloader.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.BuildConfig
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow

@Composable
fun AboutSettingsScreen(
    onOpenUpdates: () -> Unit,
    onResetSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val svgImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    var confirmDialog by remember { mutableStateOf<SettingConfirmDialogState?>(null) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    confirmDialog?.let { state ->
        SettingConfirmDialog(
            state = state,
            onDismiss = { confirmDialog = null },
        )
    }

    PreferencePageScaffold(
        title = "About and support",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Info,
                    title = "Package name",
                    subtitle = "Installed application identifier.",
                    value = BuildConfig.APPLICATION_ID,
                    onClick = null,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    subtitle = "Current installed app version.",
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = null,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Updates center",
                    subtitle = "Manage app, yt-dlp, and FFmpeg update flows from one place.",
                    onClick = onOpenUpdates,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Language,
                    title = "Official website",
                    subtitle = "video.sandipmaity.me",
                    onClick = { openUrl("https://video.sandipmaity.me") },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Code,
                    title = "App source code",
                    subtitle = "github.com/iam-sandipmaity/video-downloader",
                    onClick = { openUrl("https://github.com/iam-sandipmaity/video-downloader") },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Description,
                    title = "yt-dlp",
                    subtitle = "Open the upstream downloader engine project.",
                    onClick = { openUrl("https://github.com/yt-dlp/yt-dlp") },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Description,
                    title = "FFmpeg",
                    subtitle = "Open the upstream media processing project.",
                    onClick = { openUrl("https://github.com/FFmpeg/FFmpeg") },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Language,
                    title = "Developer GitHub",
                    subtitle = "@iam-sandipmaity",
                    onClick = { openUrl("https://github.com/iam-sandipmaity") },
                )
                PreferenceDivider()
                AboutAssetRow(
                    assetPath = "file:///android_asset/platform_logos/x.svg",
                    imageLoader = svgImageLoader,
                    title = "Developer X",
                    subtitle = "@iam_sandipmaity",
                    onClick = { openUrl("https://x.com/iam_sandipmaity") },
                )
                PreferenceDivider()
                AboutAssetRow(
                    assetPath = "file:///android_asset/platform_logos/instagram.svg",
                    imageLoader = svgImageLoader,
                    title = "Developer Instagram",
                    subtitle = "@iam_sandipmaity",
                    onClick = { openUrl("https://instagram.com/iam_sandipmaity") },
                )
                PreferenceDivider()
                AboutAssetRow(
                    assetPath = "file:///android_asset/platform_logos/linkedin.svg",
                    imageLoader = svgImageLoader,
                    title = "Developer LinkedIn",
                    subtitle = "sandip-maity",
                    onClick = { openUrl("https://www.linkedin.com/in/sandip-maity") },
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.RestartAlt,
                    title = "Reset all settings",
                    subtitle = "Restore appearance, folders, download defaults, access preferences, and library behavior back to the default setup.",
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = "Reset settings",
                            body = "This restores appearance, folders, download defaults, and library behavior back to the default setup.",
                            confirmLabel = "Reset now",
                            onConfirm = {
                                onResetSettings()
                                confirmDialog = null
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutAssetRow(
    assetPath: String,
    imageLoader: ImageLoader,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f),
        ) {
            AsyncImage(
                model = assetPath,
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier
                    .padding(11.dp)
                    .size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Code,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
