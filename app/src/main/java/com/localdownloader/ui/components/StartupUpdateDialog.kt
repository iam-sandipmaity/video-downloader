package com.localdownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.updates.StartupUpdatePrompt

@Composable
fun StartupUpdateDialog(
    prompt: StartupUpdatePrompt,
    onOpenUpdates: () -> Unit,
    onDismiss: (snoozeVersion: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { onDismiss(false) },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.updates_startup_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.updates_startup_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                prompt.appUpdate?.takeIf { it.updateAvailable }?.let { update ->
                    StartupUpdateItemCard(
                        icon = Icons.Outlined.Smartphone,
                        title = stringResource(R.string.updates_startup_app_title),
                        currentVersion = update.currentVersion,
                        latestVersion = update.latestVersion,
                        summary = update.summary,
                    )
                }

                prompt.ytDlpUpdate?.takeIf { it.updateAvailable }?.let { update ->
                    StartupUpdateItemCard(
                        icon = Icons.Outlined.Code,
                        title = stringResource(R.string.updates_startup_ytdlp_title),
                        currentVersion = update.currentVersion,
                        latestVersion = update.latestVersion,
                        summary = update.summary,
                    )
                }

                prompt.ffmpegUpdate?.takeIf { it.updateAvailable }?.let { update ->
                    StartupUpdateItemCard(
                        icon = Icons.Outlined.Movie,
                        title = stringResource(R.string.updates_startup_ffmpeg_title),
                        currentVersion = update.currentVersion,
                        latestVersion = update.latestVersion,
                        summary = update.summary,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenUpdates) {
                Text(text = stringResource(R.string.updates_startup_open_updates))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onDismiss(true) }) {
                    Text(
                        text = stringResource(R.string.updates_startup_skip_version),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onDismiss(false) }) {
                    Text(text = stringResource(R.string.updates_startup_remind_later))
                }
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
    )
}

@Composable
private fun StartupUpdateItemCard(
    icon: ImageVector,
    title: String,
    currentVersion: String?,
    latestVersion: String?,
    summary: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!latestVersion.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "v$latestVersion".removePrefix("vv"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                val versionTransition = if (!currentVersion.isNullOrBlank() && !latestVersion.isNullOrBlank()) {
                    "$currentVersion → $latestVersion"
                } else {
                    summary
                }

                Text(
                    text = versionTransition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
