package com.localdownloader.ffmpeg

/**
 * Parses FFmpeg stderr lines for duration and time progress.
 * Extracted to a shared utility so Compressor and FormatConverter don't duplicate parsing code.
 */
object FfmpegProgressParser {
    private val durationPattern = Regex("""Duration:\s*(\d+):(\d+):(\d+\.?\d*)""")
    private val timePattern = Regex("""time=\s*(\d+):(\d+):(\d+\.?\d*)""")

    fun parseDuration(line: String): Double? {
        val m = durationPattern.find(line) ?: return null
        val (h, min, s) = m.destructured
        return h.toDouble() * 3600 + min.toDouble() * 60 + s.toDouble()
    }

    fun parseTime(line: String): Double? {
        val m = timePattern.find(line) ?: return null
        val (h, min, s) = m.destructured
        return h.toDouble() * 3600 + min.toDouble() * 60 + s.toDouble()
    }
}
