package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.ffmpeg.suggestedCompressionOutputExtension
import com.localdownloader.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Well-known compression height presets with human-friendly labels. */
data class ResolutionPreset(val label: String, val height: Int)

val RESOLUTION_PRESETS = listOf(
    ResolutionPreset("144p", 144),
    ResolutionPreset("240p", 240),
    ResolutionPreset("360p", 360),
    ResolutionPreset("480p", 480),
    ResolutionPreset("720p HD", 720),
    ResolutionPreset("1080p FHD", 1080),
    ResolutionPreset("1440p QHD", 1440),
    ResolutionPreset("2160p 4K", 2160),
)

/** Bitrate presets for video compression. */
data class VideoBitratePreset(val label: String, val kbps: Int?)

val VIDEO_BITRATE_PRESETS = listOf(
    VideoBitratePreset("Auto (default)", null),
    VideoBitratePreset("Small 500kbps", 500),
    VideoBitratePreset("Medium 1000kbps", 1000),
    VideoBitratePreset("High 2500kbps", 2500),
    VideoBitratePreset("Very High 5000kbps", 5000),
)

/** Audio bitrate presets for compression. */
data class AudioBitratePreset(val label: String, val kbps: Int?)

val AUDIO_BITRATE_PRESETS = listOf(
    AudioBitratePreset("Auto (default)", null),
    AudioBitratePreset("96kbps", 96),
    AudioBitratePreset("128kbps", 128),
    AudioBitratePreset("192kbps", 192),
    AudioBitratePreset("320kbps", 320),
)

/** Display info for the selected input file. */
data class InputFileInfo(
    val name: String,
    val sizeBytes: Long,
    val path: String,
)

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

data class MediaToolsUiState(
    // Convert
    val convertInputPath: String = "",
    val convertInputFileInfo: InputFileInfo? = null,
    val convertOutputFormat: String = "mp4",
    val convertAudioBitrate: String = "",
    val convertVideoBitrate: String = "",
    val convertPresetIndex: Int = 1, // "Best compatibility" by default
    val isConverting: Boolean = false,
    val isStoppingConvert: Boolean = false,
    val convertWasCanceled: Boolean = false,
    val convertProgress: Float? = null,
    val convertResult: String? = null,
    val convertResultSizeBytes: Long? = null,
    val convertSourceSizeBytes: Long? = null,
    val convertError: String? = null,

    // Compress
    val compressInputPath: String = "",
    val compressInputFileInfo: InputFileInfo? = null,
    val compressResolutionPresetIndex: Int = RESOLUTION_PRESETS.indexOfFirst { it.height == 720 }.coerceAtLeast(4),
    val compressMaxHeight: String = "720",
    val compressVideoBitratePresetIndex: Int = 2, // Medium 1000kbps
    val compressVideoBitrate: String = "1000",
    val compressAudioBitratePresetIndex: Int = 2, // 128kbps
    val compressAudioBitrate: String = "128",
    val isCompressing: Boolean = false,
    val isStoppingCompress: Boolean = false,
    val compressWasCanceled: Boolean = false,
    val compressProgress: Float? = null,
    val compressResult: String? = null,
    val compressResultSizeBytes: Long? = null,
    val compressSourceSizeBytes: Long? = null,
    val compressError: String? = null,
)

@HiltViewModel
class MediaToolsViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    val fileUtils: FileUtils,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaToolsUiState())
    val uiState: StateFlow<MediaToolsUiState> = _uiState.asStateFlow()
    private var convertJob: Job? = null
    private var compressJob: Job? = null

    // ── Helpers ──────────────────────────────────────────────────────

    private fun extractFileInfo(path: String): InputFileInfo? {
        val file = File(path.trim())
        return if (file.exists()) {
            InputFileInfo(
                name = file.name,
                sizeBytes = file.length(),
                path = file.absolutePath,
            )
        } else {
            null
        }
    }

    // ── Convert fields ────────────────────────────────────────────────

    fun onConvertInputPathChanged(v: String) {
        val fileInfo = extractFileInfo(v)
        _uiState.update {
            it.copy(
                convertInputPath = v,
                convertInputFileInfo = fileInfo,
                convertResult = null,
                convertError = null,
                convertWasCanceled = false,
                isStoppingConvert = false,
                convertResultSizeBytes = null,
                convertSourceSizeBytes = fileInfo?.sizeBytes,
            )
        }
    }

    fun onConvertOutputFormatChanged(v: String) = _uiState.update { it.copy(convertOutputFormat = v) }
    fun onConvertAudioBitrateChanged(v: String) = _uiState.update { it.copy(convertAudioBitrate = v) }
    fun onConvertVideoBitrateChanged(v: String) = _uiState.update { it.copy(convertVideoBitrate = v) }

    /** Convenience: apply a conversion preset in one call. */
    fun applyConversionPreset(presetIndex: Int) {
        val preset = com.localdownloader.ffmpeg.CONVERSION_PRESETS.getOrNull(presetIndex) ?: return
        _uiState.update {
            it.copy(
                convertPresetIndex = presetIndex,
                convertOutputFormat = preset.format,
                convertVideoBitrate = preset.videoBitrateKbps?.toString().orEmpty(),
                convertAudioBitrate = preset.audioBitrateKbps?.toString().orEmpty(),
            )
        }
    }

    fun startConvert() {
        val s = _uiState.value
        if (convertJob?.isActive == true) return
        if (s.convertInputPath.isBlank()) {
            _uiState.update { it.copy(convertError = "Input file path is required") }
            return
        }

        val downloadsDir = runCatching { fileUtils.ensureDownloadsDir() }
            .getOrElse {
                _uiState.update { it.copy(convertError = "Cannot access downloads directory") }
                return
            }

        val ext = s.convertOutputFormat.trim().lowercase()
        val baseName = s.convertInputPath.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .let { if ('.' in it) it.substringBeforeLast('.') else it }
        val outputPath = downloadsDir.absolutePath + "/" + fileUtils.sanitizeFileName(baseName) + "." + ext
        
        val request = ConversionRequest(
            inputFilePath = s.convertInputPath.trim(),
            outputFilePath = outputPath,
            outputFormat = ext,
            audioBitrateKbps = s.convertAudioBitrate.toIntOrNull(),
            videoBitrateKbps = s.convertVideoBitrate.toIntOrNull(),
        )
        val sourceSize = s.convertSourceSizeBytes ?: File(s.convertInputPath.trim()).takeIf { it.exists() }?.length()
        convertJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    isStoppingConvert = false,
                    convertWasCanceled = false,
                    convertResult = null,
                    convertError = null,
                    convertProgress = 0f,
                )
            }
            try {
                val result = repository.convertMedia(request) { progress ->
                    _uiState.update { state -> state.copy(convertProgress = progress) }
                }
                val resultPath = result.getOrNull()
                // Copy to public Downloads folder so user can find it
                val publicPath = resultPath?.let { path ->
                    runCatching { fileUtils.copyToPublicDownloads(File(path), null) }.getOrNull()
                }
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        isStoppingConvert = false,
                        convertWasCanceled = false,
                        convertProgress = null,
                        convertResult = resultPath?.let { path -> "Saved to: ${publicPath ?: path}" },
                        convertResultSizeBytes = resultPath?.let { path -> File(path).length() },
                        convertSourceSizeBytes = sourceSize,
                        convertError = result.exceptionOrNull()?.message,
                    )
                }
            } catch (_: CancellationException) {
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        isStoppingConvert = false,
                        convertWasCanceled = true,
                        convertProgress = null,
                        convertResult = null,
                        convertResultSizeBytes = null,
                        convertSourceSizeBytes = sourceSize,
                        convertError = "Conversion canceled by you.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        isStoppingConvert = false,
                        convertWasCanceled = false,
                        convertProgress = null,
                        convertResult = null,
                        convertResultSizeBytes = null,
                        convertSourceSizeBytes = sourceSize,
                        convertError = error.message ?: "Conversion failed.",
                    )
                }
            } finally {
                convertJob = null
            }
        }
    }

    fun stopConvert() {
        if (convertJob?.isActive != true) return
        _uiState.update { it.copy(isStoppingConvert = true) }
        convertJob?.cancel(CancellationException("Conversion canceled by user"))
    }

    fun dismissConvertResult() = _uiState.update {
        it.copy(
            convertResult = null,
            convertError = null,
            convertWasCanceled = false,
            convertResultSizeBytes = null,
        )
    }

    // ── Compress fields ───────────────────────────────────────────────

    fun onCompressInputPathChanged(v: String) {
        val fileInfo = extractFileInfo(v)
        _uiState.update {
            it.copy(
                compressInputPath = v,
                compressInputFileInfo = fileInfo,
                compressResult = null,
                compressError = null,
                compressWasCanceled = false,
                isStoppingCompress = false,
                compressResultSizeBytes = null,
                compressSourceSizeBytes = fileInfo?.sizeBytes,
            )
        }
    }

    /** Select a resolution preset by index and update maxHeight. */
    fun onCompressResolutionPresetSelected(index: Int) {
        val preset = RESOLUTION_PRESETS.getOrNull(index) ?: return
        _uiState.update {
            it.copy(compressResolutionPresetIndex = index, compressMaxHeight = preset.height.toString())
        }
    }

    /** Select a video bitrate preset by index and update the value. */
    fun onCompressVideoBitratePresetSelected(index: Int) {
        val preset = VIDEO_BITRATE_PRESETS.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                compressVideoBitratePresetIndex = index,
                compressVideoBitrate = preset.kbps?.toString().orEmpty(),
            )
        }
    }

    /** Select an audio bitrate preset by index and update the value. */
    fun onCompressAudioBitratePresetSelected(index: Int) {
        val preset = AUDIO_BITRATE_PRESETS.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                compressAudioBitratePresetIndex = index,
                compressAudioBitrate = preset.kbps?.toString().orEmpty(),
            )
        }
    }

    fun onCompressMaxHeightChanged(v: String) = _uiState.update { it.copy(compressMaxHeight = v) }
    fun onCompressVideoBitrateChanged(v: String) = _uiState.update { it.copy(compressVideoBitrate = v) }
    fun onCompressAudioBitrateChanged(v: String) = _uiState.update { it.copy(compressAudioBitrate = v) }

    /** Convenience: apply all-at-once legacy quick preset Small/Medium/High. */
    fun onCompressQuickPresetSelected(vbr: String, abr: String, height: String) {
        val resIndex = RESOLUTION_PRESETS.indexOfFirst { it.height.toString() == height }
        _uiState.update {
            it.copy(
                compressVideoBitrate = vbr,
                compressAudioBitrate = abr,
                compressMaxHeight = height,
                compressResolutionPresetIndex = resIndex.coerceAtLeast(0),
            )
        }
    }

    fun startCompress() {
        val s = _uiState.value
        if (compressJob?.isActive == true) return
        if (s.compressInputPath.isBlank()) {
            _uiState.update { it.copy(compressError = "Input file path is required") }
            return
        }

        val downloadsDir = runCatching { fileUtils.ensureDownloadsDir() }
            .getOrElse {
                _uiState.update { it.copy(compressError = "Cannot access downloads directory") }
                return
            }

        val outputExt = suggestedCompressionOutputExtension(s.compressInputPath.trim())
        val baseName = s.compressInputPath.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .let { if ('.' in it) it.substringBeforeLast('.') else it }
        val outputPath = downloadsDir.absolutePath + "/" + fileUtils.sanitizeFileName("${baseName}_compressed") + "." + outputExt
        val request = CompressionRequest(
            inputFilePath = s.compressInputPath.trim(),
            outputFilePath = outputPath,
            targetVideoBitrateKbps = s.compressVideoBitrate.toIntOrNull(),
            targetAudioBitrateKbps = s.compressAudioBitrate.toIntOrNull(),
            maxHeight = s.compressMaxHeight.toIntOrNull(),
        )
        val sourceSize = s.compressSourceSizeBytes ?: File(s.compressInputPath.trim()).takeIf { it.exists() }?.length()
        compressJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCompressing = true,
                    isStoppingCompress = false,
                    compressWasCanceled = false,
                    compressResult = null,
                    compressError = null,
                    compressProgress = 0f,
                )
            }
            try {
                val result = repository.compressMedia(request) { progress ->
                    _uiState.update { state -> state.copy(compressProgress = progress) }
                }
                val resultPath = result.getOrNull()
                // Copy to public Downloads folder so user can find it
                val publicPath = resultPath?.let { path ->
                    runCatching { fileUtils.copyToPublicDownloads(File(path), null) }.getOrNull()
                }
                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        isStoppingCompress = false,
                        compressWasCanceled = false,
                        compressProgress = null,
                        compressResult = resultPath?.let { path -> "Saved to: ${publicPath ?: path}" },
                        compressResultSizeBytes = resultPath?.let { path -> File(path).length() },
                        compressSourceSizeBytes = sourceSize,
                        compressError = result.exceptionOrNull()?.message,
                    )
                }
            } catch (_: CancellationException) {
                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        isStoppingCompress = false,
                        compressWasCanceled = true,
                        compressProgress = null,
                        compressResult = null,
                        compressResultSizeBytes = null,
                        compressSourceSizeBytes = sourceSize,
                        compressError = "Compression canceled by you.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        isStoppingCompress = false,
                        compressWasCanceled = false,
                        compressProgress = null,
                        compressResult = null,
                        compressResultSizeBytes = null,
                        compressSourceSizeBytes = sourceSize,
                        compressError = error.message ?: "Compression failed.",
                    )
                }
            } finally {
                compressJob = null
            }
        }
    }

    fun stopCompress() {
        if (compressJob?.isActive != true) return
        _uiState.update { it.copy(isStoppingCompress = true) }
        compressJob?.cancel(CancellationException("Compression canceled by user"))
    }

    fun dismissCompressResult() = _uiState.update {
        it.copy(
            compressResult = null,
            compressError = null,
            compressWasCanceled = false,
            compressResultSizeBytes = null,
        )
    }
}
