package com.localdownloader.viewmodel

import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality

internal data class ResolvedDownloadFormat(
    val formatSelector: String,
    val mergeOutputFormat: String? = null,
)

internal object DownloadFormatResolver {
    fun resolve(
        videoInfo: VideoInfo,
        quality: VideoQuality,
        streamType: StreamType,
        container: String,
    ): ResolvedDownloadFormat {
        val normalizedContainer = container.lowercase()
        val fallback = ResolvedDownloadFormat(
            formatSelector = buildFallbackSelector(
                quality = quality,
                streamType = streamType,
                container = normalizedContainer,
            ),
            mergeOutputFormat = if (streamType == StreamType.VIDEO_AUDIO) {
                normalizedContainer.ifBlank { null }
            } else {
                null
            },
        )

        if (!videoInfo.webpageUrl.isYouTubeUrl()) {
            return fallback
        }

        val formats = videoInfo.formats
        return when (streamType) {
            StreamType.AUDIO_ONLY -> {
                val audioFormat = selectBestAudioFormat(formats, normalizedContainer)
                if (audioFormat != null) {
                    ResolvedDownloadFormat(formatSelector = audioFormat.formatId)
                } else {
                    fallback.copy(mergeOutputFormat = null)
                }
            }

            StreamType.VIDEO_ONLY -> {
                val videoFormat = selectPreferredVideoFormat(
                    formats = formats.filter { it.hasVideo },
                    quality = quality,
                    container = normalizedContainer,
                    preferMuxed = false,
                )
                if (videoFormat != null) {
                    ResolvedDownloadFormat(formatSelector = videoFormat.formatId)
                } else {
                    fallback.copy(mergeOutputFormat = null)
                }
            }

            StreamType.VIDEO_AUDIO -> {
                val muxedFormat = selectPreferredVideoFormat(
                    formats = formats.filter { it.hasVideo && it.hasAudio },
                    quality = quality,
                    container = normalizedContainer,
                    preferMuxed = true,
                )
                val splitVideoFormat = selectPreferredVideoFormat(
                    formats = formats.filter { it.isVideoOnly },
                    quality = quality,
                    container = normalizedContainer,
                    preferMuxed = false,
                )

                val chosenVideo = listOfNotNull(muxedFormat, splitVideoFormat)
                    .maxWithOrNull(compareVideoFormats(normalizedContainer, preferMuxed = true))
                    ?: return fallback

                if (chosenVideo.hasAudio) {
                    ResolvedDownloadFormat(formatSelector = chosenVideo.formatId)
                } else {
                    val audioFormat = selectBestAudioFormat(formats, normalizedContainer) ?: return fallback
                    ResolvedDownloadFormat(
                        formatSelector = "${chosenVideo.formatId}+${audioFormat.formatId}",
                        mergeOutputFormat = normalizedContainer.ifBlank { null },
                    )
                }
            }
        }
    }

    private fun selectPreferredVideoFormat(
        formats: List<MediaFormat>,
        quality: VideoQuality,
        container: String,
        preferMuxed: Boolean,
    ): MediaFormat? {
        val filtered = filterFormatsByQuality(formats, quality)
        return filtered.maxWithOrNull(compareVideoFormats(container, preferMuxed))
    }

    private fun selectBestAudioFormat(
        formats: List<MediaFormat>,
        container: String,
    ): MediaFormat? {
        return formats.filter { it.isAudioOnly }
            .maxWithOrNull(
                compareByDescending<MediaFormat> { if (prefersAudioContainer(it, container)) 1 else 0 }
                    .thenByDescending { it.bitrateKbps ?: 0 }
                    .thenByDescending { it.fileSizeBytes ?: 0L },
            )
    }

    private fun filterFormatsByQuality(
        formats: List<MediaFormat>,
        quality: VideoQuality,
    ): List<MediaFormat> {
        val maxHeight = quality.maxHeight ?: return formats
        val matchingHeight = formats.filter { format ->
            format.heightPixels?.let { it <= maxHeight } == true
        }
        if (matchingHeight.isNotEmpty()) return matchingHeight

        val unknownHeight = formats.filter { it.heightPixels == null }
        return unknownHeight
    }

    private fun compareVideoFormats(
        container: String,
        preferMuxed: Boolean,
    ): Comparator<MediaFormat> {
        return compareByDescending<MediaFormat> { it.heightPixels ?: -1 }
            .thenByDescending { if (prefersVideoContainer(it, container)) 1 else 0 }
            .thenByDescending { if (preferMuxed) if (it.hasAudio) 1 else 0 else if (it.isVideoOnly) 1 else 0 }
            .thenByDescending { it.bitrateKbps ?: 0 }
            .thenByDescending { it.fileSizeBytes ?: 0L }
    }

    private fun prefersVideoContainer(format: MediaFormat, container: String): Boolean {
        if (container.isBlank() || container == "mkv") return true
        return when (container) {
            "mp4", "mov" -> format.extension == "mp4"
            "webm" -> format.extension == "webm"
            else -> format.extension == container
        }
    }

    private fun prefersAudioContainer(format: MediaFormat, container: String): Boolean {
        if (container.isBlank() || container == "mkv") return true
        return when (container) {
            "mp4", "mov" -> format.extension == "m4a" || format.extension == "mp4"
            "webm" -> format.extension == "webm"
            else -> format.extension == container
        }
    }

    private fun buildFallbackSelector(
        quality: VideoQuality,
        streamType: StreamType,
        container: String,
    ): String {
        val heightFilter = quality.maxHeight?.let { "[height<=$it]" }.orEmpty()
        return when (streamType) {
            StreamType.AUDIO_ONLY -> "bestaudio/best"
            StreamType.VIDEO_ONLY -> "bestvideo$heightFilter/bestvideo"
            StreamType.VIDEO_AUDIO -> {
                val videoExtFilter = when (container) {
                    "mp4", "mov" -> "[ext=mp4]"
                    "webm" -> "[ext=webm]"
                    else -> ""
                }
                val audioExtFilter = when (container) {
                    "mp4", "mov" -> "[ext=m4a]"
                    "webm" -> "[ext=webm]"
                    else -> ""
                }
                "bestvideo$heightFilter$videoExtFilter+bestaudio$audioExtFilter/bestvideo$heightFilter+bestaudio/best$heightFilter/best"
            }
        }
    }

    private fun String.isYouTubeUrl(): Boolean {
        return contains("youtube.com", ignoreCase = true) || contains("youtu.be", ignoreCase = true)
    }
}
