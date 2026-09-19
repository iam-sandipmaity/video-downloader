package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.SettingsStore
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.PlaylistDownloadRequest
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.shouldTreatAsAudioOnlyChoice
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.downloader.FormatSelectorBuilder
import com.localdownloader.downloader.YoutubeRequestPlanner
import com.localdownloader.downloader.looksLikeYoutubeUrl
import com.localdownloader.ui.model.toReadableSize
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import com.localdownloader.utils.NetworkStatusMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class QuickQualityOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val height: Int? = null,
    val bitrateKbps: Int? = null,
    val fps: Double? = null,
    val formatChoice: FormatChoice? = null,
    val mediaFormat: MediaFormat? = null,
    val fileSizeBytes: Long? = null,
) {
    val displayLabel: String
        get() = if (!subtitle.isNullOrBlank()) "$title · $subtitle" else title
}

data class QuickFormatOption(
    val id: String,
    val label: String,
    val container: String,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val isOriginalStream: Boolean = false,
)

data class QuickDownloadUiState(
    val url: String = "",
    val isAnalyzing: Boolean = true,
    val isQueueing: Boolean = false,
    val isDownloadSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showMeteredNetworkDialog: Boolean = false,
    val showTitleEditDialog: Boolean = false,
    val isQuickSettingsExpanded: Boolean = false,
    val videoInfo: VideoInfo? = null,
    val title: String = "",
    val uploader: String? = null,
    val durationFormatted: String? = null,
    val thumbnailUrl: String? = null,
    val domainHost: String? = null,
    val selectedStreamType: StreamType = StreamType.VIDEO_AUDIO,
    val videoQualityOptions: List<QuickQualityOption> = emptyList(),
    val audioQualityOptions: List<QuickQualityOption> = emptyList(),
    val selectedVideoQuality: QuickQualityOption? = null,
    val selectedAudioQuality: QuickQualityOption? = null,
    val videoFormatOptions: List<QuickFormatOption> = emptyList(),
    val audioFormatOptions: List<QuickFormatOption> = emptyList(),
    val selectedVideoFormat: QuickFormatOption? = null,
    val selectedAudioFormat: QuickFormatOption? = null,
    val threads: Int = 4,
    val appSettings: AppSettings = AppSettings(),
) {
    val isAudioMode: Boolean
        get() = selectedStreamType == StreamType.AUDIO_ONLY

    val currentSelectedQualityLabel: String
        get() = if (isAudioMode) {
            selectedAudioQuality?.title ?: selectedAudioFormat?.label ?: "Audio"
        } else {
            selectedVideoQuality?.title ?: selectedVideoFormat?.label ?: "Video"
        }

    val currentSelectedSize: String?
        get() = if (isAudioMode) {
            selectedAudioQuality?.subtitle
        } else {
            selectedVideoQuality?.subtitle
        }

    val ctaButtonLabel: String
        get() {
            val label = currentSelectedQualityLabel
            val size = currentSelectedSize
            return if (!size.isNullOrBlank()) {
                "Download $label ($size)"
            } else {
                "Download $label"
            }
        }
}

@HiltViewModel
class QuickDownloadViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val settingsStore: SettingsStore,
    private val fileUtils: FileUtils,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val logger: Logger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickDownloadUiState())
    val uiState: StateFlow<QuickDownloadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = runCatching { settingsStore.observeSettings().first() }
                .getOrDefault(AppSettings())
            _uiState.update { it.copy(appSettings = settings) }
        }
    }

    fun initUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || (_uiState.value.url == trimmed && _uiState.value.videoInfo != null)) {
            return
        }
        _uiState.update {
            it.copy(
                url = trimmed,
                isAnalyzing = true,
                errorMessage = null,
                isDownloadSuccess = false,
            )
        }
        analyzeUrl(trimmed)
    }

    fun retry() {
        val currentUrl = _uiState.value.url
        if (currentUrl.isNotBlank()) {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            analyzeUrl(currentUrl)
        }
    }

    private fun analyzeUrl(url: String) {
        viewModelScope.launch {
            val settings = _uiState.value.appSettings
            val cookiesPath = resolveCookiesPath(url, settings)
            val userAgent = if (settings.cookieUserAgentEnabled) {
                CookieTextCodec.COOKIE_USER_AGENT
            } else {
                null
            }

            val preferredExtractorArgs = if (looksLikeYoutubeUrl(url) && settings.youtubeAuthConfig.enabled && settings.youtubeAuthConfig.isConfigured()) {
                YoutubeRequestPlanner.preferredAuthenticatedExtractorArgs(
                    poToken = settings.youtubeAuthConfig.buildPoTokenValue(),
                    preferredHint = settings.youtubeAuthConfig.clientHint,
                    dataSyncId = settings.youtubeAuthConfig.dataSyncId.ifBlank { null },
                    visitorData = settings.youtubeAuthConfig.visitorData.ifBlank { null },
                )
            } else {
                null
            }

            val result = repository.analyzeUrl(
                url = url,
                cookiesPath = cookiesPath,
                userAgent = userAgent,
                preferredExtractorArgs = preferredExtractorArgs,
            )

            result.fold(
                onSuccess = { info ->
                    logger.i("QuickDownloadViewModel", "Analyzed successfully: ${info.title}")
                    val videoFormats = buildVideoFormats(info)
                    val defaultVideoFormat = videoFormats.firstOrNull { it.container == "mp4" || it.label.contains("MP4") }
                        ?: videoFormats.firstOrNull()
                    val videoQualities = buildVideoQualities(info, defaultVideoFormat)
                    val defaultVideoQuality = videoQualities.firstOrNull { it.height != null && it.height <= 720 }
                        ?: videoQualities.firstOrNull()

                    val audioFormats = buildAudioFormats(info)
                    val defaultAudioFormat = audioFormats.firstOrNull { it.isOriginalStream }
                        ?: audioFormats.firstOrNull { it.container == "mp3" }
                        ?: audioFormats.firstOrNull()
                    val audioQualities = buildAudioQualities(info, defaultAudioFormat)
                    val defaultAudioQuality = audioQualities.firstOrNull { it.bitrateKbps == 160 || it.bitrateKbps == 128 }
                        ?: audioQualities.firstOrNull()

                    val durationStr = formatDurationSeconds(info.durationSeconds)
                    val domainStr = extractDomainHost(info.webpageUrl.ifBlank { url })

                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            errorMessage = null,
                            videoInfo = info,
                            title = info.title,
                            uploader = info.uploader,
                            durationFormatted = durationStr,
                            thumbnailUrl = info.thumbnailUrl,
                            domainHost = domainStr,
                            selectedStreamType = StreamType.VIDEO_AUDIO,
                            videoQualityOptions = videoQualities,
                            audioQualityOptions = audioQualities,
                            selectedVideoQuality = defaultVideoQuality,
                            selectedAudioQuality = defaultAudioQuality,
                            videoFormatOptions = videoFormats,
                            audioFormatOptions = audioFormats,
                            selectedVideoFormat = defaultVideoFormat,
                            selectedAudioFormat = defaultAudioFormat,
                            threads = 4,
                        )
                    }
                },
                onFailure = { error ->
                    logger.e("QuickDownloadViewModel", "Failed to analyze URL: $url", error)
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "Failed to analyze link",
                        )
                    }
                },
            )
        }
    }

    fun onStreamTypeChanged(streamType: StreamType) {
        _uiState.update { it.copy(selectedStreamType = streamType) }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onShowTitleEditDialog(show: Boolean) {
        _uiState.update { it.copy(showTitleEditDialog = show) }
    }

    fun onToggleQuickSettings() {
        _uiState.update { it.copy(isQuickSettingsExpanded = !it.isQuickSettingsExpanded) }
    }

    fun onVideoQualitySelected(option: QuickQualityOption) {
        _uiState.update {
            it.copy(
                selectedVideoQuality = option,
                selectedStreamType = StreamType.VIDEO_AUDIO,
            )
        }
    }

    fun onAudioQualitySelected(option: QuickQualityOption) {
        _uiState.update {
            it.copy(
                selectedAudioQuality = option,
                selectedStreamType = StreamType.AUDIO_ONLY,
            )
        }
    }

    fun onVideoFormatSelected(option: QuickFormatOption) {
        val info = _uiState.value.videoInfo
        if (info == null) {
            _uiState.update { it.copy(selectedVideoFormat = option) }
            return
        }
        val newQualities = buildVideoQualities(info, option)
        val currentHeight = _uiState.value.selectedVideoQuality?.height
        val newSelectedQuality = newQualities.firstOrNull { it.height == currentHeight }
            ?: newQualities.firstOrNull { it.height != null && it.height <= 720 }
            ?: newQualities.firstOrNull()
        _uiState.update {
            it.copy(
                selectedVideoFormat = option,
                videoQualityOptions = newQualities,
                selectedVideoQuality = newSelectedQuality,
            )
        }
    }

    fun onAudioFormatSelected(option: QuickFormatOption) {
        val info = _uiState.value.videoInfo
        if (info == null) {
            _uiState.update { it.copy(selectedAudioFormat = option) }
            return
        }
        val newQualities = buildAudioQualities(info, option)
        val currentBitrate = _uiState.value.selectedAudioQuality?.bitrateKbps
        val newSelectedQuality = newQualities.firstOrNull { it.bitrateKbps == currentBitrate }
            ?: newQualities.firstOrNull { it.bitrateKbps == 160 || it.bitrateKbps == 128 }
            ?: newQualities.firstOrNull()
        _uiState.update {
            it.copy(
                selectedAudioFormat = option,
                audioQualityOptions = newQualities,
                selectedAudioQuality = newSelectedQuality,
            )
        }
    }

    fun onThreadsChanged(newThreads: Int) {
        _uiState.update { it.copy(threads = newThreads.coerceIn(1, 16)) }
    }

    fun download() {
        val state = _uiState.value
        if (state.videoInfo == null || state.isQueueing) return

        if (!state.appSettings.allowMeteredDownloads && networkStatusMonitor.isConnectedToMeteredNetwork()) {
            _uiState.update { it.copy(showMeteredNetworkDialog = true) }
            return
        }

        executeDownload()
    }

    fun dismissMeteredNetworkDialog() {
        _uiState.update { it.copy(showMeteredNetworkDialog = false) }
    }

    fun allowCellularAndDownload() {
        viewModelScope.launch {
            val updatedSettings = _uiState.value.appSettings.copy(allowMeteredDownloads = true)
            _uiState.update { it.copy(appSettings = updatedSettings, showMeteredNetworkDialog = false) }
            runCatching { repository.updateSettings(updatedSettings) }
            executeDownload()
        }
    }

    fun downloadWhenWifiAvailable() {
        _uiState.update { it.copy(showMeteredNetworkDialog = false) }
        executeDownload()
    }

    private fun executeDownload() {
        val state = _uiState.value
        val info = state.videoInfo ?: return
        if (state.isQueueing) return

        _uiState.update { it.copy(isQueueing = true, errorMessage = null) }

        viewModelScope.launch {
            val streamType = state.selectedStreamType
            val isAudio = streamType == StreamType.AUDIO_ONLY
            val isVideoOnly = streamType == StreamType.VIDEO_ONLY

            val requestedContainer = state.selectedVideoFormat?.container?.lowercase() ?: "mp4"
            val targetCategory = if (isAudio) {
                FileUtils.MediaFolderCategory.AUDIO
            } else {
                FileUtils.MediaFolderCategory.VIDEO
            }

            val formatId = when (streamType) {
                StreamType.AUDIO_ONLY -> {
                    val quality = state.selectedAudioQuality
                    val formatOpt = state.selectedAudioFormat
                    if (formatOpt?.isOriginalStream == true && quality?.mediaFormat != null) {
                        FormatSelectorBuilder.buildAudioOnlySelector(quality.mediaFormat)
                    } else {
                        "bestaudio/best"
                    }
                }
                StreamType.VIDEO_ONLY -> {
                    val quality = state.selectedVideoQuality
                    val vFormat = quality?.mediaFormat
                    if (vFormat != null) {
                        FormatSelectorBuilder.buildVideoOnlySelector(vFormat)
                    } else {
                        val h = quality?.height?.let { "[height<=$it]" }.orEmpty()
                        val vExt = when (requestedContainer) {
                            "mp4", "mov" -> "[ext=mp4]"
                            "webm" -> "[ext=webm]"
                            else -> ""
                        }
                        if (vExt.isNotEmpty()) {
                            "bestvideo$h$vExt/bestvideo$h/best$h/best"
                        } else {
                            "bestvideo$h/bestvideo/best$h/best"
                        }
                    }
                }
                StreamType.VIDEO_AUDIO -> {
                    val quality = state.selectedVideoQuality
                    val vFormat = quality?.mediaFormat
                    if (vFormat != null) {
                        if (!vFormat.isVideoOnly && vFormat.audioCodec != "none") {
                            FormatSelectorBuilder.buildMuxedSelector(vFormat)
                        } else {
                            val audioList = info.formats.filter { it.isAudioOnly || it.shouldTreatAsAudioOnlyChoice() }
                            val prefExts = if (requestedContainer == "mp4" || vFormat.normalizedExtension == "mp4") {
                                listOf("m4a", "aac", "mp3")
                            } else {
                                listOf("opus", "webm", "m4a")
                            }
                            val bestAudio = prefExts.asSequence()
                                .mapNotNull { ext -> audioList.filter { it.normalizedExtension == ext }.maxByOrNull { it.bitrateKbps ?: 0 } }
                                .firstOrNull() ?: audioList.maxByOrNull { it.bitrateKbps ?: 0 }

                            if (bestAudio != null) {
                                FormatSelectorBuilder.buildMergedSelector(vFormat, bestAudio)
                            } else {
                                val h = quality.height?.let { "[height<=$it]" }.orEmpty()
                                val vExt = when (requestedContainer) {
                                    "mp4", "mov" -> "[ext=mp4]"
                                    "webm" -> "[ext=webm]"
                                    else -> ""
                                }
                                if (vExt.isNotEmpty()) {
                                    "bestvideo$h$vExt+bestaudio/bestvideo$h+bestaudio/best$h/best"
                                } else {
                                    "bestvideo$h+bestaudio/best$h/best"
                                }
                            }
                        }
                    } else {
                        val h = quality?.height?.let { "[height<=$it]" }.orEmpty()
                        val vExt = when (requestedContainer) {
                            "mp4", "mov" -> "[ext=mp4]"
                            "webm" -> "[ext=webm]"
                            else -> ""
                        }
                        val aExt = when (requestedContainer) {
                            "mp4", "mov" -> "[ext=m4a]"
                            "webm" -> "[ext=webm]"
                            else -> ""
                        }
                        if (vExt.isNotEmpty()) {
                            "bestvideo$h$vExt+bestaudio$aExt/bestvideo$h+bestaudio/best$h/best"
                        } else {
                            "bestvideo$h+bestaudio/best$h/best"
                        }
                    }
                }
            }

            val mergeFormat = if (!isAudio) {
                when (requestedContainer) {
                    "auto" -> "mp4"
                    else -> requestedContainer
                }
            } else {
                null
            }

            val cookiesPath = resolveCookiesPath(state.url, state.appSettings)

            val baseTemplate = if (isAudio) {
                state.appSettings.defaultAudioOutputTemplate
            } else {
                state.appSettings.defaultOutputTemplate
            }

            val targetTemplate = if (state.title.isNotBlank() && state.title != info.title) {
                val sanitized = fileUtils.sanitizeFileName(state.title.trim())
                if (sanitized.isNotBlank()) {
                    "$sanitized [%(id)s].%(ext)s"
                } else {
                    baseTemplate
                }
            } else {
                baseTemplate
            }

            val resolvedOutputTemplate = fileUtils.createOutputTemplateWithDirectory(
                template = targetTemplate,
                category = targetCategory,
            )

            val baseOptions = DownloadOptions(
                url = state.url,
                formatId = formatId,
                outputTemplate = resolvedOutputTemplate,
                thumbnailUrl = info.thumbnailUrl,
                youtubeCookiesPath = cookiesPath,
                youtubeAuthEnabled = state.appSettings.youtubeAuthConfig.isConfigured(),
                youtubePoToken = state.appSettings.youtubeAuthConfig.buildPoTokenValue(),
                youtubePoTokenClientHint = state.appSettings.youtubeAuthConfig.clientHint,
                mergeOutputFormat = mergeFormat,
                preferredVideoHeight = if (!isAudio) state.selectedVideoQuality?.height else null,
                expectedDurationSeconds = info.durationSeconds,
                extractAudio = isAudio,
                downloadVideoOnly = isVideoOnly,
                removeAudioFromVideo = isVideoOnly,
                audioFormat = if (isAudio) state.selectedAudioFormat?.container ?: "mp3" else null,
                audioBitrateKbps = if (isAudio) state.selectedAudioQuality?.bitrateKbps ?: 160 else null,
                concurrentFragments = state.threads,
                shouldEmbedMetadata = state.appSettings.autoEmbedMetadata,
                shouldEmbedThumbnail = state.appSettings.autoEmbedThumbnail,
                shouldDownloadSubtitles = state.appSettings.autoDownloadSubtitles,
                shouldEmbedSubtitles = state.appSettings.autoEmbedSubtitles,
            )

            val enqueueResult = if (info.isPlaylist && info.playlistEntries.isNotEmpty()) {
                val requests = info.playlistEntries.map { entry ->
                    PlaylistDownloadRequest(
                        entry = entry,
                        options = baseOptions.copy(
                            url = entry.webpageUrl.ifBlank { state.url },
                            thumbnailUrl = entry.thumbnailUrl ?: baseOptions.thumbnailUrl,
                        ),
                        titleHint = entry.title,
                    )
                }
                repository.enqueuePlaylistDownload(
                    playlistTitle = state.title.ifBlank { info.title },
                    requests = requests,
                ).map { it.firstOrNull().orEmpty() }
            } else if (info.isPlaylist) {
                val playlistTemplate = fileUtils.createOutputTemplateWithDirectory(
                    template = "%(playlist_title)s/%(playlist_index)s - %(title)s [%(id)s].%(ext)s",
                    category = targetCategory,
                )
                repository.enqueueDownload(
                    options = baseOptions.copy(
                        outputTemplate = playlistTemplate,
                        isPlaylistEnabled = true,
                    ),
                    titleHint = state.title.ifBlank { info.title },
                )
            } else {
                repository.enqueueDownload(
                    options = baseOptions,
                    titleHint = state.title.ifBlank { info.title },
                )
            }

            enqueueResult.fold(
                onSuccess = {
                    logger.i("QuickDownloadViewModel", "Queued successfully task id: $it")
                    _uiState.update { it.copy(isQueueing = false, isDownloadSuccess = true) }
                },
                onFailure = { error ->
                    logger.e("QuickDownloadViewModel", "Failed to enqueue download", error)
                    _uiState.update {
                        it.copy(
                            isQueueing = false,
                            errorMessage = error.message?.takeIf { msg -> msg.isNotBlank() } ?: "Failed to start download",
                        )
                    }
                },
            )
        }
    }

    private fun buildVideoFormats(info: VideoInfo): List<QuickFormatOption> {
        val rawVideoFormats = info.formats.filter {
            !it.isAudioOnly && !it.isImageLike && !it.shouldTreatAsAudioOnlyChoice()
        }.ifEmpty {
            info.formats.filter { !it.isImageLike }
        }

        if (rawVideoFormats.isEmpty()) {
            return listOf(
                QuickFormatOption(id = "mp4", label = "MP4", container = "mp4"),
                QuickFormatOption(id = "webm", label = "WebM", container = "webm"),
                QuickFormatOption(id = "mkv", label = "MKV", container = "mkv"),
            )
        }

        val groups = rawVideoFormats.groupBy { format ->
            Pair(normalizeContainer(format), normalizeCodecFamily(format.videoCodec))
        }

        return groups.map { (key, _) ->
            val container = key.first
            val codecFamily = key.second
            val label = if (codecFamily != null) {
                "$codecFamily · ${container.uppercase()}"
            } else {
                container.uppercase()
            }
            val id = if (codecFamily != null) "${container}_${codecFamily.lowercase()}" else container
            QuickFormatOption(
                id = id,
                label = label,
                container = container,
                videoCodec = codecFamily,
            )
        }.sortedWith(
            compareBy<QuickFormatOption> { option ->
                when {
                    option.container == "mp4" && option.videoCodec == "H264" -> 0
                    option.container == "mp4" -> 1
                    option.container == "webm" && option.videoCodec == "VP9" -> 2
                    option.container == "webm" -> 3
                    option.container == "mkv" -> 4
                    else -> 5
                }
            }.thenBy { it.label }
        )
    }

    private fun buildVideoQualities(info: VideoInfo, selectedFormat: QuickFormatOption?): List<QuickQualityOption> {
        val rawVideoFormats = info.formats.filter {
            !it.isAudioOnly && !it.isImageLike && !it.shouldTreatAsAudioOnlyChoice()
        }.ifEmpty {
            info.formats.filter { !it.isImageLike }
        }

        val matchingFormats = if (selectedFormat != null) {
            rawVideoFormats.filter { format ->
                normalizeContainer(format) == selectedFormat.container &&
                    (selectedFormat.videoCodec == null ||
                        normalizeCodecFamily(format.videoCodec) == selectedFormat.videoCodec ||
                        normalizeCodecFamily(format.videoCodec) == null)
            }.ifEmpty {
                rawVideoFormats.filter { normalizeContainer(it) == selectedFormat.container }
            }.ifEmpty {
                rawVideoFormats
            }
        } else {
            rawVideoFormats
        }

        val audioFormats = info.formats.filter { it.isAudioOnly || it.shouldTreatAsAudioOnlyChoice() }
        val prefAudioExts = if (selectedFormat?.container == "mp4") listOf("m4a", "aac", "mp3") else listOf("opus", "webm", "m4a")
        val bestAudio = prefAudioExts.asSequence()
            .mapNotNull { ext -> audioFormats.filter { it.normalizedExtension == ext }.maxByOrNull { it.fileSizeBytes ?: (it.bitrateKbps?.toLong() ?: 0L) } }
            .firstOrNull() ?: audioFormats.maxByOrNull { it.fileSizeBytes ?: (it.bitrateKbps?.toLong() ?: 0L) }
        val bestAudioSize = bestAudio?.fileSizeBytes
        val bestAudioBitrate = bestAudio?.bitrateKbps ?: 128

        val distinctHeights = matchingFormats
            .mapNotNull { parseHeight(it.resolution) }
            .distinct()
            .sortedDescending()

        if (distinctHeights.isNotEmpty()) {
            return distinctHeights.map { height ->
                val matchingForHeight = matchingFormats.filter { parseHeight(it.resolution) == height }
                val bestFormat = matchingForHeight.maxByOrNull { it.fileSizeBytes ?: (it.bitrateKbps?.toLong() ?: 0L) } ?: matchingForHeight.first()

                val videoSize = bestFormat.fileSizeBytes
                val totalSizeBytes = if (videoSize != null) {
                    if (bestFormat.isVideoOnly) videoSize + (bestAudioSize ?: 0L) else videoSize
                } else {
                    val totalBitrate = (bestFormat.bitrateKbps ?: defaultBitrateForHeight(height)) + if (bestFormat.isVideoOnly) bestAudioBitrate else 0
                    estimateFormatSizeBytes(info.durationSeconds, totalBitrate)
                }

                val isExactSize = videoSize != null
                val sizeLabel = totalSizeBytes?.takeIf { it > 0L }?.let { bytes ->
                    if (isExactSize) bytes.toReadableSize() else "~${bytes.toReadableSize()}"
                }

                val fpsSuffix = if (bestFormat.fps != null && bestFormat.fps >= 50.0) " ${bestFormat.fps.toInt()}fps" else ""
                val title = when (height) {
                    2160 -> "4K · 2160p"
                    1440 -> "2K · 1440p"
                    else -> "${height}p"
                } + fpsSuffix

                QuickQualityOption(
                    id = "${height}p_${bestFormat.formatId}",
                    title = title,
                    subtitle = sizeLabel,
                    height = height,
                    bitrateKbps = bestFormat.bitrateKbps,
                    fps = bestFormat.fps,
                    mediaFormat = bestFormat,
                    fileSizeBytes = totalSizeBytes,
                )
            }
        }

        if (matchingFormats.isNotEmpty()) {
            return matchingFormats.distinctBy { it.formatId }.map { format ->
                val sizeBytes = format.fileSizeBytes ?: estimateFormatSizeBytes(info.durationSeconds, format.bitrateKbps)
                val isExact = format.fileSizeBytes != null
                val sizeLabel = sizeBytes?.let { if (isExact) it.toReadableSize() else "~${it.toReadableSize()}" }
                val title = format.resolution ?: format.note ?: format.asReadableLabel()
                QuickQualityOption(
                    id = format.formatId,
                    title = title,
                    subtitle = sizeLabel,
                    bitrateKbps = format.bitrateKbps,
                    fps = format.fps,
                    mediaFormat = format,
                    fileSizeBytes = sizeBytes,
                )
            }
        }

        return listOf(
            QuickQualityOption(id = "1080p", title = "1080p", subtitle = estimateSize(info.durationSeconds, 4500), height = 1080),
            QuickQualityOption(id = "720p", title = "720p", subtitle = estimateSize(info.durationSeconds, 2500), height = 720),
            QuickQualityOption(id = "480p", title = "480p", subtitle = estimateSize(info.durationSeconds, 1200), height = 480),
            QuickQualityOption(id = "360p", title = "360p", subtitle = estimateSize(info.durationSeconds, 800), height = 360),
            QuickQualityOption(id = "240p", title = "240p", subtitle = estimateSize(info.durationSeconds, 400), height = 240),
        )
    }

    private fun buildAudioFormats(info: VideoInfo): List<QuickFormatOption> {
        val audioStreams = info.formats.filter { it.isAudioOnly || it.shouldTreatAsAudioOnlyChoice() }
        val streamExts = audioStreams.map { it.normalizedExtension.ifBlank { "m4a" } }.distinct()

        return buildList {
            if ("m4a" in streamExts || "aac" in streamExts) {
                add(QuickFormatOption(id = "m4a_stream", label = "M4A (Original)", container = "m4a", isOriginalStream = true))
            }
            if ("opus" in streamExts || "webm" in streamExts || "weba" in streamExts) {
                add(QuickFormatOption(id = "opus_stream", label = "Opus (Original)", container = "opus", isOriginalStream = true))
            }
            if ("mp3" in streamExts) {
                add(QuickFormatOption(id = "mp3_stream", label = "MP3 (Original)", container = "mp3", isOriginalStream = true))
            }
            add(QuickFormatOption(id = "mp3", label = "MP3", container = "mp3", isOriginalStream = false))
            if ("m4a" !in streamExts && "aac" !in streamExts) {
                add(QuickFormatOption(id = "m4a", label = "M4A", container = "m4a", isOriginalStream = false))
            }
            add(QuickFormatOption(id = "flac", label = "FLAC (Lossless)", container = "flac", isOriginalStream = false))
            add(QuickFormatOption(id = "wav", label = "WAV (Lossless)", container = "wav", isOriginalStream = false))
            if ("opus" !in streamExts && "webm" !in streamExts) {
                add(QuickFormatOption(id = "opus", label = "Opus", container = "opus", isOriginalStream = false))
            }
        }
    }

    private fun buildAudioQualities(info: VideoInfo, selectedFormat: QuickFormatOption?): List<QuickQualityOption> {
        val audioStreams = info.formats.filter { it.isAudioOnly || it.shouldTreatAsAudioOnlyChoice() }

        if (selectedFormat?.isOriginalStream == true) {
            val matchingAudio = audioStreams.filter {
                it.normalizedExtension == selectedFormat.container ||
                    (selectedFormat.container == "opus" && it.normalizedExtension in listOf("opus", "webm", "weba"))
            }

            if (matchingAudio.isNotEmpty()) {
                return matchingAudio.distinctBy { it.formatId }.map { format ->
                    val size = format.fileSizeBytes ?: estimateFormatSizeBytes(info.durationSeconds, format.bitrateKbps)
                    val isExact = format.fileSizeBytes != null
                    val sizeLabel = size?.let { if (isExact) it.toReadableSize() else "~${it.toReadableSize()}" }
                    val title = if (format.bitrateKbps != null) "${format.bitrateKbps}k" else (format.note ?: "Original Audio")
                    QuickQualityOption(
                        id = "${format.formatId}_audio",
                        title = title,
                        subtitle = sizeLabel,
                        bitrateKbps = format.bitrateKbps,
                        mediaFormat = format,
                        fileSizeBytes = size,
                    )
                }.sortedByDescending { it.bitrateKbps ?: 0 }
            }
        }

        val maxSourceBitrate = audioStreams.mapNotNull { it.bitrateKbps }.maxOrNull() ?: 320
        val standardBitrates = listOf(320, 256, 192, 160, 128, 96, 64)
        val bitrates = standardBitrates.filter { it <= maxSourceBitrate || it == 320 || it == standardBitrates.first() }.distinct()

        return bitrates.map { bitrate ->
            val sizeBytes = estimateFormatSizeBytes(info.durationSeconds, bitrate)
            val sizeLabel = sizeBytes?.let { "~${it.toReadableSize()}" }
            QuickQualityOption(
                id = "${bitrate}k",
                title = "${bitrate}k",
                subtitle = sizeLabel,
                bitrateKbps = bitrate,
                fileSizeBytes = sizeBytes,
            )
        }
    }

    private fun normalizeContainer(format: MediaFormat): String {
        val c = format.normalizedContainer.ifBlank { format.normalizedExtension }.lowercase()
        return when {
            c.contains("mp4") -> "mp4"
            c.contains("webm") -> "webm"
            c.contains("mkv") -> "mkv"
            c.contains("mov") -> "mov"
            c.contains("flv") -> "flv"
            c.contains("3gp") -> "3gp"
            c.contains("ts") -> "ts"
            else -> format.normalizedExtension.ifBlank { "mp4" }
        }
    }

    private fun normalizeCodecFamily(codec: String?): String? {
        val c = codec?.trim()?.lowercase() ?: return null
        return when {
            c.startsWith("avc") || c.startsWith("h264") -> "H264"
            c.startsWith("vp9") || c.startsWith("vp09") -> "VP9"
            c.startsWith("av01") || c.startsWith("av1") -> "AV1"
            c.startsWith("hevc") || c.startsWith("h265") -> "H265"
            c == "none" || c.isBlank() -> null
            else -> c.uppercase()
        }
    }

    private fun defaultBitrateForHeight(height: Int): Int {
        return when {
            height >= 2160 -> 15000
            height >= 1440 -> 8000
            height >= 1080 -> 4500
            height >= 720 -> 2500
            height >= 480 -> 1200
            height >= 360 -> 800
            else -> 400
        }
    }

    private fun estimateFormatSizeBytes(durationSeconds: Long?, bitrateKbps: Int?): Long? {
        val safeDurationSeconds = durationSeconds?.takeIf { it > 0L } ?: return null
        val safeBitrateKbps = bitrateKbps?.takeIf { it > 0 } ?: return null
        return (safeDurationSeconds * safeBitrateKbps * 1_000L) / 8L
    }

    private fun estimateSize(durationSeconds: Long?, bitrateKbps: Int): String? {
        val bytes = estimateFormatSizeBytes(durationSeconds, bitrateKbps) ?: return null
        return "~${bytes.toReadableSize()}"
    }

    private fun parseHeight(resolution: String?): Int? {
        val trimmed = resolution?.trim() ?: return null
        return Regex("""(\d+)x(\d+)""").find(trimmed)?.groupValues?.getOrNull(2)?.toIntOrNull()
            ?: Regex("""(\d+)p""").find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: trimmed.toIntOrNull()
    }

    private fun resolveCookiesPath(url: String, settings: AppSettings): String? {
        if (!settings.cookiesEnabled) return null
        val profile = CookieTextCodec.findBestMatch(settings.cookieProfiles, url)
        return profile?.localFilePath?.takeIf { File(it).exists() }
    }

    private fun formatDurationSeconds(seconds: Long?): String? {
        if (seconds == null || seconds <= 0) return null
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
        }
    }

    private fun extractDomainHost(url: String): String? {
        return runCatching {
            val uri = java.net.URI(url)
            val host = uri.host ?: return null
            host.removePrefix("www.").removePrefix("m.")
        }.getOrNull()
    }
}
