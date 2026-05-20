package com.localdownloader.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
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
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader
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
        item {
            PreferenceHeroCard(
                eyebrow = "Revamped",
                title = "A lighter control center for the whole app",
                subtitle = "Inspired by Seal's smoother settings flow, but still shaped around this app's broader toolset and download workflow.",
                badges = listOf(
                    themeModeLabel(uiState.themeMode),
                    "${uiState.cookieProfiles.size} cookies",
                    "v${BuildConfig.VERSION_NAME}",
                ),
            )
        }
        settingsMessages(
            settingsInfoMessage = settingsInfoMessage,
            settingsErrorMessage = settingsErrorMessage,
            mediaInfoMessage = mediaInfoMessage,
            mediaErrorMessage = mediaErrorMessage,
            onDismissMediaLibraryMessage = onDismissMediaLibraryMessage,
        )
        item {
            PreferenceSectionHeader(
                title = "Personalize",
                subtitle = "Visual controls that shape how calm, bright, or contrast-heavy the app feels.",
            )
        }
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
            PreferenceSectionHeader(
                title = "Download behavior",
                subtitle = "Defaults for filenames, formats, slots, and post-processing actions.",
            )
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
                    subtitle = "Quick links into Android's app and per-channel notification controls.",
                    onClick = onOpenNotifications,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Access and support",
                subtitle = "Session handling, YouTube retries, update entry points, and the app story around them.",
            )
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
    return "$cookieSummary $youtubeSummary"
}

private fun accessValue(uiState: FormatUiState): String {
    return when {
        uiState.youtubeAuthConfig.isConfigured() && uiState.cookiesEnabled -> "Ready"
        uiState.cookiesEnabled -> "Cookies on"
        else -> "Set up"
    }
}
