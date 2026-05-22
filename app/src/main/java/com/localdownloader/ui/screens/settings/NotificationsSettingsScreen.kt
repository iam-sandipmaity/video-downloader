package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSwitchRow
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun NotificationsSettingsScreen(
    uiState: FormatUiState,
    onNotifyCompletedDownloadsChanged: (Boolean) -> Unit,
    onNotifyDownloadErrorsChanged: (Boolean) -> Unit,
    onNotifyCanceledDownloadsChanged: (Boolean) -> Unit,
    onNotifyPromotionsChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePageScaffold(
        title = stringResource(R.string.settings_notifications_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.notifications_completed_title),
                    subtitle = stringResource(R.string.notifications_completed_subtitle),
                    checked = uiState.notifyCompletedDownloads,
                    onCheckedChange = onNotifyCompletedDownloadsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.ErrorOutline,
                    title = stringResource(R.string.notifications_errors_title),
                    subtitle = stringResource(R.string.notifications_errors_subtitle),
                    checked = uiState.notifyDownloadErrors,
                    onCheckedChange = onNotifyDownloadErrorsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.NotificationsOff,
                    title = stringResource(R.string.notifications_canceled_title),
                    subtitle = stringResource(R.string.notifications_canceled_subtitle),
                    checked = uiState.notifyCanceledDownloads,
                    onCheckedChange = onNotifyCanceledDownloadsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Campaign,
                    title = stringResource(R.string.notifications_promotions_title),
                    subtitle = stringResource(R.string.notifications_promotions_subtitle),
                    checked = uiState.notifyPromotions,
                    onCheckedChange = onNotifyPromotionsChanged,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.notifications_active_title),
                    subtitle = stringResource(R.string.notifications_active_subtitle),
                    onClick = null,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.notifications_music_title),
                    subtitle = stringResource(R.string.notifications_music_subtitle),
                    onClick = null,
                )
            }
        }
    }
}
