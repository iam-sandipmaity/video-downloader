package com.localdownloader.ui.screens.settings

import android.content.Intent
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.BuildConfig
import com.localdownloader.R
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle

@Composable
fun AboutSettingsScreen(
    onOpenUpdates: () -> Unit,
    onResetSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resetDialogTitle = stringResource(R.string.storage_reset_dialog_title)
    val resetDialogBody = stringResource(R.string.storage_reset_dialog_body)
    val resetNowLabel = stringResource(R.string.common_reset_now)
    val svgImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val openSourceCredits = listOf(
        OpenSourceCredit(
            title = "Android Jetpack Compose",
            subtitle = "v2026.05.00 | Declarative UI foundation for the app.",
            url = "https://developer.android.com/jetpack/compose",
        ),
        OpenSourceCredit(
            title = "Kotlin",
            subtitle = "v2.3.21 | Main application language and Compose tooling.",
            url = "https://kotlinlang.org/",
        ),
        OpenSourceCredit(
            title = "Kotlin Coroutines",
            subtitle = "v1.11.0 | Async work, flows, and background coordination.",
            url = "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        OpenSourceCredit(
            title = "Kotlin Serialization",
            subtitle = "v1.11.0 | JSON parsing for yt-dlp metadata and app state.",
            url = "https://github.com/Kotlin/kotlinx.serialization",
        ),
        OpenSourceCredit(
            title = "Media3 ExoPlayer",
            subtitle = "v1.10.1 | Local video and audio playback inside the app.",
            url = "https://developer.android.com/media/media3",
        ),
        OpenSourceCredit(
            title = "Coil",
            subtitle = "v2.7.0 | Image, GIF, and SVG loading across the UI.",
            url = "https://coil-kt.github.io/coil/",
        ),
        OpenSourceCredit(
            title = "yt-dlp Android Runtime",
            subtitle = "v0.18.1 | Embedded downloader runtime via youtubedl-android.",
            url = "https://github.com/yausername/youtubedl-android",
        ),
        OpenSourceCredit(
            title = "yt-dlp",
            subtitle = "Managed runtime | Core extractor/downloader engine used by the app.",
            url = "https://github.com/yt-dlp/yt-dlp",
        ),
        OpenSourceCredit(
            title = "FFmpeg",
            subtitle = "Managed runtime | Media merge, remux, convert, and compression support.",
            url = "https://ffmpeg.org/",
        ),
        OpenSourceCredit(
            title = "Hilt",
            subtitle = "v2.59.2 / AndroidX Hilt 1.3.0 | Dependency injection and worker wiring.",
            url = "https://dagger.dev/hilt/",
        ),
        OpenSourceCredit(
            title = "WorkManager",
            subtitle = "v2.11.2 | Download queue scheduling and background maintenance.",
            url = "https://developer.android.com/topic/libraries/architecture/workmanager",
        ),
        OpenSourceCredit(
            title = "Room",
            subtitle = "v2.8.4 | Persistent queue, history, and local task storage.",
            url = "https://developer.android.com/jetpack/androidx/releases/room",
        ),
        OpenSourceCredit(
            title = "Material 3",
            subtitle = "Material Components 1.14.0 | App theming, components, and motion language.",
            url = "https://m3.material.io/",
        ),
    )
    var confirmDialog by remember { mutableStateOf<SettingConfirmDialogState?>(null) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    confirmDialog?.let { state ->
        SettingConfirmDialog(
            state = state,
            onDismiss = { confirmDialog = null },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.settings_about_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "APPLICATION INFO")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.about_package_title),
                value = BuildConfig.APPLICATION_ID,
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.about_version_title),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.SystemUpdate,
                title = stringResource(R.string.about_updates_title),
                description = "App, yt-dlp, and FFmpeg updates",
                onClick = onOpenUpdates,
            )
        }
        item {
            PreferenceSubtitle(text = "COMMUNITY & LINKS")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.about_website_title),
                description = "video.sandipmaity.me",
                onClick = { openUrl("https://video.sandipmaity.me") },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.about_source_title),
                description = "github.com/iam-sandipmaity/video-downloader",
                onClick = { openUrl("https://github.com/iam-sandipmaity/video-downloader") },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = "yt-dlp",
                description = "Downloader engine",
                onClick = { openUrl("https://github.com/yt-dlp/yt-dlp") },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = "FFmpeg",
                description = "Media processing runtime",
                onClick = { openUrl("https://github.com/FFmpeg/FFmpeg") },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.about_developer_github_title),
                description = "@iam-sandipmaity",
                onClick = { openUrl("https://github.com/iam-sandipmaity") },
            )
        }
        item {
            AboutAssetRow(
                assetPath = "file:///android_asset/platform_logos/x.svg",
                imageLoader = svgImageLoader,
                title = stringResource(R.string.about_developer_x_title),
                subtitle = "@iam_sandipmaity",
                onClick = { openUrl("https://x.com/iam_sandipmaity") },
            )
        }
        item {
            PreferenceSubtitle(text = "OPEN SOURCE CREDITS")
        }
        items(openSourceCredits.size) { index ->
            val credit = openSourceCredits[index]
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = credit.title,
                description = credit.subtitle,
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            PreferenceSubtitle(text = "RESET")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(R.string.about_reset_title),
                description = "Restore all preferences to default",
                onClick = {
                    confirmDialog = SettingConfirmDialogState(
                        title = resetDialogTitle,
                        body = resetDialogBody,
                        confirmLabel = resetNowLabel,
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

private data class OpenSourceCredit(
    val title: String,
    val subtitle: String,
    val url: String,
)

@Composable
private fun AboutAssetRow(
    assetPath: String,
    imageLoader: ImageLoader,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = assetPath,
                imageLoader = imageLoader,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .padding(start = 4.dp, end = 18.dp)
                    .size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
