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
        ),
        queueNote = "YouTube muxed video was routed through a safer adaptive merge path to avoid broken duration and postprocessing issues.",
    )
}

private fun buildAdaptiveVideoAudioSelector(
    preferredHeight: Int?,
    container: String,
): String {
    val heightFilter = preferredHeight?.let { "[height<=$it]" }.orEmpty()
    val normalizedContainer = container.trim().lowercase()
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
    return "bestvideo$heightFilter$videoExtensionFilter+bestaudio$audioExtensionFilter/bestvideo$heightFilter+bestaudio/best$heightFilter/best"
}

private fun isYoutubeUrl(url: String): Boolean {
    val normalized = url.lowercase()
    return normalized.contains("youtube.com") || normalized.contains("youtu.be")
}
