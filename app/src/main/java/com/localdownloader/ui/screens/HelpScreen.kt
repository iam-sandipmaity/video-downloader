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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.Web
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.ui.components.PreferencePageScaffold
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
        title = "Help",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            HelpActionGrid(
                onOpenCookies = onOpenCookies,
                onOpenYoutubeAccess = onOpenYoutubeAccess,
                onExportLogs = { shareAppLogs(context) },
                onReportIssue = { openSupportIssue(context) },
            )
        }
        item {
            HelpSectionCard(
                title = "Best first move when a link acts up",
            ) {
                HelpTimelineStep(
                    number = "1",
                    title = "Start normal",
                    body = "Paste the link on Home, analyze it, choose the format you want, and queue the download. Cookies are recommended, but not required to begin.",
                )
                DividerInset()
                HelpTimelineStep(
                    number = "2",
                    title = "Use the queue before retrying blindly",
                    body = "Open the queue, inspect the item, and use retry or the diagnostics panel first. The queue can now show the source host, recent logs, and error details in one place.",
                )
                DividerInset()
                HelpTimelineStep(
                    number = "3",
                    title = "Add access only when the site needs it",
                    body = "Use Cookies for sign-in, age-gated, region-limited, or session-protected pages. For YouTube, open YouTube access when tougher long-form retries need PO generation too.",
                )
                DividerInset()
                HelpTimelineStep(
                    number = "4",
                    title = "Report with proof if it still fails",
                    body = "Export log.txt, take a screenshot of the failure state, and explain what you expected versus what happened. That usually saves a lot of back-and-forth.",
                )
            }
        }
        item {
            HelpSectionCard(
                title = "Know where each screen helps",
            ) {
                HelpInfoRow(
                    icon = Icons.Outlined.Home,
                    title = "Home",
                    body = "Paste links, analyze media, compare formats, and queue the download you want.",
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Outlined.CloudDownload,
                    title = "Downloads",
                    body = "Open completed files, rename them, share them, clean the library, or jump into playback.",
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Outlined.Web,
                    title = "More",
                    body = "This is where quick shortcuts for queue, updates, help, and media tools live, while the deeper access setup also lives in Settings.",
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    body = "Change folders, notifications, theme, contrast, download defaults, and cleanup behavior without cluttering the main tabs.",
                )
            }
        }
        item {
            HelpSectionCard(
                title = "What usually fixes each type of problem",
            ) {
                HelpTipRow(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Analysis fails immediately",
                    body = "Retry once first. Some pages expose unstable metadata on the first pass, especially when the extractor has to refresh fresh session data.",
                )
                DividerInset()
                HelpTipRow(
                    icon = Icons.Outlined.Web,
                    title = "Sign-in, age, private, or region checks",
                    body = "Add a cookie for the exact site. A matching browser session often fixes access problems without changing your format choices.",
                )
                DividerInset()
                HelpTipRow(
                    icon = Icons.Outlined.Shield,
                    title = "YouTube retries keep failing later",
                    body = "Regenerate YouTube access, then retry the failed item from the queue. That refreshes the saved cookies and PO tokens together.",
                )
                DividerInset()
                HelpTipRow(
                    icon = Icons.Outlined.Info,
                    title = "A playlist item behaves differently from one-off downloads",
                    body = "Use item actions for one playlist entry and tab-level batch actions when you mean the whole group. They are intentionally separate now.",
                )
            }
        }
        item {
            HelpSectionCard(
                title = "Built-in tools after the download is saved",
            ) {
                HelpInfoRow(
                    icon = Icons.Outlined.Transform,
                    title = "Compressor",
                    body = "Lower resolution or bitrate before sharing when storage space or upload size matters more than perfect quality.",
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Converter",
                    body = "Change a local file into another container or audio format using FFmpeg when a target device needs something specific.",
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Downloaded music flow",
                    body = "Audio files can jump into the built-in player without cluttering the rest of the downloads library.",
                )
                DividerInset()
                HelpInfoRow(
                    icon = Icons.Outlined.Storage,
                    title = "Folders and cleanup",
                    body = "Downloads stay grouped under the folders you choose in Settings, and cache cleanup is separate from deleting real saved media.",
                )
            }
        }
        item {
            HelpSectionCard(
                title = "What to include when you open an issue",
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HelpBadge("Screenshot")
                    HelpBadge("Exported log.txt")
                    HelpBadge("Short explanation")
                    HelpBadge("Site or URL context")
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Best report formula",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Say what link or site you used, what output you chose, what you expected, what actually happened, and attach the exported logs. If the link is private, explain the site and the access condition instead of pasting sensitive data.",
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
                icon = Icons.Outlined.Web,
                title = "Open Cookies",
                subtitle = "Save a site session for protected links.",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenCookies,
            )
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Shield,
                title = "YouTube access",
                subtitle = "Refresh saved YouTube cookies and PO tokens.",
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f),
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
                icon = Icons.Outlined.Storage,
                title = "Export logs",
                subtitle = "Share app.log and crash.log in one step.",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f),
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onExportLogs,
            )
            HelpActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Info,
                title = "Report issue",
                subtitle = "Open the GitHub issue form with reporting guidance.",
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.55f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp),
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
        shape = RoundedCornerShape(30.dp),
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
                modifier = Modifier.size(34.dp),
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
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(12.dp)
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(10.dp)
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
        color = MaterialTheme.colorScheme.surface,
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
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
    )
}
