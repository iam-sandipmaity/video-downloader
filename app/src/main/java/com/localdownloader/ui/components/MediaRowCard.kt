package com.localdownloader.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class MediaRowChip(
    val text: String? = null,
    val icon: ImageVector? = null,
    val accent: Color? = null,
)

data class LocalMediaSnapshot(
    val sizeLabel: String? = null,
    val resolutionLabel: String? = null,
    val formatLabel: String? = null,
)

@Composable
fun rememberLocalMediaSnapshot(filePath: String?): LocalMediaSnapshot {
    return produceState(initialValue = LocalMediaSnapshot(), key1 = filePath) {
        value = withContext(Dispatchers.IO) {
            if (filePath.isNullOrBlank()) {
                LocalMediaSnapshot()
            } else {
                val file = File(filePath)
                val sizeLabel = file.takeIf { it.exists() }?.length()?.toReadableSize()
                val formatLabel = file.extension
                    .takeIf { it.isNotBlank() }
                    ?.uppercase()
                val resolutionLabel = runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(filePath)
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toIntOrNull()
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toIntOrNull()
                        if (width != null && height != null && width > 0 && height > 0) {
                            "${height}P (${width}x$height)"
                        } else {
                            null
                        }
                    } finally {
                        retriever.release()
                    }
                }.getOrNull()
                LocalMediaSnapshot(
                    sizeLabel = sizeLabel,
                    resolutionLabel = resolutionLabel,
                    formatLabel = formatLabel,
                )
            }
        }
    }.value
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MediaRowCard(
    title: String,
    chips: List<MediaRowChip>,
    thumbnail: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
                content = thumbnail,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    trailing?.invoke()
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.filter { it.icon != null || !it.text.isNullOrBlank() }
                        .forEach { chip ->
                            MediaRowChipView(chip = chip)
                        }
                }
                supportingContent?.invoke()
            }
        }
    }
}

@Composable
private fun MediaRowChipView(
    chip: MediaRowChip,
) {
    val accent = chip.accent ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (chip.text.isNullOrBlank()) 10.dp else 12.dp,
                vertical = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chip.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                )
            }
            chip.text?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun Long.toReadableSize(): String {
    if (this <= 0L) return ""
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    return when {
        this >= gib -> "${(this / gib * 10.0).roundToInt() / 10.0} GB"
        this >= mib -> "${(this / mib * 10.0).roundToInt() / 10.0} MB"
        this >= kib -> "${(this / kib * 10.0).roundToInt() / 10.0} KB"
        else -> "$this B"
    }
}
