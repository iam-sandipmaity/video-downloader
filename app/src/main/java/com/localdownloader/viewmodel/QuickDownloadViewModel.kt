package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.SettingsStore
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.shouldTreatAsAudioOnlyChoice
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.downloader.FormatSelectorBuilder
import com.localdownloader.ui.model.toReadableSize
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.Logger
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
    val formatChoice: FormatChoice? = null,
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
)

data class QuickDownloadUiState(
    val url: String = "",
    val isAnalyzing: Boolean = true,
    val isQueueing: Boolean = false,
    val isDownloadSuccess: Boolean = false,
    val errorMessage: String? = null,
    val videoInfo: VideoInfo? = null,
    val title: String = "",
    val isAudioMode: Boolean = false,
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
)

@HiltViewModel
class QuickDownloadViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val settingsStore: SettingsStore,
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

            val result = repository.analyzeUrl(
                url = url,
                cookiesPath = cookiesPath,
                userAgent = userAgent,
            )

            result.fold(
                onSuccess = { info ->
                    logger.i("QuickDownloadViewModel", "Analyzed successfully: ${info.title}")
                    val videoQualities = buildVideoQualities(info)
                    val audioQualities = buildAudioQualities(info)
                    val videoFormats = buildVideoFormats(info)
                    val audioFormats = buildAudioFormats()

                    val defaultVideoQuality = videoQualities.firstOrNull { it.height != null && it.height <= 720 }
                        ?: videoQualities.firstOrNull()
                    val defaultAudioQuality = audioQualities.firstOrNull { it.bitrateKbps == 160 || it.bitrateKbps == 128 }
                        ?: audioQualities.firstOrNull()
                    val defaultVideoFormat = videoFormats.firstOrNull { it.container == "mp4" || it.label.contains("MP4") }
                        ?: videoFormats.firstOrNull()
                    val defaultAudioFormat = audioFormats.firstOrNull { it.container == "mp3" }
                        ?: audioFormats.firstOrNull()

                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            errorMessage = null,
                            videoInfo = info,
                            title = info.title,
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

    fun onModeChanged(isAudio: Boolean) {
        _uiState.update { it.copy(isAudioMode = isAudio) }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onVideoQualitySelected(option: QuickQualityOption) {
        _uiState.update { it.copy(selectedVideoQuality = option) }
    }

    fun onAudioQualitySelected(option: QuickQualityOption) {
        _uiState.update { it.copy(selectedAudioQuality = option) }
    }

    fun onVideoFormatSelected(option: QuickFormatOption) {
        _uiState.update { it.copy(selectedVideoFormat = option) }
    }

    fun onAudioFormatSelected(option: QuickFormatOption) {
        _uiState.update { it.copy(selectedAudioFormat = option) }
    }

    fun onThreadsChanged(newThreads: Int) {
        _uiState.update { it.copy(threads = newThreads.coerceIn(1, 16)) }
    }

    fun download() {
        val state = _uiState.value
        val info = state.videoInfo ?: return
        if (state.isQueueing) return

        _uiState.update { it.copy(isQueueing = true, errorMessage = null) }

        viewModelScope.launch {
            val isAudio = state.isAudioMode
            val formatId = if (isAudio) {
                state.selectedAudioQuality?.formatChoice?.selector
                    ?: "ba/b"
            } else {
                val height = state.selectedVideoQuality?.height
                val codec = state.selectedVideoFormat?.videoCodec
                when {
                    state.selectedVideoQuality?.formatChoice != null ->
                        state.selectedVideoQuality.formatChoice.selector
                    height != null && codec != null && codec.contains("vp9", ignoreCase = true) ->
                        "bv*[height<=$height][vcodec^=vp9]+ba/bv*[height<=$height]+ba/b[height<=$height]/b"
                    height != null && codec != null && (codec.contains("avc", ignoreCase = true) || codec.contains("h264", ignoreCase = true)) ->
                        "bv*[height<=$height][vcodec^=avc1]+ba/bv*[height<=$height]+ba/b[height<=$height]/b"
                    height != null ->
                        "bv*[height<=$height]+ba/b[height<=$height]/b"
                    else ->
                        "bv*+ba/b"
                }
            }

            val mergeFormat = if (!isAudio) {
                state.selectedVideoFormat?.container?.takeUnless { it == "auto" }
            } else {
                null
            }

            val cookiesPath = resolveCookiesPath(state.url, state.appSettings)
            val options = DownloadOptions(
                url = state.url,
                formatId = formatId,
                outputTemplate = if (state.title.isNotBlank() && state.title != info.title) {
                    "${state.title.trim()} [%(id)s].%(ext)s"
                } else {
                    state.appSettings.defaultOutputTemplate
                },
                thumbnailUrl = info.thumbnailUrl,
                youtubeCookiesPath = cookiesPath,
                youtubeAuthEnabled = state.appSettings.youtubeAuthConfig.isConfigured(),
                youtubePoToken = state.appSettings.youtubeAuthConfig.buildPoTokenValue(),
                youtubePoTokenClientHint = state.appSettings.youtubeAuthConfig.clientHint,
                mergeOutputFormat = mergeFormat,
                preferredVideoHeight = if (!isAudio) state.selectedVideoQuality?.height else null,
                expectedDurationSeconds = info.durationSeconds,
                extractAudio = isAudio,
                audioFormat = if (isAudio) state.selectedAudioFormat?.container ?: "mp3" else null,
                audioBitrateKbps = if (isAudio) state.selectedAudioQuality?.bitrateKbps ?: 160 else null,
                concurrentFragments = state.threads,
                shouldEmbedMetadata = state.appSettings.autoEmbedMetadata,
                shouldEmbedThumbnail = state.appSettings.autoEmbedThumbnail,
                shouldDownloadSubtitles = state.appSettings.autoDownloadSubtitles,
                shouldEmbedSubtitles = state.appSettings.autoEmbedSubtitles,
            )

            val enqueueResult = repository.enqueueDownload(
                options = options,
                titleHint = state.title.ifBlank { info.title },
            )

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

    private fun buildVideoQualities(info: VideoInfo): List<QuickQualityOption> {
        val videoFormats = info.formats.filter { it.isVideoOnly || (!it.shouldTreatAsAudioOnlyChoice() && parseHeight(it.resolution) != null) }
        val distinctHeights = videoFormats
            .mapNotNull { parseHeight(it.resolution) }
            .distinct()
            .sortedDescending()

        if (distinctHeights.isEmpty()) {
            return listOf(
                QuickQualityOption(id = "1080p", title = "1080p", subtitle = estimateSize(info.durationSeconds, 4500), height = 1080),
                QuickQualityOption(id = "720p", title = "720p", subtitle = estimateSize(info.durationSeconds, 2500), height = 720),
                QuickQualityOption(id = "480p", title = "480p", subtitle = estimateSize(info.durationSeconds, 1200), height = 480),
                QuickQualityOption(id = "360p", title = "360p", subtitle = estimateSize(info.durationSeconds, 800), height = 360),
                QuickQualityOption(id = "240p", title = "240p", subtitle = estimateSize(info.durationSeconds, 400), height = 240),
            )
        }

        return distinctHeights.map { height ->
            val matching = videoFormats.filter { parseHeight(it.resolution) == height }
            val bestMatching = matching.maxByOrNull { it.fileSizeBytes ?: (it.bitrateKbps?.toLong() ?: 0L) }
            val sizeBytes = bestMatching?.fileSizeBytes
                ?: (estimateFormatSizeBytes(info.durationSeconds, bestMatching?.bitrateKbps)
                    ?: estimateFormatSizeBytes(info.durationSeconds, defaultBitrateForHeight(height)))
            val sizeLabel = sizeBytes?.let { "~${it.toReadableSize()}" }
            val title = when (height) {
                2160 -> "4K · 2160p"
                1440 -> "2K · 1440p"
                else -> "${height}p"
            }
            val choice = bestMatching?.let { fmt ->
                FormatChoice(
                    selector = if (fmt.isVideoOnly) FormatSelectorBuilder.buildVideoOnlySelector(fmt) + "+ba" else FormatSelectorBuilder.buildMuxedSelector(fmt),
                    label = title,
                    streamType = StreamType.VIDEO_AUDIO,
                    container = fmt.normalizedExtension,
                    height = height,
                    isMerged = fmt.isVideoOnly,
                    isImageLike = false,
                    fileSizeBytes = sizeBytes,
                    videoCodec = fmt.videoCodec,
                    audioCodec = fmt.audioCodec,
                    bitrateKbps = fmt.bitrateKbps,
                )
            }
            QuickQualityOption(
                id = "${height}p",
                title = title,
                subtitle = sizeLabel,
                height = height,
                bitrateKbps = bestMatching?.bitrateKbps,
                formatChoice = choice,
            )
        }
    }

    private fun buildAudioQualities(info: VideoInfo): List<QuickQualityOption> {
        val standardBitrates = listOf(320, 192, 160, 128, 92, 64)
        return standardBitrates.map { bitrate ->
            val sizeBytes = estimateFormatSizeBytes(info.durationSeconds, bitrate)
            val sizeLabel = sizeBytes?.let { "~${it.toReadableSize()}" }
            QuickQualityOption(
                id = "${bitrate}k",
                title = "${bitrate}k",
                subtitle = sizeLabel,
                bitrateKbps = bitrate,
            )
        }
    }

    private fun buildVideoFormats(info: VideoInfo): List<QuickFormatOption> {
        val videoFormats = info.formats.filter { it.isVideoOnly || !it.shouldTreatAsAudioOnlyChoice() }
        val hasVp9 = videoFormats.any { it.videoCodec.contains("vp9", ignoreCase = true) }
        val hasH264 = videoFormats.any {
            it.videoCodec.contains("avc", ignoreCase = true) ||
                it.videoCodec.contains("h264", ignoreCase = true)
        }
        val hasAv1 = videoFormats.any { it.videoCodec.contains("av1", ignoreCase = true) || it.videoCodec.contains("av01", ignoreCase = true) }

        val list = mutableListOf<QuickFormatOption>()
        if (hasVp9) {
            list += QuickFormatOption(id = "vp9_webm", label = "VP9 · WebM", container = "webm", videoCodec = "vp9")
        }
        if (hasH264) {
            list += QuickFormatOption(id = "h264_mp4", label = "H.264 · MP4", container = "mp4", videoCodec = "h264")
        }
        if (hasAv1) {
            list += QuickFormatOption(id = "av1_mp4", label = "AV1 · MP4", container = "mp4", videoCodec = "av1")
        }

        // Fallbacks if no specific codec matches found
        if (list.isEmpty()) {
            list += QuickFormatOption(id = "mp4", label = "MP4", container = "mp4", videoCodec = "h264")
            list += QuickFormatOption(id = "webm", label = "VP9 · WebM", container = "webm", videoCodec = "vp9")
        }
        list += QuickFormatOption(id = "mkv", label = "MKV", container = "mkv")
        return list
    }

    private fun buildAudioFormats(): List<QuickFormatOption> {
        return listOf(
            QuickFormatOption(id = "mp3", label = "MP3", container = "mp3"),
            QuickFormatOption(id = "m4a", label = "M4A", container = "m4a"),
            QuickFormatOption(id = "opus", label = "Opus", container = "opus"),
            QuickFormatOption(id = "flac", label = "FLAC", container = "flac"),
            QuickFormatOption(id = "wav", label = "WAV", container = "wav"),
            QuickFormatOption(id = "aac", label = "AAC", container = "aac"),
        )
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
}
