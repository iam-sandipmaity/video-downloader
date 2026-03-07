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
            val args = listOf(
                "-J",
                "--skip-download",
                "--no-warnings",
                url,
            )
            logger.i("FormatExtractor", "Starting yt-dlp analyze for URL: $url")
            val result = ytDlpExecutor.execute(args = args)
            logger.i(
                "FormatExtractor",
                "Analyze command finished exitCode=${result.exitCode}, stdoutLen=${result.stdout.length}, stderrLen=${result.stderr.length}",
            )
            if (!result.isSuccess) {
                logger.w("FormatExtractor", "Analyze failed stderr=${result.stderr.take(1000)}")
                throw IllegalStateException(result.stderr.ifBlank { "yt-dlp analyze failed" })
            }

            val root = json.parseToJsonElement(result.stdout).jsonObject
            mapVideoInfo(root = root, fallbackUrl = url).also { info ->
                logger.i(
                    "FormatExtractor",
                    "Analyze parsed successfully title='${info.title}', formats=${info.formats.size}",
                )
            }
        }.onFailure { error ->
            logger.e("FormatExtractor", "Analyze exception for URL: $url", error)
        }
    }

    private fun mapVideoInfo(root: JsonObject, fallbackUrl: String): VideoInfo {
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
            isPlaylist = type == "playlist",
            playlistCount = root["playlist_count"]?.jsonPrimitive?.intOrNull,
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
}
