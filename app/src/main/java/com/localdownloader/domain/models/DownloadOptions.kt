package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

/**
 * User-selected download behavior mapped to yt-dlp arguments.
 */
@Serializable
data class DownloadOptions(
    val url: String,
    val formatId: String,
    val outputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val extractorArgs: String? = null,
    val fallbackExtractorArgs: String? = null,
    val youtubeAuthEnabled: Boolean = false,
    val youtubeCookiesPath: String? = null,
    val youtubePoToken: String? = null,
    val youtubePoTokenClientHint: String = "web.gvs",
    val mergeOutputFormat: String? = null,
    val isPlaylistEnabled: Boolean = false,
    val shouldDownloadSubtitles: Boolean = false,
    val shouldEmbedMetadata: Boolean = true,
    val shouldEmbedThumbnail: Boolean = false,
    val shouldWriteThumbnail: Boolean = false,
    val extractAudio: Boolean = false,
    val audioFormat: String? = null,
    val audioBitrateKbps: Int? = null,
    val playlistItemIndex: Int? = null,
    val playlistFolderName: String? = null,
)

/**
 * FFmpeg conversion operation on a local media file.
 */
data class ConversionRequest(
    val inputFilePath: String,
    val outputFilePath: String,
    val outputFormat: String,
    val audioBitrateKbps: Int? = null,
    val videoBitrateKbps: Int? = null,
)

/**
 * FFmpeg compression operation for file-size reduction.
 */
data class CompressionRequest(
    val inputFilePath: String,
    val outputFilePath: String,
    val targetVideoBitrateKbps: Int?,
    val targetAudioBitrateKbps: Int?,
    val maxHeight: Int?,
)
