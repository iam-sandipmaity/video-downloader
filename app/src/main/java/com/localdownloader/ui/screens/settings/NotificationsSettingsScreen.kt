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
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
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
            PreferenceSubtitle(text = "DOWNLOAD ALERTS")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(R.string.notifications_completed_title),
                isChecked = uiState.notifyCompletedDownloads,
                onClick = { onNotifyCompletedDownloadsChanged(!uiState.notifyCompletedDownloads) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.ErrorOutline,
                title = stringResource(R.string.notifications_errors_title),
                isChecked = uiState.notifyDownloadErrors,
                onClick = { onNotifyDownloadErrorsChanged(!uiState.notifyDownloadErrors) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.NotificationsOff,
                title = stringResource(R.string.notifications_canceled_title),
                isChecked = uiState.notifyCanceledDownloads,
                onClick = { onNotifyCanceledDownloadsChanged(!uiState.notifyCanceledDownloads) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Campaign,
                title = stringResource(R.string.notifications_promotions_title),
                description = stringResource(R.string.notifications_promotions_subtitle),
                isChecked = uiState.notifyPromotions,
                onClick = { onNotifyPromotionsChanged(!uiState.notifyPromotions) },
            )
        }
        item {
            PreferenceSubtitle(text = "FOREGROUND SERVICES")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.notifications_active_title),
                description = stringResource(R.string.notifications_active_subtitle),
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.notifications_music_title),
                description = stringResource(R.string.notifications_music_subtitle),
                onClick = null,
            )
        }
    }
}

