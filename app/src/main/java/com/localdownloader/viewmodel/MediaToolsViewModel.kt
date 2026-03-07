package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.usecases.ConvertMediaUseCase
import com.localdownloader.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaToolsUiState(
    // Convert
    val convertInputPath: String = "",
    val convertOutputFormat: String = "mp4",
    val convertAudioBitrate: String = "",
    val convertVideoBitrate: String = "",
    val isConverting: Boolean = false,
    val convertProgress: Float? = null,
    val convertResult: String? = null,
    val convertError: String? = null,

    // Compress
    val compressInputPath: String = "",
    val compressMaxHeight: String = "720",
    val compressVideoBitrate: String = "1000",
    val compressAudioBitrate: String = "128",
    val isCompressing: Boolean = false,
    val compressProgress: Float? = null,
    val compressResult: String? = null,
    val compressError: String? = null,
)

@HiltViewModel
class MediaToolsViewModel @Inject constructor(
    private val convertMediaUseCase: ConvertMediaUseCase,
    private val fileUtils: FileUtils,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaToolsUiState())
    val uiState: StateFlow<MediaToolsUiState> = _uiState.asStateFlow()

    // ── Convert fields ────────────────────────────────────────────────

    fun onConvertInputPathChanged(v: String) = _uiState.update { it.copy(convertInputPath = v, convertResult = null, convertError = null) }
    fun onConvertOutputFormatChanged(v: String) = _uiState.update { it.copy(convertOutputFormat = v) }
    fun onConvertAudioBitrateChanged(v: String) = _uiState.update { it.copy(convertAudioBitrate = v) }
    fun onConvertVideoBitrateChanged(v: String) = _uiState.update { it.copy(convertVideoBitrate = v) }

    fun startConvert() {
        val s = _uiState.value
        if (s.convertInputPath.isBlank()) {
            _uiState.update { it.copy(convertError = "Input file path is required") }
            return
        }
        val ext = s.convertOutputFormat.trim().lowercase()
        val baseName = s.convertInputPath.trim()
            .substringAfterLast('/')
            .let { if ('.' in it) it.substringBeforeLast('.') else it }
        val outputPath = fileUtils.ensureDownloadsDir().absolutePath +
            "/" + fileUtils.sanitizeFileName(baseName) + "." + ext
        val request = ConversionRequest(
            inputFilePath = s.convertInputPath.trim(),
            outputFilePath = outputPath,
            outputFormat = ext,
            audioBitrateKbps = s.convertAudioBitrate.toIntOrNull(),
            videoBitrateKbps = s.convertVideoBitrate.toIntOrNull(),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, convertResult = null, convertError = null, convertProgress = 0f) }
            val result = convertMediaUseCase.convert(request) { progress ->
                _uiState.value = _uiState.value.copy(convertProgress = progress)
            }
            _uiState.update {
                it.copy(
                    isConverting = false,
                    convertProgress = null,
                    convertResult = result.getOrNull()?.let { path -> "Saved to: $path" },
                    convertError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun dismissConvertResult() = _uiState.update { it.copy(convertResult = null, convertError = null) }

    // ── Compress fields ───────────────────────────────────────────────

    fun onCompressInputPathChanged(v: String) = _uiState.update { it.copy(compressInputPath = v, compressResult = null, compressError = null) }
    fun onCompressMaxHeightChanged(v: String) = _uiState.update { it.copy(compressMaxHeight = v) }
    fun onCompressVideoBitrateChanged(v: String) = _uiState.update { it.copy(compressVideoBitrate = v) }
    fun onCompressAudioBitrateChanged(v: String) = _uiState.update { it.copy(compressAudioBitrate = v) }

    fun startCompress() {
        val s = _uiState.value
        if (s.compressInputPath.isBlank()) {
            _uiState.update { it.copy(compressError = "Input file path is required") }
            return
        }
        val inputExt = s.compressInputPath.trim().substringAfterLast('.').lowercase().ifBlank { "mp4" }
        val baseName = s.compressInputPath.trim()
            .substringAfterLast('/')
            .let { if ('.' in it) it.substringBeforeLast('.') else it }
        val outputPath = fileUtils.ensureDownloadsDir().absolutePath +
            "/" + fileUtils.sanitizeFileName("${baseName}_compressed") + "." + inputExt
        val request = CompressionRequest(
            inputFilePath = s.compressInputPath.trim(),
            outputFilePath = outputPath,
            targetVideoBitrateKbps = s.compressVideoBitrate.toIntOrNull(),
            targetAudioBitrateKbps = s.compressAudioBitrate.toIntOrNull(),
            maxHeight = s.compressMaxHeight.toIntOrNull(),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isCompressing = true, compressResult = null, compressError = null, compressProgress = 0f) }
            val result = convertMediaUseCase.compress(request) { progress ->
                _uiState.value = _uiState.value.copy(compressProgress = progress)
            }
            _uiState.update {
                it.copy(
                    isCompressing = false,
                    compressProgress = null,
                    compressResult = result.getOrNull()?.let { path -> "Saved to: $path" },
                    compressError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun dismissCompressResult() = _uiState.update { it.copy(compressResult = null, compressError = null) }
}

private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
