package com.localdownloader.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader

@Composable
fun MoreScreen(
    onOpenQueue: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompress: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePageScaffold(
        title = "More",
        onBack = null,
        modifier = modifier,
    ) {
        item {
            PreferenceHeroCard(
                eyebrow = "Utility center",
                title = "Everything that helps after Home and Downloads",
                subtitle = "This area stays focused on support, recovery, tooling, and configuration so the main workflow can stay calmer and lighter.",
                badges = listOf("Queue", "Access", "Tools"),
            )
        }
        item {
            PreferenceSectionHeader(
                title = "Workflow",
                subtitle = "Shortcuts for the places you usually open when something is waiting, finished, or needs a settings-level adjustment.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = "Download queue",
                    subtitle = "See what is preparing, downloading, paused, or waiting next.",
                    onClick = onOpenQueue,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.History,
                    title = "History",
                    subtitle = "Review completed downloads and their recent activity.",
                    onClick = onOpenHistory,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Settings,
                    title = "Settings",
                    subtitle = "Open the new Seal-inspired settings hub for appearance, storage, notifications, access, and about.",
                    onClick = onOpenSettings,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Access and upkeep",
                subtitle = "The recovery and maintenance paths that usually matter when a site gets stricter or you want to check the runtime layer.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Web,
                    title = "Cookies",
                    subtitle = "Manage saved site sessions used by the downloader.",
                    onClick = onOpenCookies,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Shield,
                    title = "YouTube access",
                    subtitle = "Handle cookies, PO generation, and tougher protected playback recovery.",
                    onClick = onOpenYoutubeAccess,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Updates",
                    subtitle = "Check app, yt-dlp, and FFmpeg versions from one update center.",
                    onClick = onOpenUpdates,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.HelpOutline,
                    title = "Help",
                    subtitle = "Open guides, support notes, and troubleshooting tips.",
                    onClick = onOpenHelp,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Media tools",
                subtitle = "Quick local file tools that sit beside the downloader rather than inside the main fetch flow.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.SwapHoriz,
                    title = "Converter",
                    subtitle = "Turn downloaded media into a different format.",
                    onClick = onOpenConvert,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Transform,
                    title = "Compressor",
                    subtitle = "Reduce file size before sharing or archiving.",
                    onClick = onOpenCompress,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.ManageAccounts,
                    title = "Support posture",
                    subtitle = "The refreshed layout keeps settings, recovery tools, and updates easier to reach without crowding the main tabs.",
                    value = "New UI",
                    onClick = null,
                )
            }
        }
    }
}
