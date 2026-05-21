package com.localdownloader.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localdownloader.BuildConfig
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.screens.settings.accentLabel
import com.localdownloader.ui.screens.settings.themeModeLabel
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun SettingsScreen(
    uiState: FormatUiState,
    savedItemsCount: Int,
    mediaInfoMessage: String? = null,
    mediaErrorMessage: String? = null,
    onDismissMediaLibraryMessage: () -> Unit = {},
    onOpenAppearance: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAppLog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsInfoMessage = uiState.infoMessageFor(FormatMessageScope.SETTINGS)
    val settingsErrorMessage = uiState.errorMessageFor(FormatMessageScope.SETTINGS)

    PreferencePageScaffold(
        title = "Settings",
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
                PreferenceRow(
                    icon = Icons.Rounded.Palette,
                    title = "Appearance",
                    subtitle = "Theme mode, accent palette, contrast, and language.",
                    value = "${themeModeLabel(uiState.themeMode)} / ${accentLabel(uiState.accentPreset)}",
                    onClick = onOpenAppearance,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = "Download defaults",
                    subtitle = "Templates, containers, subtitles, metadata, artwork, and queue slot preference.",
                    value = uiState.selectedContainer.uppercase(),
                    onClick = onOpenDownloads,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Folder,
                    title = "Folders and storage",
                    subtitle = "Root folders, cleanup behavior, cache tools, and library maintenance.",
                    value = "${savedItemsCount} saved",
                    onClick = onOpenStorage,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Notifications",
                    subtitle = "Completion, error, cancel, and playback alerts.",
                    onClick = onOpenNotifications,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Security,
                    title = "Access and network",
                    subtitle = buildAccessSummary(uiState),
                    value = accessValue(uiState),
                    onClick = onOpenAccess,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Info,
                    title = "About and support",
                    subtitle = "Version details, links, updates center, developer pages, and preference reset actions.",
                    value = BuildConfig.VERSION_NAME,
                    onClick = onOpenAbout,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Description,
                    title = "App log reader",
                    subtitle = "Read app.log inside the app, filter lines by failures, successes, or day, then copy or export the result.",
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
                label = "Settings",
                message = settingsInfoMessage,
                isError = false,
            )
        }
    }
    if (!settingsErrorMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = "Settings",
                message = settingsErrorMessage,
                isError = true,
            )
        }
    }
    if (!mediaInfoMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = "Library",
                message = mediaInfoMessage,
                isError = false,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }
    }
    if (!mediaErrorMessage.isNullOrBlank()) {
        item {
            InlineFeedbackCard(
                label = "Library",
                message = mediaErrorMessage,
                isError = true,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }
    }
}

private fun buildAccessSummary(uiState: FormatUiState): String {
    val networkSummary = if (uiState.allowMeteredDownloads) {
        "Cellular downloads are allowed."
    } else {
        "Downloads wait for Wi-Fi."
    }
    val cookieSummary = if (uiState.cookieProfiles.isEmpty()) {
        "No site sessions saved yet."
    } else {
        "${uiState.cookieProfiles.size} saved site session${if (uiState.cookieProfiles.size == 1) "" else "s"} ready to reuse."
    }
    val youtubeSummary = if (uiState.youtubeAuthConfig.isConfigured()) {
        "YouTube access is configured for tougher retries."
    } else {
        "YouTube access is not configured yet."
    }
    return "$networkSummary $cookieSummary $youtubeSummary"
}

private fun accessValue(uiState: FormatUiState): String {
    return when {
        uiState.allowMeteredDownloads -> "Cellular allowed"
        else -> "Wi-Fi only"
    }
}
