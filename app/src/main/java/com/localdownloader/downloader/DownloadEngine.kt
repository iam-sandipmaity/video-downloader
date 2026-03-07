package com.localdownloader.downloader

import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.utils.Logger
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
            // Use the Android player client for YouTube to avoid po_token / 403 CDN errors.
            "--extractor-args", "youtube:player_client=android,web",
            // Retry on transient CDN 403s and expired DASH segment URLs.
            "--retries", "10",
            "--fragment-retries", "10",
            "--retry-sleep", "3",
            // Single-threaded fragment downloads prevent aggressive rate-limiting.
            "--concurrent-fragments", "1",
            "-f",
            options.formatId,
            "-o",
            outputTemplate,
        )

        if (options.isPlaylistEnabled) {
            args += "--yes-playlist"
        } else {
            args += "--no-playlist"
        }

        options.mergeOutputFormat?.let {
            args += listOf("--merge-output-format", it)
        }

        if (options.shouldDownloadSubtitles) {
            args += listOf("--write-subs", "--sub-langs", "all,-live_chat")
        }
        if (options.shouldEmbedMetadata) {
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
            options.audioBitrateKbps?.let { args += listOf("--audio-quality", "${it}K") }
        }

        args += options.url
        logger.i(
            "DownloadEngine",
            "Starting download URL=${options.url}, format=${options.formatId}, outputTemplate=$outputTemplate, extractAudio=${options.extractAudio}",
        )
        logger.d("DownloadEngine", "yt-dlp args: ${args.joinToString(" ")}")

        val result = ytDlpExecutor.execute(
            args = args,
            onStdoutLine = { line ->
                logger.d("DownloadEngine/stdout", line)
                onOutputLine(line)
                ProgressParser.parse(line)?.let(onProgress)
            },
            onStderrLine = { line ->
                logger.d("DownloadEngine/stderr", line)
                onOutputLine(line)
                ProgressParser.parse(line)?.let(onProgress)
            },
        )
        logger.i(
            "DownloadEngine",
            "Download command finished exitCode=${result.exitCode}, stderrLen=${result.stderr.length}",
        )
        return result
    }
}
