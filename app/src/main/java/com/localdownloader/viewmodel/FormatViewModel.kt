package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.utils.Logger
import com.localdownloader.utils.UrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormatViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val urlValidator: UrlValidator,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FormatUiState())
    val uiState: StateFlow<FormatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                applySettings(settings)
            }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { state ->
            state.copy(urlInput = url, errorMessage = null, infoMessage = null)
        }
    }

    fun analyzeUrl() {
        val url = uiState.value.urlInput.trim()
        logger.i("FormatViewModel", "Analyze requested for URL: $url")
        if (!urlValidator.isValidHttpUrl(url)) {
            logger.w("FormatViewModel", "Rejected analyze request due to invalid URL: $url")
            _uiState.update { state ->
                state.copy(errorMessage = "Please enter a valid http/https URL.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isAnalyzing = true,
                    errorMessage = null,
                    infoMessage = null,
                    videoInfo = null,
                )
            }

            val result = runCatching { repository.analyzeUrl(url) }
                .getOrElse { throwable ->
                    Result.failure(IllegalStateException(throwable.message ?: "Analyze failed", throwable))
                }
            result.fold(
                onSuccess = { info ->
                    logger.i(
                        "FormatViewModel",
                        "Analyze success for URL: $url, title='${info.title}', formats=${info.formats.size}",
                    )
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            videoInfo = info,
                            infoMessage = "Found ${info.formats.size} formats.",
                        )
                    }
                },
                onFailure = { error ->
                    logger.e("FormatViewModel", "Analyze failed for URL: $url", error)
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            errorMessage = buildString {
                                append(error.message ?: "Failed to analyze URL.")
                                append(" Ensure yt-dlp runtime is initialized and this device ABI is supported.")
                            },
                        )
                    }
                },
            )
        }
    }

    fun onQualityChanged(quality: VideoQuality) {
        _uiState.update { state -> state.copy(selectedQuality = quality) }
    }

    fun onStreamTypeChanged(streamType: StreamType) {
        _uiState.update { state -> state.copy(selectedStreamType = streamType) }
    }

    fun onContainerChanged(container: String) {
        _uiState.update { state -> state.copy(selectedContainer = container.lowercase()) }
    }

    fun onAudioFormatChanged(value: String) {
        _uiState.update { state -> state.copy(selectedAudioFormat = value.lowercase()) }
    }

    fun onAudioBitrateChanged(value: Int) {
        _uiState.update { state -> state.copy(audioBitrateKbps = value.coerceAtLeast(64)) }
    }

    fun onDownloadSubtitlesChanged(value: Boolean) {
        _uiState.update { state -> state.copy(downloadSubtitles = value) }
    }

    fun onEmbedMetadataChanged(value: Boolean) {
        _uiState.update { state -> state.copy(embedMetadata = value) }
    }

    fun onEmbedThumbnailChanged(value: Boolean) {
        _uiState.update { state -> state.copy(embedThumbnail = value) }
    }

    fun onWriteThumbnailChanged(value: Boolean) {
        _uiState.update { state -> state.copy(writeThumbnail = value) }
    }

    fun onPlaylistEnabledChanged(value: Boolean) {
        _uiState.update { state -> state.copy(enablePlaylist = value) }
    }

    fun onOutputTemplateChanged(value: String) {
        _uiState.update { state -> state.copy(outputTemplate = value) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = uiState.value
            val newSettings = state.appSettings.copy(
                defaultOutputTemplate = state.outputTemplate,
                defaultMergeContainer = state.selectedContainer,
                autoEmbedMetadata = state.embedMetadata,
                autoEmbedThumbnail = state.embedThumbnail,
                darkTheme = state.isDarkTheme,
            )
            runCatching { repository.updateSettings(newSettings) }
                .onSuccess {
                    _uiState.update { it.copy(infoMessage = "Settings saved locally.") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to save settings.") }
                }
        }
    }

    fun queueDownload() {
        val state = uiState.value
        val info = state.videoInfo
        logger.i(
            "FormatViewModel",
            "Queue requested. hasVideoInfo=${info != null}, quality=${state.selectedQuality}, streamType=${state.selectedStreamType}",
        )
        if (info == null) {
            logger.w("FormatViewModel", "Queue rejected: analyze missing")
            _uiState.update { current ->
                current.copy(errorMessage = "Analyze a URL first.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isQueueing = true, errorMessage = null, infoMessage = null) }

            val isAudioOnly = state.selectedStreamType == StreamType.AUDIO_ONLY
            val formatSelector = buildFormatSelector(
                quality = state.selectedQuality,
                streamType = state.selectedStreamType,
                container = state.selectedContainer,
            )

            val options = DownloadOptions(
                url = info.webpageUrl,
                formatId = formatSelector,
                outputTemplate = state.outputTemplate,
                mergeOutputFormat = if (!isAudioOnly) state.selectedContainer.ifBlank { null } else null,
                isPlaylistEnabled = state.enablePlaylist,
                shouldDownloadSubtitles = state.downloadSubtitles,
                shouldEmbedMetadata = state.embedMetadata,
                shouldEmbedThumbnail = state.embedThumbnail,
                shouldWriteThumbnail = state.writeThumbnail,
                extractAudio = isAudioOnly,
                audioFormat = if (isAudioOnly) state.selectedAudioFormat.ifBlank { null } else null,
                audioBitrateKbps = if (isAudioOnly) state.audioBitrateKbps else null,
            )
            logger.i(
                "FormatViewModel",
                "Queueing download for URL=${options.url}, formatSelector=$formatSelector, extractAudio=${options.extractAudio}",
            )

            val queueResult = runCatching { repository.enqueueDownload(options, info.title) }
                .getOrElse { throwable ->
                    Result.failure(IllegalStateException(throwable.message ?: "Unable to queue download.", throwable))
                }

            queueResult.fold(
                onSuccess = { taskId ->
                    logger.i("FormatViewModel", "Queue success. taskId=$taskId")
                    _uiState.update { current ->
                        current.copy(
                            isQueueing = false,
                            infoMessage = "Added to queue. Task: $taskId",
                        )
                    }
                },
                onFailure = { error ->
                    logger.e("FormatViewModel", "Queue failed", error)
                    _uiState.update { current ->
                        current.copy(
                            isQueueing = false,
                            errorMessage = error.message ?: "Unable to queue download.",
                        )
                    }
                },
            )
        }
    }

    fun dismissMessage() {
        _uiState.update { state -> state.copy(errorMessage = null, infoMessage = null) }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _uiState.update { it.copy(isDarkTheme = enabled) }
        viewModelScope.launch {
            val state = uiState.value
            repository.updateSettings(state.appSettings.copy(darkTheme = enabled))
        }
    }

    private fun buildFormatSelector(
        quality: VideoQuality,
        streamType: StreamType,
        container: String,
    ): String {
        val h = quality.maxHeight ?.let { "[height<=$it]" } ?: ""
        val isAudio = streamType == StreamType.AUDIO_ONLY
        val hasContainer = !isAudio && container.isNotBlank()
        val containerFilter = when {
            !hasContainer -> ""
            container == "mp4" || container == "mov" -> "[ext=mp4]"
            container == "webm" -> "[ext=webm]"
            else -> ""
        }
        return when (streamType) {
            StreamType.AUDIO_ONLY -> "bestaudio/best"
            StreamType.VIDEO_ONLY -> "bestvideo$h$containerFilter/bestvideo"
            StreamType.VIDEO_AUDIO -> "best$h/best"
        }
    }

    private fun applySettings(settings: AppSettings) {
        logger.d(
            "FormatViewModel",
            "Applying settings: merge=${settings.defaultMergeContainer}, template=${settings.defaultOutputTemplate}, embedMeta=${settings.autoEmbedMetadata}",
        )
        _uiState.update { state ->
            state.copy(
                appSettings = settings,
                selectedContainer = settings.defaultMergeContainer,
                outputTemplate = settings.defaultOutputTemplate,
                embedMetadata = settings.autoEmbedMetadata,
                embedThumbnail = settings.autoEmbedThumbnail,
                isDarkTheme = settings.darkTheme,
            )
        }
    }
}
