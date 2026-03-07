package com.localdownloader.downloader

/**
 * Generic process execution result for yt-dlp and FFmpeg calls.
 */
data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val isSuccess: Boolean
        get() = exitCode == 0
}
