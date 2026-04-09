package com.localdownloader.ffmpeg

import com.localdownloader.domain.models.CompressionRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Compressor @Inject constructor(
    private val ffmpegExecutor: FfmpegExecutor,
) {
    suspend fun compress(
        request: CompressionRequest,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<String> {
        val inputFile = File(request.inputFilePath)
        if (!inputFile.exists()) {
            return Result.failure(IllegalArgumentException("Source file does not exist: ${request.inputFilePath}"))
        }

        if (request.targetVideoBitrateKbps != null && request.targetVideoBitrateKbps <= 0) {
            return Result.failure(IllegalArgumentException("Video bitrate must be positive"))
        }
        if (request.targetAudioBitrateKbps != null && request.targetAudioBitrateKbps <= 0) {
            return Result.failure(IllegalArgumentException("Audio bitrate must be positive"))
        }
        if (request.maxHeight != null && request.maxHeight <= 0) {
            return Result.failure(IllegalArgumentException("Max height must be positive"))
        }

        val args = mutableListOf(
            "-i",
            request.inputFilePath,
        )

        val inputExt = request.inputFilePath.substringAfterLast('.').lowercase()
        val outputExt = request.outputFilePath.substringAfterLast('.').lowercase()
        val isAudioOnlyInput = inputExt in listOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")

        if (!isAudioOnlyInput) {
            // Use mpeg4 video encoder (bundled FFmpeg doesn't have libx264).
            args += listOf("-c:v", "mpeg4")

            val bitrateSpec = request.targetVideoBitrateKbps != null
            val hasVideoFilter = request.maxHeight != null

            // If no explicit bitrate or filter is set, use a sane default.
            if (!bitrateSpec && !hasVideoFilter) {
                args += listOf("-b:v", "800k")
            } else {
                request.targetVideoBitrateKbps?.let { args += listOf("-b:v", "${it}k") }
                request.maxHeight?.let { maxHeight ->
                    args += listOf("-vf", "scale=-2:$maxHeight")
                }
            }
        } else {
            // Audio-only input: just re-encode audio at a lower bitrate.
            args += listOf("-vn")
        }

        request.targetAudioBitrateKbps?.let { args += listOf("-b:a", "${it}k") }

        // Determine output codec based on extension
        val finalArgs = if (isAudioOnlyInput) {
            args + listOf("-y", request.outputFilePath)
        } else {
            val audioCodec = when (outputExt) {
                "mp4", "mov", "mkv" -> listOf("-c:a", "aac", "-movflags", "+faststart")
                "avi", "flv" -> listOf("-c:a", "mp3")
                else -> listOf("-c:a", "aac")
            }
            args + audioCodec + listOf("-y", request.outputFilePath)
        }

        val parser = FfmpegProgressParser
        var lastProgress = 0f
        val result = ffmpegExecutor.execute(
            args = finalArgs,
            onStderrLine = { line ->
                val totalSec = parser.parseDuration(line)
                val cur = parser.parseTime(line)
                if (totalSec != null && cur != null) {
                    lastProgress = (cur / totalSec).toFloat().coerceIn(0f, 1f)
                    onProgress?.invoke(lastProgress)
                }
            },
        )
        // Ensure we report completion even if parser didn't catch it
        if (lastProgress < 1f && result.isSuccess) {
            onProgress?.invoke(1f)
        }
        return if (result.isSuccess) {
            // Ensure the output file actually exists.
            if (File(request.outputFilePath).exists()) {
                Result.success(request.outputFilePath)
            } else {
                Result.failure(IllegalStateException("Compression completed but output file was not found"))
            }
        } else {
            Result.failure(IllegalStateException(result.stderr.ifBlank { "FFmpeg compression failed" }))
        }
    }
}
