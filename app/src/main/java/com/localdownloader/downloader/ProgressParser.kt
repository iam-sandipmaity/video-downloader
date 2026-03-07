package com.localdownloader.downloader

/**
 * Normalized progress output from yt-dlp.
 */
data class DownloadProgressSnapshot(
    val percent: Int?,
    val speed: String?,
    val eta: String?,
    val downloadedStr: String?,
    val totalStr: String?,
)

object ProgressParser {
    private val ytDlpRegex = Regex("""\[download\]\s+([\d.]+)%.*?at\s+([^\s]+).*?ETA\s+([^\s]+)""")
    // "PROG|" is a custom marker injected into the --progress-template output so we can detect
    // progress lines reliably. The "download:" part in the template arg is a channel selector
    // that yt-dlp does NOT print — only the template body after the colon is printed.
    private const val TEMPLATE_MARKER = "PROG|"

    fun parse(line: String): DownloadProgressSnapshot? {
        val trimmed = line.trimStart()
        if (trimmed.startsWith(TEMPLATE_MARKER)) {
            val parts = trimmed.removePrefix(TEMPLATE_MARKER).split("|")
            if (parts.isEmpty()) return null
            val percent = parts[0].replace("%", "").trim().toFloatOrNull()?.toInt()
            val speed = parts.getOrNull(1)?.trim()?.sanitizeUnknown()
            val eta = parts.getOrNull(2)?.trim()?.sanitizeUnknown()
            val downloaded = parts.getOrNull(3)?.trim()?.sanitizeSize()
            val total = parts.getOrNull(4)?.trim()?.sanitizeSize()
            return DownloadProgressSnapshot(
                percent = percent,
                speed = speed,
                eta = eta,
                downloadedStr = downloaded,
                totalStr = total,
            )
        }

        val match = ytDlpRegex.find(line) ?: return null
        return DownloadProgressSnapshot(
            percent = match.groupValues[1].toFloatOrNull()?.toInt(),
            speed = match.groupValues[2].sanitizeUnknown(),
            eta = match.groupValues[3].sanitizeUnknown(),
            downloadedStr = null,
            totalStr = null,
        )
    }

    private fun String.sanitizeUnknown(): String? =
        ifBlank { null }?.takeIf { it != "N/A" && it != "Unknown speed" && it != "Unknown ETA" }

    private fun String.sanitizeSize(): String? =
        ifBlank { null }?.takeIf { it != "N/A" && !startsWith("~N") }
}
