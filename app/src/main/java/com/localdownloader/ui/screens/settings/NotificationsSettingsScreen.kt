package com.localdownloader.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.localdownloader.notifications.AppNotifications
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader

@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    fun openChannelSettings(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
            context.startActivity(intent)
        } else {
            openAppNotificationSettings()
        }
    }

    PreferencePageScaffold(
        title = "Notifications",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceHeroCard(
                eyebrow = "System handoff",
                title = "Use Android's own notification controls when you need them",
                subtitle = "This page stays intentionally light and forwards you into the device settings pages that already own notification permissions and per-channel behavior.",
                badges = listOf("Active", "Completed", "Errors"),
            )
        }
        item {
            PreferenceSectionHeader(
                title = "App-level controls",
                subtitle = "Open the main notification entry point for this app before diving into any one channel.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "App notification settings",
                    subtitle = "Open Android's main notification controls for this app.",
                    onClick = ::openAppNotificationSettings,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Notification channels",
                subtitle = "Fine-tune the categories that matter without digging through the system settings manually.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Active downloads",
                    subtitle = "Live progress cards for currently running items.",
                    onClick = { openChannelSettings(AppNotifications.CHANNEL_ACTIVE_DOWNLOADS) },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Completed downloads",
                    subtitle = "Completion notifications for each finished file.",
                    onClick = { openChannelSettings(AppNotifications.CHANNEL_COMPLETED_DOWNLOADS) },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.ErrorOutline,
                    title = "Download errors",
                    subtitle = "Failed items that need attention.",
                    onClick = { openChannelSettings(AppNotifications.CHANNEL_DOWNLOAD_ERRORS) },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.NotificationsOff,
                    title = "Canceled downloads",
                    subtitle = "Alerts for tasks you stop yourself.",
                    onClick = { openChannelSettings(AppNotifications.CHANNEL_CANCELED_DOWNLOADS) },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.MusicNote,
                    title = "Music player controls",
                    subtitle = "Previous, next, seek, play, and pause notification controls.",
                    onClick = { openChannelSettings(AppNotifications.CHANNEL_AUDIO_PLAYBACK) },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Campaign,
                    title = "Promotions and updates",
                    subtitle = "Optional product announcements and future promotional alerts.",
                    onClick = { openChannelSettings(AppNotifications.CHANNEL_PROMOTIONS) },
                )
            }
        }
    }
}
