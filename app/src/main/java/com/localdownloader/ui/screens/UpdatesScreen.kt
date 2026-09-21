package com.localdownloader.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.localdownloader.BuildConfig
import com.localdownloader.R
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferenceNavigationRow
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.ui.screens.settings.SettingChoiceDialog
import com.localdownloader.ui.screens.settings.SettingChoiceDialogState
import com.localdownloader.ui.screens.settings.SettingChoiceOption
import com.localdownloader.updates.FfmpegReleaseChannel
import com.localdownloader.updates.PreparedAppUpdate
import com.localdownloader.updates.YtDlpReleaseChannel
import com.localdownloader.viewmodel.UpdateSectionUiState
import com.localdownloader.viewmodel.UpdatesUiState
import java.io.File

@Composable
fun UpdatesScreen(
    uiState: UpdatesUiState,
    onBack: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApp: () -> Unit,
    onRefreshYtDlp: () -> Unit,
    onRefreshFfmpeg: () -> Unit,
    onInstallAppUpdate: () -> Unit,
    onInstallYtDlpUpdate: () -> Unit,
    onInstallFfmpegUpdate: () -> Unit,
    onYtDlpChannelChanged: (YtDlpReleaseChannel) -> Unit,
    onFfmpegChannelChanged: (FfmpegReleaseChannel) -> Unit,
    onAutoUpdateYtDlpChanged: (Boolean) -> Unit,
    onIncludePrereleaseAppReleasesChanged: (Boolean) -> Unit,
    onCheckUpdatesOnStartupChanged: (Boolean) -> Unit,
    onOpenChangelog: (String) -> Unit,
    onConsumePendingAppInstall: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    LaunchedEffect(uiState.pendingAppInstallRequestId) {
        val pendingInstall = uiState.pendingAppInstall ?: return@LaunchedEffect
        if (pendingInstall.requiresInstallPermission) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            launchApkInstaller(context = context, preparedUpdate = pendingInstall)
            onConsumePendingAppInstall()
        }
    }

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.updates_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = onRefreshAll) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.updates_refresh_all),
                )
            }
        },
    ) {
        if (uiState.infoMessage != null || uiState.errorMessage != null) {
            item {
                InlineFeedbackCard(
                    label = "Updates",
                    message = uiState.errorMessage ?: uiState.infoMessage.orEmpty(),
                    isError = uiState.errorMessage != null,
                    onDismiss = onDismissMessage,
                )
            }
        }

        item {
            PreferenceSubtitle(text = "APPLICATION")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.updates_current_version),
                description = stringResource(R.string.updates_app_current_subtitle),
                value = uiState.app.currentVersion ?: stringResource(R.string.common_unknown),
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Settings,
                title = "App release channel",
                description = if (BuildConfig.APP_RELEASE_CHANNEL.equals("nightly", ignoreCase = true)) {
                    "Nightly builds check the rolling nightly release tag"
                } else {
                    "Stable channel checks official release tags"
                },
                value = BuildConfig.APP_RELEASE_CHANNEL,
                onClick = null,
            )
        }
        if (BuildConfig.APP_RELEASE_CHANNEL.equals("nightly", ignoreCase = true)) {
            item {
                PreferenceItem(
                    icon = Icons.Rounded.Settings,
                    title = "Prerelease checks",
                    description = "Locked to nightly so stable and nightly packages cannot cross-update",
                    value = "nightly only",
                    onClick = null,
                )
            }
        } else {
            item {
                PreferenceSwitch(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.updates_beta_releases),
                    description = stringResource(R.string.updates_app_beta_subtitle),
                    isChecked = uiState.preferences.includePrereleaseAppReleases,
                    onClick = { onIncludePrereleaseAppReleasesChanged(!uiState.preferences.includePrereleaseAppReleases) },
                )
            }
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Sync,
                title = stringResource(R.string.updates_check_on_startup_title),
                description = stringResource(R.string.updates_check_on_startup_subtitle),
                isChecked = uiState.preferences.checkUpdatesOnStartup,
                onClick = { onCheckUpdatesOnStartupChanged(!uiState.preferences.checkUpdatesOnStartup) },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Refresh,
                title = stringResource(R.string.updates_check_now),
                description = buildCheckSubtitle(uiState.app),
                onClick = onRefreshApp,
            )
        }
        if (uiState.app.updateAvailable || uiState.app.isInstalling || uiState.pendingAppInstall != null) {
            item {
                PreferenceItem(
                    icon = Icons.Rounded.Download,
                    title = if (uiState.pendingAppInstall != null) {
                        stringResource(R.string.updates_continue_app_install)
                    } else {
                        stringResource(R.string.updates_install_app_update)
                    },
                    description = buildInstallSubtitle(
                        section = uiState.app,
                        hasPreparedInstall = uiState.pendingAppInstall != null,
                    ),
                    onClick = onInstallAppUpdate,
                )
            }
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.updates_changelog),
                description = stringResource(R.string.updates_app_changelog_subtitle),
                onClick = { onOpenChangelog(UpdateChangelogSections.APP) },
            )
        }

        item {
            PreferenceSubtitle(text = "YT-DLP RUNTIME")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.updates_current_version),
                description = stringResource(R.string.updates_ytdlp_current_subtitle),
                value = uiState.ytDlp.currentVersion ?: stringResource(R.string.common_unknown),
                onClick = null,
            )
        }
        item {
            val ytdlpSourceTitle = stringResource(R.string.updates_ytdlp_source)
            PreferenceItem(
                icon = Icons.Rounded.Settings,
                title = ytdlpSourceTitle,
                description = uiState.preferences.ytDlpChannel.description,
                value = uiState.preferences.ytDlpChannel.id,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = ytdlpSourceTitle,
                        selected = uiState.preferences.ytDlpChannel.title,
                        options = YtDlpReleaseChannel.entries.map { channel ->
                            SettingChoiceOption(
                                title = channel.title,
                                subtitle = channel.description,
                                onSelect = { onYtDlpChannelChanged(channel) },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Sync,
                title = stringResource(R.string.updates_auto_update_ytdlp),
                description = stringResource(
                    if (BuildConfig.YTDLP_AUTO_UPDATE_DEFAULT) {
                        R.string.updates_ytdlp_auto_subtitle
                    } else {
                        R.string.updates_ytdlp_auto_subtitle_repo_safe
                    },
                ),
                isChecked = uiState.preferences.autoUpdateYtDlp,
                onClick = { onAutoUpdateYtDlpChanged(!uiState.preferences.autoUpdateYtDlp) },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Refresh,
                title = stringResource(R.string.updates_check_now),
                description = buildCheckSubtitle(uiState.ytDlp),
                onClick = onRefreshYtDlp,
            )
        }
        if (uiState.ytDlp.updateAvailable || uiState.ytDlp.isInstalling) {
            item {
                PreferenceItem(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.updates_install_ytdlp),
                    description = buildInstallSubtitle(section = uiState.ytDlp),
                    onClick = onInstallYtDlpUpdate,
                )
            }
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.updates_changelog),
                description = if (uiState.ytDlp.releaseNotes.isNullOrBlank()) {
                    stringResource(R.string.updates_ytdlp_changelog_subtitle_fallback)
                } else {
                    stringResource(R.string.updates_ytdlp_changelog_subtitle_ready)
                },
                onClick = { onOpenChangelog(UpdateChangelogSections.YT_DLP) },
            )
        }

        item {
            PreferenceSubtitle(text = "FFMPEG RUNTIME")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.updates_current_version),
                description = stringResource(R.string.updates_ffmpeg_current_subtitle),
                value = uiState.ffmpeg.currentVersion ?: stringResource(R.string.common_unknown),
                onClick = null,
            )
        }
        item {
            val ffmpegSourceTitle = stringResource(R.string.updates_ffmpeg_source)
            PreferenceItem(
                icon = Icons.Rounded.Settings,
                title = ffmpegSourceTitle,
                description = uiState.preferences.ffmpegChannel.description,
                value = uiState.preferences.ffmpegChannel.id,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = ffmpegSourceTitle,
                        selected = uiState.preferences.ffmpegChannel.title,
                        options = FfmpegReleaseChannel.entries.map { channel ->
                            SettingChoiceOption(
                                title = channel.title,
                                subtitle = channel.description,
                                onSelect = { onFfmpegChannelChanged(channel) },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Refresh,
                title = stringResource(R.string.updates_check_now),
                description = buildCheckSubtitle(uiState.ffmpeg),
                onClick = onRefreshFfmpeg,
            )
        }
        if (shouldShowFfmpegInstallRow(uiState.ffmpeg)) {
            item {
                PreferenceItem(
                    icon = Icons.Rounded.Download,
                    title = when {
                        uiState.ffmpeg.latestCheck?.requiresInitialInstall == true && uiState.ffmpeg.updateAvailable ->
                            "Install FFmpeg ${uiState.ffmpeg.latestVersion?.let { "v$it" }.orEmpty()}".trim()
                        uiState.ffmpeg.latestCheck?.requiresInitialInstall == true ->
                            stringResource(R.string.updates_install_ffmpeg)
                        else ->
                            stringResource(R.string.updates_update_ffmpeg)
                    },
                    description = buildInstallSubtitle(
                        section = uiState.ffmpeg,
                        isInitialInstall = uiState.ffmpeg.latestCheck?.requiresInitialInstall == true,
                    ),
                    onClick = onInstallFfmpegUpdate,
                )
            }
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.updates_changelog),
                description = if (uiState.ffmpeg.releaseNotes.isNullOrBlank()) {
                    stringResource(R.string.updates_ffmpeg_changelog_subtitle_fallback)
                } else {
                    stringResource(R.string.updates_ffmpeg_changelog_subtitle_ready)
                },
                onClick = { onOpenChangelog(UpdateChangelogSections.FFMPEG) },
            )
        }
    }
}

private fun buildCheckSubtitle(section: UpdateSectionUiState): String {
    return when {
        section.isChecking -> "Checking for updates..."
        section.updateAvailable -> "Latest available: ${section.latestVersion ?: "Unknown"}"
        !section.lastStatus.isNullOrBlank() -> "${section.summary} Last status: ${section.lastStatus}"
        else -> section.summary
    }
}

private fun buildInstallSubtitle(
    section: UpdateSectionUiState,
    hasPreparedInstall: Boolean = false,
    isInitialInstall: Boolean = false,
): String {
    return when {
        section.isInstalling && section.progressPercent != null -> "Downloading... ${section.progressPercent}%"
        hasPreparedInstall -> "The update APK is already downloaded and ready to install."
        section.updateAvailable && isInitialInstall -> "Install managed runtime package v${section.latestVersion ?: ""} for direct in-app updates."
        section.updateAvailable -> "Install version ${section.latestVersion ?: "the latest version"}"
        isInitialInstall -> "Install the optional managed runtime so FFmpeg can be updated directly in the app."
        else -> section.summary
    }
}

private fun shouldShowFfmpegInstallRow(section: UpdateSectionUiState): Boolean {
    return section.isInstalling ||
        section.updateAvailable ||
        section.latestCheck?.requiresInitialInstall == true
}

private fun launchApkInstaller(
    context: android.content.Context,
    preparedUpdate: PreparedAppUpdate,
) {
    val apkFile = File(preparedUpdate.apkPath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }
    context.startActivity(intent)
}
