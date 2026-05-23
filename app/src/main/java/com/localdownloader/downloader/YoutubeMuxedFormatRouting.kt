package com.localdownloader.downloader

import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType

data class YoutubeFormatRouting(
    val selector: String,
    val queueNote: String? = null,
)

internal fun resolveYoutubeFormatRouting(
    sourceUrl: String,
    streamType: StreamType,
    selectedChoice: FormatChoice?,
    requestedContainer: String,
    fallbackSelector: String,
    hasMergedVideoAudioChoice: Boolean,
): YoutubeFormatRouting {
    if (!isYoutubeUrl(sourceUrl)) {
        return YoutubeFormatRouting(selector = fallbackSelector)
    }
    if (streamType != StreamType.VIDEO_AUDIO) {
        return YoutubeFormatRouting(selector = fallbackSelector)
    }
    val choice = selectedChoice ?: return YoutubeFormatRouting(selector = fallbackSelector)
    if (choice.isMerged || choice.selector.contains("+")) {
        return YoutubeFormatRouting(selector = fallbackSelector)
    }
    if (!hasMergedVideoAudioChoice) {
        return YoutubeFormatRouting(selector = fallbackSelector)
    }

    return YoutubeFormatRouting(
        selector = buildAdaptiveVideoAudioSelector(
            preferredHeight = choice.height,
            container = requestedContainer.ifBlank { choice.container },
            preferredVideoCodec = choice.videoCodec,
        ),
        queueNote = "YouTube muxed video was routed through a safer adaptive merge path to avoid broken duration and postprocessing issues.",
    )
}

private fun buildAdaptiveVideoAudioSelector(
    preferredHeight: Int?,
    container: String,
    preferredVideoCodec: String?,
): String {
    val heightFilter = preferredHeight?.let { "[height<=$it]" }.orEmpty()
    val normalizedContainer = container.trim().lowercase()
    val videoCodecFilter = preferredVideoCodecFilter(preferredVideoCodec)
    val videoExtensionFilter = when (normalizedContainer) {
        "mp4", "mov", "m4v" -> "[ext=mp4]"
        "webm" -> "[ext=webm]"
        else -> ""
    }
    val audioExtensionFilter = when (normalizedContainer) {
        "mp4", "mov", "m4v" -> "[ext=m4a]"
        "webm" -> "[ext=webm]"
        else -> ""
    }
    return "bestvideo$heightFilter$videoExtensionFilter$videoCodecFilter+bestaudio$audioExtensionFilter/" +
        "bestvideo$heightFilter$videoExtensionFilter+bestaudio$audioExtensionFilter/" +
        "bestvideo$heightFilter$videoCodecFilter+bestaudio/" +
        "bestvideo$heightFilter+bestaudio/" +
        "best$heightFilter/best"
}

private fun preferredVideoCodecFilter(codec: String?): String {
    val normalized = codec?.trim()?.lowercase().orEmpty()
    return when {
        normalized.startsWith("avc1") -> "[vcodec^=avc1]"
        normalized.startsWith("av01") || normalized == "av1" -> "[vcodec^=av01]"
        normalized.startsWith("vp9") -> "[vcodec^=vp9]"
        else -> ""
    }
}

private fun isYoutubeUrl(url: String): Boolean {
    val normalized = url.lowercase()
    return normalized.contains("youtube.com") || normalized.contains("youtu.be")
}
