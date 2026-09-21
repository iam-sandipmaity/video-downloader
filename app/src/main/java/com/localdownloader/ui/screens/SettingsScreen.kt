package com.localdownloader.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.localdownloader.BuildConfig
import com.localdownloader.R
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceNavigationRow
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun SettingsScreen(
    uiState: FormatUiState,
    savedItemsCount: Int,
    modifier: Modifier = Modifier,
    mediaInfoMessage: String? = null,
    mediaErrorMessage: String? = null,
    onDismissMediaLibraryMessage: () -> Unit = {},
    onOpenAppearance: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAppLog: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settingsInfoMessage = uiState.infoMessageFor(FormatMessageScope.SETTINGS)
    val settingsErrorMessage = uiState.errorMessageFor(FormatMessageScope.SETTINGS)

    PreferencePageScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        settingsMessages(
            settingsInfoMessage = settingsInfoMessage,
            settingsErrorMessage = settingsErrorMessage,
            mediaInfoMessage = mediaInfoMessage,
            mediaErrorMessage = mediaErrorMessage,
            onDismissMediaLibraryMessage = onDismissMediaLibraryMessage,
        )
        item {
            PreferenceGroup {
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = "Theme, accent color, and language",
                    onClick = onOpenAppearance,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceNavigationRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.settings_download_defaults_title),
                    subtitle = "Format templates, containers, and queue",
                    onClick = onOpenDownloads,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = stringResource(R.string.settings_battery_title),
                    subtitle = "Threads, eco mode, and optimization",
                    onClick = onOpenBattery,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Folder,
                    title = stringResource(R.string.settings_storage_title),
                    subtitle = "Storage directories and cache cleanup",
                    onClick = onOpenStorage,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = "Completion and error alerts",
                    onClick = onOpenNotifications,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.settings_access_title),
                    subtitle = "Network rules, cookies, and YouTube",
                    onClick = onOpenAccess,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = "Version details and project links",
                    value = BuildConfig.VERSION_NAME,
                    onClick = onOpenAbout,
                )
                PreferenceDivider()
                PreferenceNavigationRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.settings_app_log_title),
                    subtitle = "View and export app logs",
                    onClick = onOpenAppLog,
                )
            }
        }
    }
}

private fun LazyListScope.settingsMessages(
    settingsInfoMessage: String?,
    settingsErrorMessage: String?,
    mediaInfoMessage: String?,
    mediaErrorMessage: String?,
    onDismissMediaLibraryMessage: () -> Unit,
) {
    if (!settingsInfoMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = stringResource(R.string.settings_feedback_label),
                message = settingsInfoMessage,
                isError = false,
            )
        }
    }
    if (!settingsErrorMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = stringResource(R.string.settings_feedback_label),
                message = settingsErrorMessage,
                isError = true,
            )
        }
    }
    if (!mediaInfoMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = stringResource(R.string.settings_library_label),
                message = mediaInfoMessage,
                isError = false,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }
    }
    if (!mediaErrorMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = stringResource(R.string.settings_library_label),
                message = mediaErrorMessage,
                isError = true,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }
    }
}

private fun buildAccessSummary(uiState: FormatUiState, context: android.content.Context): String {
    val resources = context.resources
    val networkSummary = if (uiState.allowMeteredDownloads) {
        resources.getString(R.string.settings_access_summary_cellular)
    } else {
        resources.getString(R.string.settings_access_summary_wifi)
    }
    val cookieSummary = if (uiState.cookieProfiles.isEmpty()) {
        resources.getString(R.string.settings_access_summary_no_cookies)
    } else {
        resources.getQuantityString(
            R.plurals.settings_access_summary_cookie_count,
            uiState.cookieProfiles.size,
            uiState.cookieProfiles.size,
        )
    }
    val youtubeSummary = if (uiState.youtubeAuthConfig.isConfigured()) {
        resources.getString(R.string.settings_access_summary_youtube_configured)
    } else {
        resources.getString(R.string.settings_access_summary_youtube_missing)
    }
    return "$networkSummary $cookieSummary $youtubeSummary"
}

