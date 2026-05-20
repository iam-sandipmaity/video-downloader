package com.localdownloader.ffmpeg

import com.localdownloader.domain.models.ConversionRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_INPUT_FILE_SIZE = 4L * 1024 * 1024 * 1024 // 4GB safety limit

/**
 * Maps file extension to its expected content type.
 */
enum class MediaContentType { VIDEO, AUDIO, UNKNOWN }

fun String.guessContentType(): MediaContentType {
    val ext = substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus" -> MediaContentType.AUDIO
        "mp4", "mkv", "mov", "avi", "webm", "flv", "wmv" -> MediaContentType.VIDEO
        else -> MediaContentType.UNKNOWN
    }
}

/**
 * Video-friendly output formats (limited to what the bundled FFmpeg supports).
 * The bundled FFmpeg doesn't have VP8/VP9 encoders, so webm can't produce video.
 */
val VIDEO_OUTPUT_FORMATS = listOf("mp4", "mkv", "avi", "flv", "mov")

/**
 * Audio-friendly output formats.
 */
val AUDIO_OUTPUT_FORMATS = listOf("mp3", "m4a", "aac", "wav", "opus", "flac", "ogg")

/**
 * Purpose-driven presets for conversion.
 */
data class ConversionPreset(
    val label: String,
    val description: String,
    val format: String,
    val videoBitrateKbps: Int? = null,
    val audioBitrateKbps: Int? = null,
)

val CONVERSION_PRESETS = listOf(
    // Video presets
    ConversionPreset(
        label = "High quality video",
        description = "MP4, good for archiving",
        format = "mp4",
        videoBitrateKbps = 2500,
        audioBitrateKbps = 192,
    ),
    ConversionPreset(
        label = "Best compatibility",
        description = "Standard quality MP4, works everywhere",
        format = "mp4",
        videoBitrateKbps = 1000,
        audioBitrateKbps = 128,
    ),
    ConversionPreset(
        label = "Small file",
        description = "MPEG-4, best for messaging / sharing",
        format = "mp4",
        videoBitrateKbps = 500,
        audioBitrateKbps = 96,
    ),
    // Audio-only presets
    ConversionPreset(
        label = "Audio (MP3)",
        description = "Common audio format, good compatibility",
        format = "mp3",
        videoBitrateKbps = null,
        audioBitrateKbps = 192,
    ),
    ConversionPreset(
        label = "Lossless audio (FLAC)",
        description = "No quality loss, larger file size",
        format = "flac",
        videoBitrateKbps = null,
        audioBitrateKbps = null,
    ),
    ConversionPreset(
        label = "Audio (Opus)",
        description = "Modern efficient audio codec",
        format = "opus",
        videoBitrateKbps = null,
        audioBitrateKbps = 128,
    ),
)

@Singleton
class FormatConverter @Inject constructor(
    private val ffmpegExecutor: FfmpegExecutor,
) {
    suspend fun convert(
        request: ConversionRequest,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<String> {
        val inputFile = File(request.inputFilePath)
        if (!inputFile.exists()) {
            return Result.failure(IllegalArgumentException("Source file does not exist: ${request.inputFilePath}"))
        }
        if (inputFile.length() > MAX_INPUT_FILE_SIZE) {
            return Result.failure(IllegalArgumentException("Input file is too large (>${MAX_INPUT_FILE_SIZE / (1024 * 1024 * 1024)}GB). This may cause performance issues."))
        }
        if (inputFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("Source file is empty"))
        }

        val outputExt = request.outputFilePath.substringAfterLast('.', "").lowercase().ifBlank { "mp4" }
        val isAudioOnlyOut = outputExt in AUDIO_OUTPUT_FORMATS
        val sourceType = request.inputFilePath.guessContentType()

        if (sourceType == MediaContentType.AUDIO && !isAudioOnlyOut) {
            return Result.failure(
                IllegalArgumentException(
                    "Audio-only files can only be converted to audio formats right now. Choose ${AUDIO_OUTPUT_FORMATS.joinToString(", ") { it.uppercase() }}.",
                ),
            )
        }
        if (!isAudioOnlyOut && outputExt !in VIDEO_OUTPUT_FORMATS) {
            val supported = (VIDEO_OUTPUT_FORMATS + AUDIO_OUTPUT_FORMATS).joinToString(", ")
            return Result.failure(
                IllegalArgumentException(
                    "Unsupported output format: .$outputExt. Supported: $supported",
                ),
            )
        }

        val outputDir = File(request.outputFilePath).parentFile
        if (outputDir != null && !outputDir.exists() && !outputDir.mkdirs()) {
            return Result.failure(IllegalStateException("Cannot create output directory: ${outputDir.absolutePath}"))
        }

        val args = mutableListOf("-i", request.inputFilePath)

        if (isAudioOnlyOut) {
            // Strip video, keep only audio.
            args += listOf("-vn")
            args += audioCodecArgsForOutput(outputExt)
        } else {
            // Video output: use mpeg4 encoder (bundled FFmpeg doesn't have libx264).
            args += listOf("-c:v", "mpeg4")
            args += videoAudioCodecArgsForContainer(outputExt)
            request.videoBitrateKbps?.let { args += listOf("-b:v", "${it}k") }
        }

        request.audioBitrateKbps?.let { args += listOf("-b:a", "${it}k") }

        args += listOf("-y", request.outputFilePath)

        val parser = FfmpegProgressParser
        var lastProgress = 0f
        val result = ffmpegExecutor.execute(
            args = args,
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
            val outputFile = File(request.outputFilePath)
            if (outputFile.exists() && outputFile.length() > 0) {
                Result.success(request.outputFilePath)
            } else {
                Result.failure(IllegalStateException("Conversion completed but output file was not found or empty"))
            }
        } else {
            Result.failure(
                IllegalStateException(
                    summarizeMediaToolFailure(
                        rawError = result.stderr,
                        fallbackMessage = "FFmpeg conversion failed",
                    ),
                ),
            )
        }
    }
}
