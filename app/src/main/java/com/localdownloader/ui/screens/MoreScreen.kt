package com.localdownloader.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferenceNavigationRow
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle

@Composable
fun MoreScreen(
    onOpenQueue: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompress: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePageScaffold(
        title = stringResource(R.string.more_title),
        onBack = null,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "DOWNLOADS & STORAGE")
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.CloudDownload,
                title = stringResource(R.string.more_queue_title),
                subtitle = "Active, paused, and queued tasks",
                onClick = onOpenQueue,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.more_history_title),
                subtitle = "Completed download history",
                onClick = onOpenHistory,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.more_vault_title),
                subtitle = "PIN-protected local storage",
                onClick = onOpenVault,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Settings,
                title = stringResource(R.string.common_settings),
                subtitle = "Preferences and configuration",
                onClick = onOpenSettings,
            )
        }
        item {
            PreferenceSubtitle(text = "NETWORK & TOOLS")
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Web,
                title = stringResource(R.string.more_cookies_title),
                subtitle = "Saved website sessions",
                onClick = onOpenCookies,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.more_youtube_access_title),
                subtitle = "YouTube login and session hints",
                onClick = onOpenYoutubeAccess,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.SystemUpdate,
                title = stringResource(R.string.more_updates_title),
                subtitle = "App, yt-dlp, and FFmpeg updates",
                onClick = onOpenUpdates,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.HelpOutline,
                title = stringResource(R.string.common_help),
                subtitle = "Guides and troubleshooting",
                onClick = onOpenHelp,
            )
        }
        item {
            PreferenceSubtitle(text = "MEDIA PLAYBACK & CONVERSION")
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.more_video_player_title),
                subtitle = "Built-in video player",
                onClick = onOpenVideo,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.more_music_player_title),
                subtitle = "Built-in audio player",
                onClick = onOpenMusic,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.SwapHoriz,
                title = stringResource(R.string.more_converter_title),
                subtitle = "Convert media formats",
                onClick = onOpenConvert,
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Transform,
                title = stringResource(R.string.more_compressor_title),
                subtitle = "Compress video files",
                onClick = onOpenCompress,
            )
        }
    }
}

