package com.localdownloader.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

data class AudioTrackMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
)

fun readAudioTrackMetadata(
    context: Context,
    location: String,
): AudioTrackMetadata {
    if (location.isBlank()) return AudioTrackMetadata()
    val retriever = MediaMetadataRetriever()
    return runCatching {
        if (location.startsWith("content://", ignoreCase = true)) {
            retriever.setDataSource(context, Uri.parse(location))
        } else {
            retriever.setDataSource(location)
        }
        AudioTrackMetadata(
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).cleanAudioTag(),
            artist = listOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR),
            ).firstNotNullOfOrNull { it.cleanAudioTag() },
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).cleanAudioTag(),
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L },
        )
    }.getOrDefault(AudioTrackMetadata())
        .also {
            runCatching { retriever.release() }
        }
}

private fun String?.cleanAudioTag(): String? {
    val value = this?.trim().orEmpty()
    return value
        .takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("<unknown>", ignoreCase = true) }
        ?.takeUnless { it.equals("unknown", ignoreCase = true) }
}
