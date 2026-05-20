package com.localdownloader.ui.screens.settings

import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader
import com.localdownloader.ui.components.PreferenceSwitchRow
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun AccessSettingsScreen(
    uiState: FormatUiState,
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
        title = "Access and network",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceHeroCard(
                eyebrow = "Protected sites",
                title = "Keep recovery tools close without crowding the main workflow",
                subtitle = "This page concentrates the access-related switches and deep links that usually matter only when a site becomes stricter or a retry starts failing.",
                badges = listOf(
                    if (uiState.cookiesEnabled) "Cookies on" else "Cookies off",
                    if (youtubeConfigured) "YouTube ready" else "YouTube not ready",
                    "$cookieCount saved",
                ),
            )
        }
        item {
            PreferenceSectionHeader(
                title = "Cookie-backed access",
                subtitle = "Session-backed retries work best when the app knows both whether cookies are enabled and whether a stricter browser-style header should go out with them.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Cookie,
                    title = "Use cookies",
                    subtitle = "Attach saved website cookies automatically when a matching link is analyzed or downloaded.",
                    checked = uiState.cookiesEnabled,
                    onCheckedChange = onCookiesEnabledChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Security,
                    title = "Send User-Agent with cookies",
                    subtitle = "Helpful when a site expects requests to look closer to a normal signed-in browser session.",
                    checked = uiState.cookieUserAgentEnabled,
                    onCheckedChange = onCookieUserAgentEnabledChanged,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.OpenInNew,
                    title = "Saved cookies",
                    subtitle = if (cookieCount == 0) {
                        "Add a site session once, then refresh it whenever that login changes."
                    } else {
                        "$cookieCount saved site session${if (cookieCount == 1) "" else "s"} ready to inspect, update, import, or export."
                    },
                    value = if (cookieCount == 0) "Open" else "$cookieCount",
                    onClick = onOpenCookies,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "YouTube recovery",
                subtitle = "For tougher YouTube playback checks, the app can keep PO tokens and matching cookie state together in a dedicated setup flow.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Shield,
                    title = "YouTube access",
                    subtitle = if (youtubeConfigured) {
                        "Saved PO tokens are ready, so you can refresh or inspect the current setup when retries get blocked."
                    } else {
                        "Generate YouTube access once to capture the saved session and PO tokens used on stricter retries."
                    },
                    value = if (youtubeConfigured) {
                        if (uiState.youtubeAuthConfig.enabled) "Enabled" else "Ready"
                    } else {
                        "Set up"
                    },
                    onClick = onOpenYoutubeAccess,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.TravelExplore,
                    title = "Recovery path",
                    subtitle = "The usual order is cookies first, then YouTube access if retries still keep stalling on protected playback checks.",
                    value = "Guide",
                    onClick = onOpenYoutubeAccess,
                )
            }
        }
    }
}
