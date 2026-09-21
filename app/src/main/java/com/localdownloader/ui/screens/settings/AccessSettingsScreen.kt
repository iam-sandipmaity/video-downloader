package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferenceNavigationRow
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun AccessSettingsScreen(
    uiState: FormatUiState,
    onAllowMeteredDownloadsChanged: (Boolean) -> Unit,
    onCookiesEnabledChanged: (Boolean) -> Unit,
    onCookieUserAgentEnabledChanged: (Boolean) -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val youtubeConfigured = uiState.youtubeAuthConfig.isConfigured()
    val cookieCount = uiState.cookieProfiles.size

    PreferencePageScaffold(
        title = stringResource(R.string.settings_access_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "NETWORK RULES")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.SignalCellularAlt,
                title = stringResource(R.string.access_download_cellular_title),
                description = stringResource(R.string.access_download_cellular_subtitle),
                isChecked = uiState.allowMeteredDownloads,
                onClick = { onAllowMeteredDownloadsChanged(!uiState.allowMeteredDownloads) },
            )
        }
        item {
            PreferenceSubtitle(text = "COOKIES & AUTHENTICATION")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Cookie,
                title = stringResource(R.string.access_use_cookies_title),
                isChecked = uiState.cookiesEnabled,
                onClick = { onCookiesEnabledChanged(!uiState.cookiesEnabled) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.access_user_agent_title),
                isChecked = uiState.cookieUserAgentEnabled,
                onClick = { onCookieUserAgentEnabledChanged(!uiState.cookieUserAgentEnabled) },
            )
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.OpenInNew,
                title = stringResource(R.string.access_saved_cookies_title),
                value = if (cookieCount == 0) stringResource(R.string.common_open) else "$cookieCount saved",
                onClick = onOpenCookies,
            )
        }
        item {
            PreferenceSubtitle(text = "PLATFORM ACCESS")
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.more_youtube_access_title),
                value = if (youtubeConfigured) {
                    if (uiState.youtubeAuthConfig.enabled) {
                        stringResource(R.string.common_enabled)
                    } else {
                        stringResource(R.string.common_ready)
                    }
                } else {
                    stringResource(R.string.common_set_up)
                },
                onClick = onOpenYoutubeAccess,
            )
        }
    }
}

