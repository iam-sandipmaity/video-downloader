package com.localdownloader.downloader

import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.audioFormatSupportsBitrateControl
import com.localdownloader.utils.Logger
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates high-level download options into yt-dlp CLI arguments.
 */
@Singleton
class DownloadEngine @Inject constructor(
    private val ytDlpExecutor: YtDlpExecutor,
    private val logger: Logger,
) {
    suspend fun runDownload(
        options: DownloadOptions,
        outputTemplate: String,
        onProgress: (DownloadProgressSnapshot) -> Unit,
        onOutputLine: (String) -> Unit,
    ): CommandResult {
        runCatching {
            File(outputTemplate).parentFile?.mkdirs()
        }

        val args = mutableListOf(
            "--newline",
            "--ignore-config",
            "--no-warnings",
            "--windows-filenames",
            "--trim-filenames", "160",
            "--progress-template",
            "download:PROG|%(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s|%(progress._downloaded_bytes_str)s|%(progress._total_bytes_estimate_str)s",
            // Fail fast enough for the worker's higher-level fallback logic to take over.
            "--retries", "6",
            "--fragment-retries", "4",
            "--retry-sleep", "1",
            "--retry-sleep", "fragment:1",
            "--abort-on-unavailable-fragments",
            // User-configured concurrent fragment download threads.
            "--concurrent-fragments", options.concurrentFragments.coerceIn(1, 16).toString(),
            "-f",
            options.formatId,
            "-o",
            outputTemplate,
        )

        if (options.forceFreshDownload) {
            args += "--no-continue"
        }
        if (!options.extractorArgs.isNullOrBlank()) {
            args += listOf("--extractor-args", options.extractorArgs)
        }
        if (
            !isYoutubeUrl(options.url) &&
            !options.loadInfoJsonPath.isNullOrBlank() &&
            File(options.loadInfoJsonPath).exists()
        ) {
            args += listOf("--load-info-json", options.loadInfoJsonPath)
        }
        if (!options.userAgentHeader.isNullOrBlank()) {
            args += listOf("--add-header", "User-Agent:${options.userAgentHeader}")
        }

        // Apply cookies if a valid cookies file path is available.
        // These are used for cookie-backed site sessions, including tougher YouTube cases.
        val cookiesPath = options.youtubeCookiesPath
        if (!cookiesPath.isNullOrBlank() && File(cookiesPath).exists()) {
            args += listOf("--cookies", cookiesPath)
        }

        if (options.isPlaylistEnabled) {
            args += "--yes-playlist"
        } else {
            args += "--no-playlist"
        }

        options.playlistItemIndex?.let { index ->
            args += listOf("--playlist-items", index.toString())
        }

        if (shouldPassMergeOutputFormat(options)) {
            args += listOf("--merge-output-format", requireNotNull(options.mergeOutputFormat))
        }

        if (options.shouldDownloadSubtitles || options.shouldEmbedSubtitles) {
            args += subtitleArgs(options)
        }

        if (shouldRequestMetadataEmbedding(options)) {
            args += "--embed-metadata"
        }
        if (shouldRequestThumbnailEmbedding(options)) {
            args += "--embed-thumbnail"
        }
        if (options.shouldWriteThumbnail || options.shouldEmbedThumbnail) {
            args += "--write-thumbnail"
            args += listOf("--convert-thumbnails", "png")
        }

        if (options.extractAudio) {
            args += "-x"
            options.audioFormat?.let { args += listOf("--audio-format", it) }
            if (shouldPassAudioQuality(options)) {
                options.audioBitrateKbps?.let { args += listOf("--audio-quality", "${it}K") }
            }
        }

        args += options.url
        logger.i(
            "DownloadEngine",
            "Starting download URL=${options.url}, format=${options.formatId}, outputTemplate=$outputTemplate, extractAudio=${options.extractAudio}",
        )
        logger.d("DownloadEngine", "yt-dlp args: ${args.joinToString(" ")}")

        val lastUpdateMs = java.util.concurrent.atomic.AtomicLong(0L)
        val minIntervalMs = 250L

        val result = ytDlpExecutor.execute(
            args = args,
            onStdoutLine = { line ->
                onOutputLine(line)
                ProgressParser.parse(line)?.let { progress ->
                    val now = System.currentTimeMillis()
                    val percent = progress.percent
                    val isEdgeState = percent == 0 || percent == 100
                    val last = lastUpdateMs.get()
                    if (isEdgeState || now - last >= minIntervalMs) {
                        lastUpdateMs.set(now)
                        onProgress(progress)
                    }
                }
            },
            onStderrLine = { line ->
                onOutputLine(line)
                ProgressParser.parse(line)?.let { progress ->
                    val now = System.currentTimeMillis()
                    val percent = progress.percent
                    val isEdgeState = percent == 0 || percent == 100
                    val last = lastUpdateMs.get()
                    if (isEdgeState || now - last >= minIntervalMs) {
                        lastUpdateMs.set(now)
                        onProgress(progress)
                    }
                }
            },
        )
        logger.i(
            "DownloadEngine",
            "Download command finished exitCode=${result.exitCode}, stderrLen=${result.stderr.length}",
        )
        return result
    }

    suspend fun runSubtitleDownload(
        options: DownloadOptions,
        outputTemplate: String,
        onOutputLine: (String) -> Unit,
    ): CommandResult {
        if (!options.shouldDownloadSubtitles) {
            return CommandResult(exitCode = 0, stdout = "", stderr = "")
        }

        runCatching {
            File(outputTemplate).parentFile?.mkdirs()
        }

        val args = mutableListOf(
            "--newline",
            "--ignore-config",
            "--no-warnings",
            "--windows-filenames",
            "--trim-filenames", "160",
            "--skip-download",
            "-o",
            outputTemplate,
        )

        val subtitleExtractorArgs = resolveSubtitleExtractorArgs(options)
        if (!subtitleExtractorArgs.isNullOrBlank()) {
            args += listOf("--extractor-args", subtitleExtractorArgs)
        }
        if (!options.loadInfoJsonPath.isNullOrBlank() && File(options.loadInfoJsonPath).exists()) {
            args += listOf("--load-info-json", options.loadInfoJsonPath)
        }
        if (!options.userAgentHeader.isNullOrBlank()) {
            args += listOf("--add-header", "User-Agent:${options.userAgentHeader}")
        }

        val cookiesPath = options.youtubeCookiesPath
        if (!cookiesPath.isNullOrBlank() && File(cookiesPath).exists()) {
            args += listOf("--cookies", cookiesPath)
        }

        if (options.isPlaylistEnabled) {
            args += "--yes-playlist"
        } else {
            args += "--no-playlist"
        }

        options.playlistItemIndex?.let { index ->
            args += listOf("--playlist-items", index.toString())
        }

        args += subtitleArgs(options.copy(shouldDownloadSubtitles = true, shouldEmbedSubtitles = false))
        args += options.url

        logger.i(
            "DownloadEngine",
            "Starting subtitle-only download URL=${options.url}, outputTemplate=$outputTemplate",
        )
        logger.d("DownloadEngine", "yt-dlp subtitle args: ${args.joinToString(" ")}")

        return ytDlpExecutor.execute(
            args = args,
            onStdoutLine = { line ->
                onOutputLine(line)
            },
            onStderrLine = { line ->
                onOutputLine(line)
            },
        ).also { result ->
            logger.i(
                "DownloadEngine",
                "Subtitle command finished exitCode=${result.exitCode}, stderrLen=${result.stderr.length}",
            )
        }
    }

    private fun resolveSubtitleExtractorArgs(options: DownloadOptions): String? {
        val configuredArgs = options.extractorArgs?.trim().orEmpty()
        if (isYoutubeUrl(options.url) &&
            (options.youtubePoToken.orEmpty().isNotBlank() || options.youtubeDataSyncId.orEmpty().isNotBlank()) &&
            !configuredArgs.contains("po_token=") &&
            !configuredArgs.contains("data_sync_id=")
        ) {
            return YoutubeRequestPlanner.preferredAuthenticatedExtractorArgs(
                poToken = options.youtubePoToken,
                preferredHint = options.youtubePoTokenClientHint,
                dataSyncId = options.youtubeDataSyncId,
            )
        }
        return configuredArgs.ifBlank { null }
    }

    private fun shouldRequestMetadataEmbedding(options: DownloadOptions): Boolean {
        if (!options.shouldEmbedMetadata) return false

        val normalizedContainer = options.mergeOutputFormat?.trim()?.lowercase().orEmpty()
        if (normalizedContainer == "mkv") {
            logger.i(
                "DownloadEngine",
                "Skipping yt-dlp metadata embedding for MKV output because the current runtime does not provide ffprobe",
            )
            return false
        }

        return true
    }

    private fun shouldRequestThumbnailEmbedding(options: DownloadOptions): Boolean {
        if (!options.shouldEmbedThumbnail) return false
        val normalizedContainer = options.mergeOutputFormat?.trim()?.lowercase().orEmpty()
        if (normalizedContainer == "webm") {
            logger.i(
                "DownloadEngine",
                "Skipping yt-dlp thumbnail embedding for WebM output because WebM container does not support embedded thumbnail tags",
            )
            return false
        }
        return true
    }

    internal fun subtitleArgs(options: DownloadOptions): List<String> = buildSubtitleArgs(options)

    private fun preferredYoutubeSubtitleLanguages(): String {
        val locale = Locale.getDefault()
        val candidates = linkedSetOf<String>()
        locale.toLanguageTag()
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
            ?.let(candidates::add)
        locale.language
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
            ?.let(candidates::add)
        candidates += "en"
        candidates += "en-orig"
        return candidates.joinToString(",")
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val normalized = url.lowercase()
        return normalized.contains("youtube.com") || normalized.contains("youtu.be")
    }

    // Keep URL helpers local to FormatExtractor; download should honor analysis selection.
}

internal fun shouldPassMergeOutputFormat(options: DownloadOptions): Boolean {
    return !options.extractAudio &&
        !options.downloadVideoOnly &&
        !options.removeAudioFromVideo &&
        isValidMergeContainer(options.mergeOutputFormat)
}

internal fun shouldPassAudioQuality(options: DownloadOptions): Boolean {
    return options.extractAudio &&
        options.audioBitrateKbps != null &&
        audioFormatSupportsBitrateControl(options.audioFormat)
}

internal fun buildSubtitleArgs(options: DownloadOptions): List<String> {
    val url = options.url
    val requestedLangs = options.subtitleLanguages.filter { it.isNotBlank() }
    val langs = if (requestedLangs.isNotEmpty()) {
        requestedLangs.joinToString(",")
    } else {
        "all,-live_chat"
    }

    return buildList {
        add("--no-abort-on-error")
        if (options.autoSubtitles || requestedLangs.isEmpty() || requestedLangs.any { it.contains("-orig") || it.contains("auto") }) {
            add("--write-auto-subs")
            if (!options.autoTranslatedSubtitles) {
                add("--extractor-args")
                add("youtube:skip=translated_subs")
            }
        }
        if (langs.isNotBlank()) {
            add("--sub-langs")
            add(langs)
        }
        if (options.shouldEmbedSubtitles && !options.extractAudio) {
            add("--embed-subs")
            if (options.keepSubtitleFiles || options.shouldDownloadSubtitles) {
                add("--write-subs")
            }
        } else {
            add("--write-subs")
        }
        val convertFmt = options.subtitleConvertFormat.trim().lowercase()
        if (convertFmt in listOf("srt", "ass", "vtt", "lrc")) {
            add("--convert-subs")
            add(convertFmt)
        }
    }
}

