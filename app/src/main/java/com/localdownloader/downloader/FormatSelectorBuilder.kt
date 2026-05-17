package com.localdownloader.downloader

import com.localdownloader.domain.models.MediaFormat

/**
 * Builds resilient yt-dlp format selectors so downloads can recover on sites
 * where exact format ids are flaky or partially unavailable.
 */
object FormatSelectorBuilder {
    fun buildAudioOnlySelector(audio: MediaFormat): String {
        return joinAlternatives(
            listOfNotNull(
                audio.formatId.takeIf { it.isNotBlank() },
                bestAudioForExtension(audio.normalizedExtension),
                "ba",
                "bestaudio",
            ),
        )
    }

    fun buildVideoOnlySelector(video: MediaFormat): String {
        return joinAlternatives(
            listOfNotNull(
                video.formatId.takeIf { it.isNotBlank() },
                "bv",
                "bestvideo",
            ),
        )
    }

    fun buildMuxedSelector(format: MediaFormat): String {
        return joinAlternatives(
            listOfNotNull(
                format.formatId.takeIf { it.isNotBlank() },
                bestByContainer(format.normalizedExtension),
                "b",
                "best",
            ),
        )
    }

    fun buildMergedSelector(video: MediaFormat, audio: MediaFormat): String {
        val segments = mutableListOf<String>()
        addSegment(segments, "${video.formatId}+${audio.formatId}")
        addSegment(segments, bestAudioForExtension(audio.normalizedExtension)?.let { "${video.formatId}+$it" })
        addSegment(segments, "${video.formatId}+ba")
        addSegment(segments, "${video.formatId}+bestaudio")
        addSegment(segments, video.formatId)
        addSegment(segments, bestByContainer(video.normalizedExtension))
        addSegment(segments, "b")
        addSegment(segments, "best")
        return joinAlternatives(segments)
    }

    private fun bestAudioForExtension(extension: String): String? {
        val normalized = extension.trim().lowercase()
        if (normalized.isBlank() || normalized == "bin") return null
        val safe = normalized.filter { it.isLetterOrDigit() }
        if (safe.isBlank()) return null
        return "ba[ext=$safe]"
    }

    private fun bestByContainer(container: String): String? {
        val normalized = container.trim().lowercase()
        if (normalized.isBlank() || normalized == "bin") return null
        val safe = normalized.filter { it.isLetterOrDigit() }
        if (safe.isBlank()) return null
        return "b[ext=$safe]"
    }

    private fun addSegment(segments: MutableList<String>, value: String?) {
        val normalized = value?.trim().orEmpty()
        if (normalized.isNotEmpty()) {
            segments += normalized
        }
    }

    private fun joinAlternatives(segments: List<String>): String {
        return segments
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .joinToString("/")
    }
}
