package com.localdownloader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.support.openSupportIssue
import com.localdownloader.ui.support.shareAppLogs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    PreferencePageScaffold(
        title = stringResource(R.string.help_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "QUICK ACTIONS")
        }
        item {
            HelpActionGrid(
                onOpenCookies = onOpenCookies,
                onOpenYoutubeAccess = onOpenYoutubeAccess,
                onExportLogs = { shareAppLogs(context) },
                onReportIssue = { openSupportIssue(context) },
            )
        }
        item {
            PreferenceSubtitle(text = "GETTING STARTED")
        }
        item {
            HelpSectionCard(
                title = stringResource(R.string.help_section_first_move),
            ) {
                HelpTimelineStep(
                    number = "1",
                    title = stringResource(R.string.help_step_1_title),
                    body = stringResource(R.string.help_step_1_body),
                )
                DividerInset()
                HelpTimelineStep(
                    number = "2",
                    title = stringResource(R.string.help_step_2_title),
                    body = stringResource(R.string.help_step_2_body),
                )
                DividerInset()
                HelpTimelineStep(
                    number = "3",
                    title = stringResource(R.string.help_step_3_title),
                    body = stringResource(R.string.help_step_3_body),
                )
                DividerInset()
                HelpTimelineStep(
                    number = "4",
                    title = stringResource(R.string.help_step_4_title),
                    body = stringResource(R.string.help_step_4_body),
                )
            }
        }
        item {
            PreferenceSubtitle(text = "NAVIGATION & FEATURES")
        }
        item {
            HelpSectionCard(
                title = stringResource(R.string.help_section_where),
            ) {
                HelpInfoRow(
                    icon = Icons.Rounded.Home,
                    title = stringResource(R.string.help_home_title),
                    body = stringResource(R.string.help_home_body),
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.help_downloads_title),
                    body = stringResource(R.string.help_downloads_body),
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Rounded.Web,
                    title = stringResource(R.string.help_more_title),
                    body = stringResource(R.string.help_more_body),
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.help_settings_title),
                    body = stringResource(R.string.help_settings_body),
                )
            }
        }
        item {
            PreferenceSubtitle(text = "TROUBLESHOOTING")
        }
        item {
            HelpSectionCard(
                title = stringResource(R.string.help_section_fix),
            ) {
                HelpTipRow(
                    icon = Icons.Rounded.ErrorOutline,
                    title = stringResource(R.string.help_fix_analysis_title),
                    body = stringResource(R.string.help_fix_analysis_body),
                )
                DividerInset()
                HelpTipRow(
                    icon = Icons.Rounded.Cookie,
                    title = stringResource(R.string.help_fix_access_title),
                    body = stringResource(R.string.help_fix_access_body),
                )
                DividerInset()
                HelpTipRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.help_fix_youtube_title),
                    body = stringResource(R.string.help_fix_youtube_body),
                )
                DividerInset()
                HelpTipRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.help_fix_playlist_title),
                    body = stringResource(R.string.help_fix_playlist_body),
                )
            }
        }
        item {
            PreferenceSubtitle(text = "MEDIA TOOLS")
        }
        item {
            HelpSectionCard(
                title = stringResource(R.string.help_section_tools),
            ) {
                HelpInfoRow(
                    icon = Icons.Rounded.Transform,
                    title = stringResource(R.string.help_compressor_title),
                    body = stringResource(R.string.help_compressor_body),
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Rounded.SwapHoriz,
                    title = stringResource(R.string.help_converter_title),
                    body = stringResource(R.string.help_converter_body),
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.help_music_title),
                    body = stringResource(R.string.help_music_body),
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Rounded.Folder,
                    title = stringResource(R.string.help_folders_title),
                    body = stringResource(R.string.help_folders_body),
                )
            }
        }
        item {
            PreferenceSubtitle(text = "REPORTING ISSUES")
        }
        item {
            HelpSectionCard(
                title = stringResource(R.string.help_section_issue),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HelpBadge(stringResource(R.string.help_badge_screenshot))
                    HelpBadge(stringResource(R.string.help_badge_logs))
                    HelpBadge(stringResource(R.string.help_badge_short_explanation))
                    HelpBadge(stringResource(R.string.help_badge_site_context))
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.help_best_report_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.help_best_report_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpActionGrid(
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onExportLogs: () -> Unit,
    onReportIssue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Cookie,
                title = stringResource(R.string.help_open_cookies),
                subtitle = stringResource(R.string.help_open_cookies_subtitle),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenCookies,
            )
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.help_open_youtube_access),
                subtitle = stringResource(R.string.help_open_youtube_access_subtitle),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = onOpenYoutubeAccess,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.BugReport,
                title = stringResource(R.string.help_export_logs),
                subtitle = stringResource(R.string.help_export_logs_subtitle),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onExportLogs,
            )
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.HelpOutline,
                title = stringResource(R.string.help_report_issue),
                subtitle = stringResource(R.string.help_report_issue_subtitle),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconTint = MaterialTheme.colorScheme.onSurface,
                onClick = onReportIssue,
            )
        }
    }
}

@Composable
private fun HelpActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconBackgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconBackgroundColor,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun HelpTimelineStep(
    number: String,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelpInfoRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelpTipRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HelpBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DividerInset() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    )
}
