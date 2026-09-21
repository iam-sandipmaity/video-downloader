package com.localdownloader.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceNavigationRow
import com.localdownloader.ui.components.PreferencePageScaffold

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
            PreferenceGroup {
                PreferenceNavigationRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.more_queue_title),
                    subtitle = "Active, paused, and queued tasks",
                    onClick = onOpenQueue,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.more_history_title),
                    subtitle = "Completed download history",
                    onClick = onOpenHistory,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.more_vault_title),
                    subtitle = "PIN-protected local storage",
                    onClick = onOpenVault,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.common_settings),
                    subtitle = "Preferences and configuration",
                    onClick = onOpenSettings,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Web,
                    title = stringResource(R.string.more_cookies_title),
                    subtitle = "Saved website sessions",
                    onClick = onOpenCookies,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.more_youtube_access_title),
                    subtitle = "YouTube login and session hints",
                    onClick = onOpenYoutubeAccess,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = stringResource(R.string.more_updates_title),
                    subtitle = "App, yt-dlp, and FFmpeg updates",
                    onClick = onOpenUpdates,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.HelpOutline,
                    title = stringResource(R.string.common_help),
                    subtitle = "Guides and troubleshooting",
                    onClick = onOpenHelp,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceNavigationRow(
                    icon = Icons.Outlined.PlayCircle,
                    title = stringResource(R.string.more_video_player_title),
                    subtitle = "Built-in video player",
                    onClick = onOpenVideo,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.more_music_player_title),
                    subtitle = "Built-in audio player",
                    onClick = onOpenMusic,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.SwapHoriz,
                    title = stringResource(R.string.more_converter_title),
                    subtitle = "Convert media formats",
                    onClick = onOpenConvert,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Transform,
                    title = stringResource(R.string.more_compressor_title),
                    subtitle = "Compress video files",
                    onClick = onOpenCompress,
                )
            }
        }
    }
}
