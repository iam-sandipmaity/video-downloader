package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.domain.models.YoutubeAuthBundle
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
                    availableVideoAudioChoices = emptyList(),
                    availableVideoOnlyChoices = emptyList(),
                    availableAudioOnlyChoices = emptyList(),
                    selectedFormatSelector = null,
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
                    val choiceBundle = buildChoices(info)
                    val selectedSelector = when (uiState.value.selectedStreamType) {
                        StreamType.VIDEO_AUDIO -> choiceBundle.videoAudioChoices.firstOrNull()?.selector
                        StreamType.VIDEO_ONLY -> choiceBundle.videoOnlyChoices.firstOrNull()?.selector
                        StreamType.AUDIO_ONLY -> choiceBundle.audioOnlyChoices.firstOrNull()?.selector
                    }
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            videoInfo = info,
                            availableVideoAudioChoices = choiceBundle.videoAudioChoices,
                            availableVideoOnlyChoices = choiceBundle.videoOnlyChoices,
                            availableAudioOnlyChoices = choiceBundle.audioOnlyChoices,
                            selectedFormatSelector = selectedSelector,
                            enablePlaylist = state.enablePlaylist || info.isPlaylist,
                            infoMessage = when {
                                info.isPlaylist -> {
                                    val itemCount = info.playlistCount ?: info.playlistEntries.size
                                    "Playlist ready: $itemCount items will queue one by one."
                                }
                                else -> "Found ${info.formats.size} formats."
                            },
                        )
                    }
                },
                onFailure = { error ->
                    logger.e("FormatViewModel", "Analyze failed for URL: $url", error)
                    val baseMessage = error.message?.takeIf { it.isNotBlank() } ?: "Failed to analyze URL."
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            errorMessage = buildString {
                                append(baseMessage)
                                if (shouldShowRuntimeHint(baseMessage)) {
                                    append(" Ensure yt-dlp runtime is initialized and this device ABI is supported.")
                                }
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
        _uiState.update { state ->
            val selector = when (streamType) {
                StreamType.VIDEO_AUDIO -> state.availableVideoAudioChoices.firstOrNull()?.selector
                StreamType.VIDEO_ONLY -> state.availableVideoOnlyChoices.firstOrNull()?.selector
                StreamType.AUDIO_ONLY -> state.availableAudioOnlyChoices.firstOrNull()?.selector
            }
            state.copy(selectedStreamType = streamType, selectedFormatSelector = selector)
        }
    }

    fun onFormatSelectorChanged(selector: String) {
        _uiState.update { state -> state.copy(selectedFormatSelector = selector) }
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

    fun onYoutubeAuthEnabledChanged(value: Boolean) {
        _uiState.update { state -> state.copy(youtubeAuthEnabled = value) }
    }

    fun onYoutubeCookiesPathChanged(value: String) {
        _uiState.update { state -> state.copy(youtubeCookiesPath = value) }
    }

    fun onYoutubeCookiesImported(path: String) {
        _uiState.update { state ->
            state.copy(
                youtubeCookiesPath = path,
                infoMessage = "Cookies imported. Add a PO token or import a full auth bundle.",
                errorMessage = null,
            )
        }
    }

    fun onYoutubePoTokenChanged(value: String) {
        _uiState.update { state -> state.copy(youtubePoToken = value) }
    }

    fun onYoutubePoTokenClientHintChanged(value: String) {
        _uiState.update { state -> state.copy(youtubePoTokenClientHint = value) }
    }

    fun applyYoutubeAuthBundle(
        bundle: YoutubeAuthBundle,
        importedCookiesPath: String,
    ) {
        val normalizedHint = normalizePoTokenClientHint(bundle.poTokenClientHint)
        _uiState.update { state ->
            state.copy(
                youtubeAuthEnabled = true,
                youtubeCookiesPath = importedCookiesPath,
                youtubePoToken = bundle.poToken.orEmpty(),
                youtubePoTokenClientHint = normalizedHint,
                infoMessage = "YouTube auth bundle imported. Settings are ready to save.",
                errorMessage = null,
            )
        }
        persistSettings("YouTube auth bundle imported and saved. Retry the blocked video again.")
    }

    fun onYoutubeAuthImportFailed(message: String) {
        _uiState.update { state ->
            state.copy(
                errorMessage = message,
                infoMessage = null,
            )
        }
    }

    fun saveSettings() {
        persistSettings("Settings saved locally.")
    }

    private fun persistSettings(successMessage: String) {
        viewModelScope.launch {
            val state = uiState.value
            val newSettings = state.appSettings.copy(
                defaultOutputTemplate = state.outputTemplate,
                defaultMergeContainer = state.selectedContainer,
                autoEmbedMetadata = state.embedMetadata,
                autoEmbedThumbnail = state.embedThumbnail,
                youtubeAuthEnabled = state.youtubeAuthEnabled,
                youtubeCookiesPath = state.youtubeCookiesPath,
                youtubePoToken = state.youtubePoToken,
                youtubePoTokenClientHint = normalizePoTokenClientHint(state.youtubePoTokenClientHint),
                darkTheme = state.isDarkTheme,
            )
            runCatching { repository.updateSettings(newSettings) }
                .onSuccess {
                    _uiState.update { it.copy(infoMessage = successMessage) }
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

            val selectedChoice = findChoice(state, state.selectedFormatSelector)
            val formatSelector = if (info.isPlaylist) {
                buildFormatSelector(
                    quality = state.selectedQuality,
                    streamType = state.selectedStreamType,
                    container = state.selectedContainer,
                )
            } else if (selectedChoice != null) {
                selectedChoice.selector
            } else if (isYoutubeUrl(info.webpageUrl)) {
                val h = "[height<=360]"
                val st = state.selectedStreamType
                when (st) {
                    StreamType.AUDIO_ONLY -> "bestaudio/best"
                    StreamType.VIDEO_ONLY -> "bestvideo$h/bestvideo"
                    StreamType.VIDEO_AUDIO -> "bestvideo$h+bestaudio/best$h/best"
                }
            } else {
                buildFormatSelector(
                    quality = state.selectedQuality,
                    streamType = state.selectedStreamType,
                    container = state.selectedContainer,
                )
            }
            val isAudioOnly = state.selectedStreamType == StreamType.AUDIO_ONLY
            val mergeContainer = selectedChoice?.takeIf { it.isMerged }?.container
            val (downloadExtractorArgs, fallbackExtractorArgs) = resolveDownloadExtractorArgs(info)

            val options = DownloadOptions(
                url = info.webpageUrl,
                formatId = formatSelector,
                outputTemplate = state.outputTemplate,
                extractorArgs = downloadExtractorArgs,
                fallbackExtractorArgs = fallbackExtractorArgs,
                youtubeAuthEnabled = state.youtubeAuthEnabled,
                youtubeCookiesPath = state.youtubeCookiesPath.ifBlank { null },
                youtubePoToken = state.youtubePoToken.ifBlank { null },
                youtubePoTokenClientHint = normalizePoTokenClientHint(state.youtubePoTokenClientHint),
                mergeOutputFormat = if (!isAudioOnly) mergeContainer ?: state.selectedContainer.ifBlank { null } else null,
                isPlaylistEnabled = info.isPlaylist || state.enablePlaylist,
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

            if (info.isPlaylist) {
                val playlistResult = runCatching {
                    repository.enqueuePlaylistDownload(
                        options = options,
                        playlistTitle = info.title,
                        entries = info.playlistEntries,
                    )
                }.getOrElse { throwable ->
                    Result.failure(IllegalStateException(throwable.message ?: "Unable to queue playlist.", throwable))
                }

                playlistResult.fold(
                    onSuccess = { taskIds ->
                        logger.i("FormatViewModel", "Playlist queue success. taskCount=${taskIds.size}")
                        _uiState.update { current ->
                            current.copy(
                                isQueueing = false,
                                infoMessage = "Queued ${taskIds.size} playlist items in order.",
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e("FormatViewModel", "Playlist queue failed", error)
                        _uiState.update { current ->
                            current.copy(
                                isQueueing = false,
                                errorMessage = error.message ?: "Unable to queue playlist.",
                            )
                        }
                    },
                )
                return@launch
            }

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
        return when (streamType) {
            StreamType.AUDIO_ONLY -> "bestaudio/best"
            StreamType.VIDEO_ONLY -> "bestvideo$h/bestvideo"
            StreamType.VIDEO_AUDIO -> {
                val vExt = when (container) {
                    "mp4", "mov" -> "[ext=mp4]"
                    "webm" -> "[ext=webm]"
                    else -> ""
                }
                val aExt = when (container) {
                    "mp4", "mov" -> "[ext=m4a]"
                    "webm" -> "[ext=webm]"
                    else -> ""
                }
                "bestvideo$h$vExt+bestaudio$aExt/bestvideo$h+bestaudio/best$h/best"
            }
        }
    }

    private fun findChoice(state: FormatUiState, selector: String?): FormatChoice? {
        if (selector.isNullOrBlank()) return null
        val choices = when (state.selectedStreamType) {
            StreamType.VIDEO_AUDIO -> state.availableVideoAudioChoices
            StreamType.VIDEO_ONLY -> state.availableVideoOnlyChoices
            StreamType.AUDIO_ONLY -> state.availableAudioOnlyChoices
        }
        return choices.firstOrNull { it.selector == selector }
    }

    private data class ChoiceBundle(
        val videoAudioChoices: List<FormatChoice>,
        val videoOnlyChoices: List<FormatChoice>,
        val audioOnlyChoices: List<FormatChoice>,
    )

    private fun buildChoices(info: VideoInfo): ChoiceBundle {
        if (info.isPlaylist) {
            return ChoiceBundle(
                videoAudioChoices = emptyList(),
                videoOnlyChoices = emptyList(),
                audioOnlyChoices = emptyList(),
            )
        }
        val formats = info.formats
        val audioOnly = formats.filter { it.isAudioOnly }
        val videoOnly = formats.filter { it.isVideoOnly }
        val muxed = formats.filter { !it.isAudioOnly && !it.isVideoOnly }

        val audioChoices = audioOnly.map { audio ->
            val bitrate = audio.bitrateKbps?.let { "${it}kbps" } ?: ""
            val label = listOf("audio", audio.extension, bitrate, audio.note.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" ")
            FormatChoice(
                selector = audio.formatId,
                label = label,
                streamType = StreamType.AUDIO_ONLY,
                container = audio.extension,
                height = null,
                isMerged = false,
            )
        }.sortedByDescending { extractBitrate(it.label) }

        val videoOnlyChoices = videoOnly.map { video ->
            val fps = video.fps?.let { "${it.toInt()}fps" } ?: ""
            val bitrate = video.bitrateKbps?.let { "${it}kbps" } ?: ""
            val label = listOf(video.resolution ?: "video", video.extension, fps, bitrate, video.note.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" ")
            FormatChoice(
                selector = video.formatId,
                label = label,
                streamType = StreamType.VIDEO_ONLY,
                container = video.extension,
                height = parseHeight(video.resolution),
                isMerged = false,
            )
        }.sortedWith(compareByDescending<FormatChoice> { it.height ?: 0 }.thenByDescending { extractBitrate(it.label) })

        val audioByExt = audioOnly.groupBy { it.extension.lowercase() }
        val bestAudioOverall = audioOnly.maxByOrNull { it.bitrateKbps ?: 0 }
        val mergedChoices = videoOnly.mapNotNull { video ->
            val preferredExts = preferredAudioExts(video.extension)
            val preferredAudio = preferredExts.asSequence()
                .mapNotNull { ext -> audioByExt[ext]?.maxByOrNull { it.bitrateKbps ?: 0 } }
                .firstOrNull() ?: bestAudioOverall
            preferredAudio?.let { audio ->
                val fps = video.fps?.let { "${it.toInt()}fps" } ?: ""
                val audioBitrate = audio.bitrateKbps?.let { "${it}kbps" } ?: ""
                val label = listOf(
                    video.resolution ?: "video",
                    video.extension,
                    fps,
                    "audio=${audio.extension}",
                    audioBitrate,
                    "merge",
                ).filter { it.isNotBlank() }.joinToString(" ")
                FormatChoice(
                    selector = "${video.formatId}+${audio.formatId}",
                    label = label,
                    streamType = StreamType.VIDEO_AUDIO,
                    container = video.extension,
                    height = parseHeight(video.resolution),
                    isMerged = true,
                )
            }
        }

        val muxedChoices = muxed.map { item ->
            val fps = item.fps?.let { "${it.toInt()}fps" } ?: ""
            val bitrate = item.bitrateKbps?.let { "${it}kbps" } ?: ""
            val label = listOf(item.resolution ?: "video", item.extension, fps, bitrate, "muxed", item.note.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" ")
            FormatChoice(
                selector = item.formatId,
                label = label,
                streamType = StreamType.VIDEO_AUDIO,
                container = item.extension,
                height = parseHeight(item.resolution),
                isMerged = false,
            )
        }

        val videoAudioChoices = (mergedChoices + muxedChoices)
            .distinctBy { it.selector }
            .sortedWith(compareByDescending<FormatChoice> { it.height ?: 0 }.thenByDescending { extractBitrate(it.label) })

        return ChoiceBundle(
            videoAudioChoices = videoAudioChoices,
            videoOnlyChoices = videoOnlyChoices,
            audioOnlyChoices = audioChoices,
        )
    }

    private fun preferredAudioExts(videoExt: String): List<String> {
        return when (videoExt.lowercase()) {
            "mp4", "mov", "mkv" -> listOf("m4a", "mp4", "aac")
            "webm" -> listOf("webm", "opus")
            else -> listOf("m4a", "mp4", "webm", "opus", "aac")
        }
    }

    private fun parseHeight(resolution: String?): Int? {
        val trimmed = resolution ?: return null
        return trimmed.substringBefore("p", trimmed).toIntOrNull()
    }

    private fun extractBitrate(label: String): Int {
        val match = Regex("(\\d+)kbps").find(label) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: 0
    }

    private fun resolveDownloadExtractorArgs(info: VideoInfo): Pair<String?, String?> {
        val analysisExtractorArgs = info.extractorArgs?.trim()?.ifBlank { null }
        if (!isYoutubeUrl(info.webpageUrl)) {
            return analysisExtractorArgs to null
        }

        // Let yt-dlp use its own evolving default YouTube client behavior first,
        // and keep the broader analysis client as an explicit fallback for retries.
        return null to analysisExtractorArgs
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val normalized = url.lowercase()
        return normalized.contains("youtube.com") || normalized.contains("youtu.be")
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
                youtubeAuthEnabled = settings.youtubeAuthEnabled,
                youtubeCookiesPath = settings.youtubeCookiesPath,
                youtubePoToken = settings.youtubePoToken,
                youtubePoTokenClientHint = normalizePoTokenClientHint(settings.youtubePoTokenClientHint),
                isDarkTheme = settings.darkTheme,
            )
        }
    }

    private fun normalizePoTokenClientHint(value: String?): String {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "web.gvs", "mweb.gvs" -> normalized
            else -> "web.gvs"
        }
    }

    private fun shouldShowRuntimeHint(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("abi") ||
            normalized.contains("runtime") ||
            normalized.contains("missing runtime binary") ||
            normalized.contains("missing yt-dlp script") ||
            normalized.contains("exec format") ||
            normalized.contains("libpython") ||
            normalized.contains("not initialized")
    }
}

