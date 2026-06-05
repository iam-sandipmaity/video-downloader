package com.localdownloader.ui.screens

import androidx.compose.material.icons.Icons
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
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow

@Composable
fun MoreScreen(
    onOpenQueue: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompress: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePageScaffold(
        title = stringResource(R.string.more_title),
        onBack = null,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.more_queue_title),
                    subtitle = stringResource(R.string.more_queue_subtitle),
                    onClick = onOpenQueue,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.more_history_title),
                    subtitle = stringResource(R.string.more_history_subtitle),
                    onClick = onOpenHistory,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.common_settings),
                    subtitle = stringResource(R.string.more_settings_subtitle),
                    onClick = onOpenSettings,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Web,
                    title = stringResource(R.string.more_cookies_title),
                    subtitle = stringResource(R.string.more_cookies_subtitle),
                    onClick = onOpenCookies,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.more_youtube_access_title),
                    subtitle = stringResource(R.string.more_youtube_access_subtitle),
                    onClick = onOpenYoutubeAccess,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = stringResource(R.string.more_updates_title),
                    subtitle = stringResource(R.string.more_updates_subtitle),
                    onClick = onOpenUpdates,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.HelpOutline,
                    title = stringResource(R.string.common_help),
                    subtitle = stringResource(R.string.more_help_subtitle),
                    onClick = onOpenHelp,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.more_music_player_title),
                    subtitle = stringResource(R.string.more_music_player_subtitle),
                    onClick = onOpenMusic,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.SwapHoriz,
                    title = stringResource(R.string.more_converter_title),
                    subtitle = stringResource(R.string.more_converter_subtitle),
                    onClick = onOpenConvert,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Transform,
                    title = stringResource(R.string.more_compressor_title),
                    subtitle = stringResource(R.string.more_compressor_subtitle),
                    onClick = onOpenCompress,
                )
            }
        }
    }
}
