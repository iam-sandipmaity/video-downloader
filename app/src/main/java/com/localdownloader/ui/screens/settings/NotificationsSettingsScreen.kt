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
        title = "Notifications",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Completed downloads",
                    subtitle = "Show a notification when a download finishes.",
                    checked = uiState.notifyCompletedDownloads,
                    onCheckedChange = onNotifyCompletedDownloadsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.ErrorOutline,
                    title = "Download errors",
                    subtitle = "Show a notification when a download fails.",
                    checked = uiState.notifyDownloadErrors,
                    onCheckedChange = onNotifyDownloadErrorsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.NotificationsOff,
                    title = "Canceled downloads",
                    subtitle = "Show a notification when you cancel a download.",
                    checked = uiState.notifyCanceledDownloads,
                    onCheckedChange = onNotifyCanceledDownloadsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Campaign,
                    title = "Promotions and updates",
                    subtitle = "Allow optional product announcements and update alerts.",
                    checked = uiState.notifyPromotions,
                    onCheckedChange = onNotifyPromotionsChanged,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Active downloads",
                    subtitle = "Shown automatically while a download is running.",
                    onClick = null,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.MusicNote,
                    title = "Music player controls",
                    subtitle = "Shown automatically during audio playback.",
                    onClick = null,
                )
            }
        }
    }
}
