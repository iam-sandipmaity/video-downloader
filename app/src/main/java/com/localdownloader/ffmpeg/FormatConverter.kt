package com.localdownloader.ffmpeg

import com.localdownloader.domain.models.ConversionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormatConverter @Inject constructor(
    private val ffmpegExecutor: FfmpegExecutor,
) {
    suspend fun convert(
        request: ConversionRequest,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<String> {
        val args = mutableListOf(
            "-i",
            request.inputFilePath,
        )

        request.videoBitrateKbps?.let { args += listOf("-b:v", "${it}k") }
        request.audioBitrateKbps?.let { args += listOf("-b:a", "${it}k") }

        // Allow ffmpeg to infer muxer from output extension.
        args += listOf("-y", request.outputFilePath)

        var totalSec = 0.0
        val result = ffmpegExecutor.execute(
            args = args,
            onStderrLine = { line ->
                if (totalSec == 0.0) parseFfmpegDuration(line)?.let { totalSec = it }
                if (totalSec > 0.0) {
                    parseFfmpegTime(line)?.let { cur ->
                        onProgress?.invoke((cur / totalSec).toFloat().coerceIn(0f, 1f))
                    }
                }
            },
        )
        return if (result.isSuccess) {
            Result.success(request.outputFilePath)
        } else {
            Result.failure(IllegalStateException(result.stderr.ifBlank { "FFmpeg conversion failed" }))
        }
    }
}

private val durationPattern = Regex("""Duration:\s*(\d+):(\d+):(\d+\.?\d*)""")
private val timePattern = Regex("""time=\s*(\d+):(\d+):(\d+\.?\d*)""")

private fun parseFfmpegDuration(line: String): Double? {
    val m = durationPattern.find(line) ?: return null
    val (h, min, s) = m.destructured
    return h.toDouble() * 3600 + min.toDouble() * 60 + s.toDouble()
}

private fun parseFfmpegTime(line: String): Double? {
    val m = timePattern.find(line) ?: return null
    val (h, min, s) = m.destructured
    return h.toDouble() * 3600 + min.toDouble() * 60 + s.toDouble()
}
