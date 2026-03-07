package com.localdownloader.ffmpeg

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioExtractor @Inject constructor(
    private val ffmpegExecutor: FfmpegExecutor,
) {
    suspend fun extractAudio(
        inputPath: String,
        outputPath: String,
        format: String,
        bitrateKbps: Int?,
    ): Result<String> {
        val codec = when (format.lowercase()) {
            "mp3" -> "libmp3lame"
            "aac", "m4a" -> "aac"
            "opus" -> "libopus"
            "wav" -> "pcm_s16le"
            else -> "copy"
        }

        val args = mutableListOf(
            "-i",
            inputPath,
            "-vn",
            "-c:a",
            codec,
        )
        bitrateKbps?.let { args += listOf("-b:a", "${it}k") }
        args += listOf("-y", outputPath)

        val result = ffmpegExecutor.execute(args)
        return if (result.isSuccess) {
            Result.success(outputPath)
        } else {
            Result.failure(IllegalStateException(result.stderr.ifBlank { "Audio extraction failed" }))
        }
    }
}
