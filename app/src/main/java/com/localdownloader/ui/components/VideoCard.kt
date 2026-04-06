package com.localdownloader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.localdownloader.domain.models.VideoInfo

@Composable
fun VideoCard(
    info: VideoInfo,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = info.thumbnailUrl,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = info.uploader ?: "Unknown uploader",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (info.isPlaylist) {
                        "Playlist items: ${info.playlistCount ?: info.playlistEntries.size}"
                    } else {
                        "Formats: ${info.formats.size}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
