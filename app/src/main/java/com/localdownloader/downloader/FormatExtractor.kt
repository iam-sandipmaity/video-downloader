package com.localdownloader.downloader

import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.utils.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormatExtractor @Inject constructor(
    private val ytDlpExecutor: YtDlpExecutor,
    private val json: Json,
    private val logger: Logger,
) {
    suspend fun analyze(url: String): Result<VideoInfo> {
        return runCatching {
            logger.i("FormatExtractor", "Starting yt-dlp analyze for URL: $url")

            val isYoutube = isYoutubeUrl(url)
            val extractorCandidates = listOf<String?>(null)

            var best: AnalyzeCandidate? = null
            extractorCandidates.forEachIndexed { index, extractorArgs ->
                if (best != null && !shouldTryMoreCandidates(best?.stats)) return@forEachIndexed
                val candidate = analyzeWithExtractor(url = url, extractorArgs = extractorArgs)
                if (candidate != null) {
                    val stats = candidate.stats
                    val descriptor = extractorArgs ?: "(default)"
                    logger.i(
                        "FormatExtractor",
                        "Analyze candidate[$index] args=$descriptor formats=${stats.total} videoOnly=${stats.videoOnly} audioOnly=${stats.audioOnly} maxHeight=${stats.maxHeight}",
                    )
                    if (best == null || stats.isBetterThan(best?.stats)) {
                        best = candidate
                    }
                    if (stats.hasAdaptiveVideo) return@forEachIndexed
                }
            }

            val resolved = best ?: throw IllegalStateException("yt-dlp analyze failed")
            mapVideoInfo(root = resolved.root, fallbackUrl = url, extractorArgs = resolved.extractorArgs).also { info ->
                logger.i(
                    "FormatExtractor",
                    "Analyze parsed successfully title='${info.title}', formats=${info.formats.size}, extractorArgs=${resolved.extractorArgs}",
                )
                logFormatSummary(url = url, formats = info.formats, extractorArgs = resolved.extractorArgs)
            }
        }.onFailure { error ->
            logger.e("FormatExtractor", "Analyze exception for URL: $url", error)
        }
    }

    private fun logFormatSummary(url: String, formats: List<MediaFormat>, extractorArgs: String?) {
        if (!isYoutubeUrl(url)) return
        val videoOnly = formats.filter { it.isVideoOnly }
        val audioOnly = formats.filter { it.isAudioOnly }
        val muxed = formats.filter { !it.isVideoOnly && !it.isAudioOnly }
        val maxHeight = formats.mapNotNull { parseHeight(it.resolution) }.maxOrNull() ?: 0
        logger.i(
            "FormatExtractor",
            "YouTube formats summary extractorArgs=${extractorArgs ?: "(none)"} total=${formats.size} videoOnly=${videoOnly.size} audioOnly=${audioOnly.size} muxed=${muxed.size} maxHeight=${maxHeight}",
        )
        formats.take(12).forEachIndexed { index, format ->
            logger.i(
                "FormatExtractor",
                "Format[$index] id=${format.formatId} ext=${format.extension} res=${format.resolution} vcodec=${format.videoCodec} acodec=${format.audioCodec} fps=${format.fps ?: "-"} tbr=${format.bitrateKbps ?: "-"}",
            )
        }
    }

    private fun mapVideoInfo(root: JsonObject, fallbackUrl: String, extractorArgs: String?): VideoInfo {
        val formats = parseFormats(root["formats"] as? JsonArray ?: JsonArray(emptyList()))
        val type = root["\u005ftype"]?.jsonPrimitive?.contentOrNull
        return VideoInfo(
            id = root["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            title = root["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled",
            uploader = root["uploader"]?.jsonPrimitive?.contentOrNull,
            durationSeconds = root["duration"]?.jsonPrimitive?.longOrNull,
            thumbnailUrl = root["thumbnail"]?.jsonPrimitive?.contentOrNull,
            webpageUrl = root["webpage_url"]?.jsonPrimitive?.contentOrNull ?: fallbackUrl,
            formats = formats,
            extractorArgs = extractorArgs,
            isPlaylist = type == "playlist",
            playlistCount = root["playlist_count"]?.jsonPrimitive?.intOrNull,
        )
    }

    private suspend fun analyzeWithExtractor(url: String, extractorArgs: String?): AnalyzeCandidate? {
        val args = buildList {
            add("-J")
            add("--skip-download")
            add("--no-warnings")
            add("--ignore-config")
            if (!extractorArgs.isNullOrBlank()) {
                add("--extractor-args")
                add(extractorArgs)
            }
            add(url)
        }

        val result = ytDlpExecutor.execute(args = args)
        logger.i(
            "FormatExtractor",
            "Analyze command finished exitCode=${result.exitCode}, stdoutLen=${result.stdout.length}, stderrLen=${result.stderr.length}",
        )
        if (!result.isSuccess) {
            logger.w("FormatExtractor", "Analyze failed stderr=${result.stderr.take(1000)}")
            return null
        }

        val root = json.parseToJsonElement(result.stdout).jsonObject
        val formats = parseFormats(root["formats"] as? JsonArray ?: JsonArray(emptyList()))
        return AnalyzeCandidate(
            root = root,
            formats = formats,
            extractorArgs = extractorArgs,
            stats = FormatStats.from(formats),
        )
    }

    private fun parseFormats(elements: JsonArray): List<MediaFormat> {
        return elements.mapNotNull { element ->
            parseFormatObject(element)
        }.sortedWith(
            compareByDescending<MediaFormat> { it.resolution?.substringBefore("p")?.toIntOrNull() ?: 0 }
                .thenByDescending { it.bitrateKbps ?: 0 },
        )
    }

    private fun parseFormatObject(element: JsonElement): MediaFormat? {
        val item = element.jsonObject
        val formatId = item["format_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val ext = item["ext"]?.jsonPrimitive?.contentOrNull ?: "bin"
        val height = item["height"]?.jsonPrimitive?.intOrNull
        val width = item["width"]?.jsonPrimitive?.intOrNull
        val resolutionText = item["resolution"]?.jsonPrimitive?.contentOrNull
        val resolution = when {
            height != null -> "${height}p"
            resolutionText != null -> resolutionText
            width != null -> "${width}w"
            else -> null
        }

        val approximateSize = item["filesize"]?.jsonPrimitive?.longOrNull
            ?: item["filesize_approx"]?.jsonPrimitive?.longOrNull

        return MediaFormat(
            formatId = formatId,
            extension = ext,
            container = ext,
            resolution = resolution,
            videoCodec = item["vcodec"]?.jsonPrimitive?.contentOrNull ?: "none",
            audioCodec = item["acodec"]?.jsonPrimitive?.contentOrNull ?: "none",
            fileSizeBytes = approximateSize,
            bitrateKbps = item["tbr"]?.jsonPrimitive?.doubleOrNull?.toInt(),
            fps = item["fps"]?.jsonPrimitive?.doubleOrNull,
            note = item["format_note"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseHeight(resolution: String?): Int? {
        val trimmed = resolution ?: return null
        return trimmed.substringBefore("p", trimmed).toIntOrNull()
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val normalized = url.lowercase()
        return normalized.contains("youtube.com") || normalized.contains("youtu.be")
    }

    private data class AnalyzeCandidate(
        val root: JsonObject,
        val formats: List<MediaFormat>,
        val extractorArgs: String?,
        val stats: FormatStats,
    )

    private data class FormatStats(
        val total: Int,
        val videoOnly: Int,
        val audioOnly: Int,
        val maxHeight: Int,
        val hasAdaptiveVideo: Boolean,
    ) {
        fun isBetterThan(other: FormatStats?): Boolean {
            if (other == null) return true
            if (total != other.total) return total > other.total
            if (videoOnly != other.videoOnly) return videoOnly > other.videoOnly
            if (maxHeight != other.maxHeight) return maxHeight > other.maxHeight
            return audioOnly > other.audioOnly
        }

        companion object {
            fun from(formats: List<MediaFormat>): FormatStats {
                val videoOnly = formats.count { it.isVideoOnly }
                val audioOnly = formats.count { it.isAudioOnly }
                val maxHeight = formats.mapNotNull { parseHeight(it.resolution) }.maxOrNull() ?: 0
                val hasAdaptiveVideo = videoOnly > 0 && maxHeight >= 720
                return FormatStats(
                    total = formats.size,
                    videoOnly = videoOnly,
                    audioOnly = audioOnly,
                    maxHeight = maxHeight,
                    hasAdaptiveVideo = hasAdaptiveVideo,
                )
            }

            private fun parseHeight(resolution: String?): Int? {
                val trimmed = resolution ?: return null
                return trimmed.substringBefore("p", trimmed).toIntOrNull()
            }
        }
    }

    private fun shouldTryMoreCandidates(stats: FormatStats?): Boolean {
        if (stats == null) return true
        if (stats.hasAdaptiveVideo) return false
        if (stats.maxHeight >= 720 && stats.videoOnly > 0 && stats.audioOnly > 0) return false
        if (stats.total >= 20) return false
        return true
    }
}
