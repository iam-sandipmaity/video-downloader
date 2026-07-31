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
        val args = mutableListOf(
            "--newline",
            "--ignore-config",
            "--no-warnings",
            "--progress-template",
            "download:PROG|%(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s|%(progress._downloaded_bytes_str)s|%(progress._total_bytes_estimate_str)s",
            // Fail fast enough for the worker's higher-level fallback logic to take over.
            "--retries", "6",
            "--fragment-retries", "4",
            "--retry-sleep", "1",
            "--retry-sleep", "fragment:1",
            "--abort-on-unavailable-fragments",
            // Single-threaded fragment downloads prevent aggressive rate-limiting.
            "--concurrent-fragments", "1",
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

        if ((options.shouldDownloadSubtitles || options.shouldEmbedSubtitles) && !options.extractAudio) {
            // Subtitles are always fetched as sidecars here. Embedding (when requested) is
            // performed as a separate, non-fatal post-step by the worker after the media
            // download succeeds, so a subtitle problem can never abort the whole download.
            args += subtitleArgs(
                url = options.url,
                preferredAudioLanguage = options.preferredAudioLanguage,
                embedSubtitles = false,
            )
        }

        if (shouldRequestMetadataEmbedding(options)) {
            args += "--embed-metadata"
        }
        if (options.shouldEmbedThumbnail) {
            args += "--embed-thumbnail"
        }
        if (options.shouldWriteThumbnail) {
            args += "--write-thumbnail"
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

        val args = mutableListOf(
            "--newline",
            "--ignore-config",
            "--no-warnings",
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

        args += subtitleArgs(
            url = options.url,
            preferredAudioLanguage = options.preferredAudioLanguage,
            embedSubtitles = false,
        )
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

    private fun subtitleArgs(
        url: String,
        preferredAudioLanguage: String? = null,
        embedSubtitles: Boolean,
    ): List<String> {
        return buildList {
            add("--write-subs")
            add("--write-auto-subs")
            add("--sub-langs")
            add(
                if (isYoutubeUrl(url)) {
                    preferredYoutubeSubtitleLanguages(preferredAudioLanguage = preferredAudioLanguage)
                } else {
                    "all,-live_chat"
                },
            )
            add("--convert-subs")
            add("srt")
            if (embedSubtitles) {
                add("--embed-subs")
            }
        }
    }

    private fun preferredYoutubeSubtitleLanguages(preferredAudioLanguage: String? = null): String {
        return buildYoutubeSubtitleLanguageList(
            preferredAudioLanguage = preferredAudioLanguage,
            locale = Locale.getDefault(),
        )
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val normalized = url.lowercase()
        return normalized.contains("youtube.com") || normalized.contains("youtu.be")
    }

    // Keep URL helpers local to FormatExtractor; download should honor analysis selection.
}

internal fun shouldPassMergeOutputFormat(options: DownloadOptions): Boolean {
    return !options.extractAudio && !options.mergeOutputFormat.isNullOrBlank()
}

internal fun shouldPassAudioQuality(options: DownloadOptions): Boolean {
    return options.extractAudio &&
        options.audioBitrateKbps != null &&
        audioFormatSupportsBitrateControl(options.audioFormat)
}

/**
 * Builds the YouTube `--sub-langs` candidate list in priority order. Exposed for tests so the
 * language-resolution logic can be verified independently of the device locale default.
 */
internal fun buildYoutubeSubtitleLanguageList(
    preferredAudioLanguage: String?,
    locale: java.util.Locale = java.util.Locale.getDefault(),
): String {
    val candidates = linkedSetOf<String>()
    preferredAudioLanguage
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
        ?.let(candidates::add)
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
    candidates += "all"
    return candidates.joinToString(",")
}