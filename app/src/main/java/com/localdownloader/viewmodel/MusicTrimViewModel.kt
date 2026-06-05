package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.DownloadTaskStore
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.ffmpeg.FfmpegExecutor
import com.localdownloader.ffmpeg.FfmpegProgressParser
import com.localdownloader.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MusicTrimViewModel @Inject constructor(
    private val ffmpegExecutor: FfmpegExecutor,
    private val downloadTaskStore: DownloadTaskStore,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicTrimUiState())
    val uiState: StateFlow<MusicTrimUiState> = _uiState.asStateFlow()

    fun trimAudio(
        sourceTaskId: String,
        sourceTitle: String,
        sourceUrl: String,
        sourcePath: String,
        startMs: Long,
        endMs: Long,
    ) {
        viewModelScope.launch {
            if (_uiState.value.isTrimming) return@launch

            _uiState.value = MusicTrimUiState(isTrimming = true)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val sourceFile = File(sourcePath)
                    require(sourceFile.exists()) { "Source audio file was not found." }
                    require(startMs >= 0L) { "Trim start must be at or after 00:00." }
                    require(endMs >= startMs + MIN_TRIM_DURATION_MS) {
                        "Choose at least ${MIN_TRIM_DURATION_MS / 1_000L} seconds to trim."
                    }

                    val outputFile = buildTrimOutputFile(sourceFile, startMs, endMs)
                    val trimDurationSeconds = (endMs - startMs) / 1_000.0
                    val args = listOf(
                        "-ss",
                        startMs.toFfmpegTimestamp(),
                        "-i",
                        sourceFile.absolutePath,
                        "-t",
                        (endMs - startMs).toFfmpegTimestamp(),
                        "-vn",
                        "-c:a",
                        "copy",
                        "-avoid_negative_ts",
                        "make_zero",
                        "-y",
                        outputFile.absolutePath,
                    )

                    val result = ffmpegExecutor.execute(
                        args = args,
                        onStderrLine = { line ->
                            FfmpegProgressParser.parseTime(line)?.let { currentSeconds ->
                                _uiState.update { state ->
                                    state.copy(
                                        progress = (currentSeconds / trimDurationSeconds).toFloat().coerceIn(0f, 1f),
                                    )
                                }
                            }
                        },
                    )

                    if (!result.isSuccess) {
                        error(result.stderr.ifBlank { "Unable to trim this audio." })
                    }
                    require(outputFile.exists() && outputFile.length() > 0L) {
                        "Trim completed but the output file was not created."
                    }

                    val now = System.currentTimeMillis()
                    val title = "${sourceTitle.ifBlank { sourceFile.nameWithoutExtension }} trim"
                    downloadTaskStore.upsert(
                        DownloadTask(
                            id = UUID.randomUUID().toString(),
                            url = sourceUrl,
                            title = title,
                            status = DownloadStatus.COMPLETED,
                            outputPath = outputFile.absolutePath,
                            downloadedStr = outputFile.length().toReadableSizeLabel(),
                            totalSizeStr = outputFile.length().toReadableSizeLabel(),
                            debugTrace = "Trimmed from task $sourceTaskId (${startMs.toShortTimestamp()}-${endMs.toShortTimestamp()})",
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now,
                        ),
                    )
                    outputFile.absolutePath
                }
            }

            result.fold(
                onSuccess = { outputPath ->
                    _uiState.value = MusicTrimUiState(
                        isTrimming = false,
                        progress = 1f,
                        message = "Trim saved: ${File(outputPath).name}",
                    )
                },
                onFailure = { error ->
                    logger.e("MusicTrimViewModel", "Audio trim failed", error)
                    _uiState.value = MusicTrimUiState(
                        isTrimming = false,
                        errorMessage = error.message ?: "Unable to trim this audio.",
                    )
                },
            )
        }
    }

    fun dismissResult() {
        _uiState.update { state ->
            state.copy(message = null, errorMessage = null)
        }
    }

    private fun buildTrimOutputFile(sourceFile: File, startMs: Long, endMs: Long): File {
        val parent = sourceFile.parentFile ?: error("Source audio folder is unavailable.")
        val extension = sourceFile.extension.ifBlank { "m4a" }
        val baseName = sourceFile.nameWithoutExtension.ifBlank { "audio" }
        val suffix = "trim-${startMs.toFileTimestamp()}-${endMs.toFileTimestamp()}"
        var candidate = File(parent, "$baseName-$suffix.$extension")
        var counter = 2
        while (candidate.exists()) {
            candidate = File(parent, "$baseName-$suffix-$counter.$extension")
            counter++
        }
        return candidate
    }

    private companion object {
        private const val MIN_TRIM_DURATION_MS = 1_000L
    }
}

data class MusicTrimUiState(
    val isTrimming: Boolean = false,
    val progress: Float = 0f,
    val message: String? = null,
    val errorMessage: String? = null,
)

private fun Long.toFfmpegTimestamp(): String {
    val totalMillis = coerceAtLeast(0L)
    val hours = totalMillis / 3_600_000L
    val minutes = (totalMillis % 3_600_000L) / 60_000L
    val seconds = (totalMillis % 60_000L) / 1_000L
    val millis = totalMillis % 1_000L
    return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
}

private fun Long.toShortTimestamp(): String {
    val totalSeconds = coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun Long.toFileTimestamp(): String {
    return toShortTimestamp().replace(":", "")
}

private fun Long.toReadableSizeLabel(): String {
    if (this <= 0L) return ""
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    return when {
        this >= gib -> "%.1f GB".format(this / gib)
        this >= mib -> "%.1f MB".format(this / mib)
        this >= kib -> "%.1f KB".format(this / kib)
        else -> "$this B"
    }
}
