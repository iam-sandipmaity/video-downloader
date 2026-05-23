package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.AnalyzedLinkHistoryStore
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.downloader.FormatSelectorBuilder
import com.localdownloader.downloader.YoutubeRequestPlanner
import com.localdownloader.downloader.resolveMergeContainerCompatibility
import com.localdownloader.domain.models.AnalyzedLinkRecord
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.PlaylistDownloadRequest
import com.localdownloader.domain.models.SYSTEM_LANGUAGE_TAG
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.domain.models.YoutubeAuthConfig
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import com.localdownloader.utils.NetworkStatusMonitor
import com.localdownloader.utils.UrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FormatViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val analyzedLinkHistoryStore: AnalyzedLinkHistoryStore,
    private val fileUtils: FileUtils,
    private val urlValidator: UrlValidator,
    private val logger: Logger,
    private val networkStatusMonitor: NetworkStatusMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FormatUiState())
    val uiState: StateFlow<FormatUiState> = _uiState.asStateFlow()

    private fun scopedMessageState(
        state: FormatUiState,
        scope: FormatMessageScope,
        infoMessage: String? = null,
        errorMessage: String? = null,
    ): FormatUiState = state.copy(
        messageScope = scope,
        infoMessage = infoMessage,
        errorMessage = errorMessage,
    )

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                applySettings(settings)
                syncPersistedReadyHistory(settings)
            }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { state ->
            state.copy(urlInput = url, errorMessage = null, infoMessage = null)
        }
    }

    fun clearBrowserState() {
        _uiState.update { state ->
            state.copy(
                urlInput = "",
                isAnalyzing = false,
                isQueueing = false,
                videoInfo = null,
                availableVideoAudioChoices = emptyList(),
                availableVideoOnlyChoices = emptyList(),
                availableAudioOnlyChoices = emptyList(),
                playlistItems = emptyList(),
                selectedFormatSelector = null,
                customFileName = "",
                infoMessage = null,
                errorMessage = null,
                restoringReadyItemUrl = null,
            )
        }
    }

    fun clearAnalyzedResult() {
        uiState.value.videoInfo?.webpageUrl?.let(::removeReadyItem)
    }

    fun removeReadyItem(webpageUrl: String) {
        _uiState.update { state ->
            val removedCurrent = state.videoInfo?.webpageUrl == webpageUrl
            state.copy(
                isAnalyzing = false,
                isQueueing = false,
                videoInfo = if (removedCurrent) null else state.videoInfo,
                availableVideoAudioChoices = if (removedCurrent) emptyList() else state.availableVideoAudioChoices,
                availableVideoOnlyChoices = if (removedCurrent) emptyList() else state.availableVideoOnlyChoices,
                availableAudioOnlyChoices = if (removedCurrent) emptyList() else state.availableAudioOnlyChoices,
                playlistItems = if (removedCurrent) emptyList() else state.playlistItems,
                selectedFormatSelector = if (removedCurrent) null else state.selectedFormatSelector,
                customFileName = if (removedCurrent) "" else state.customFileName,
                readyAnalyzedItems = state.readyAnalyzedItems.filterNot { it.webpageUrl == webpageUrl },
                restoringReadyItemUrl = if (state.restoringReadyItemUrl == webpageUrl) null else state.restoringReadyItemUrl,
                infoMessage = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val settings = uiState.value.appSettings
            if (settings.keepAnalyzedLinkHistory) {
                analyzedLinkHistoryStore.remove(
                    webpageUrl = webpageUrl,
                    retentionDays = settings.analyzedLinkHistoryRetentionDays,
                )
            }
        }
    }

    fun analyzeUrl() {
        val url = uiState.value.urlInput.trim()
        analyzeUrlInternal(url = url, restoreIntoReady = false)
    }

    fun reopenReadyItem(webpageUrl: String) {
        val trimmedUrl = webpageUrl.trim()
        val current = uiState.value
        if (current.videoInfo?.webpageUrl == trimmedUrl) {
            _uiState.update { state ->
                state.copy(
                    urlInput = trimmedUrl,
                    infoMessage = null,
                    errorMessage = null,
                )
            }
            return
        }
        _uiState.update { state ->
            state.copy(
                urlInput = trimmedUrl,
                restoringReadyItemUrl = trimmedUrl,
                infoMessage = null,
                errorMessage = null,
            )
        }
        analyzeUrlInternal(url = trimmedUrl, restoreIntoReady = true)
    }

    private fun analyzeUrlInternal(
        url: String,
        restoreIntoReady: Boolean,
    ) {
        logger.i("FormatViewModel", "Analyze requested for URL: $url")
        val normalizedUrl = urlValidator.normalizeForSecureUse(url)
        if (normalizedUrl == null) {
            logger.w("FormatViewModel", "Rejected analyze request due to invalid URL: $url")
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.BROWSER,
                    errorMessage = "Please enter a valid URL. Only secure HTTPS links are allowed.",
                )
            }
            return
        }
        val secureUrl = normalizedUrl.normalizedUrl
        val upgradeNotice = if (normalizedUrl.upgradedToHttps) {
            "Insecure HTTP was upgraded to HTTPS before analysis."
        } else {
            null
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    urlInput = secureUrl,
                    isAnalyzing = true,
                    errorMessage = null,
                    infoMessage = upgradeNotice,
                    videoInfo = null,
                    availableVideoAudioChoices = emptyList(),
                    availableVideoOnlyChoices = emptyList(),
                    availableAudioOnlyChoices = emptyList(),
                    playlistItems = emptyList(),
                    selectedFormatSelector = null,
                    customFileName = "",
                    restoringReadyItemUrl = if (restoreIntoReady) secureUrl else state.restoringReadyItemUrl,
                )
            }

            val runtimeCookiesPath = resolveRuntimeCookiesPathForUrl(secureUrl)
            val result = runCatching {
                repository.analyzeUrl(
                    url = secureUrl,
                    cookiesPath = runtimeCookiesPath,
                    userAgent = if (uiState.value.cookiesEnabled && uiState.value.cookieUserAgentEnabled) {
                        CookieTextCodec.COOKIE_USER_AGENT
                    } else {
                        null
                    },
                )
            }
                .getOrElse { throwable ->
                    Result.failure(IllegalStateException(throwable.message ?: "Analyze failed", throwable))
                }
            result.fold(
                onSuccess = { info ->
                    logger.i(
                        "FormatViewModel",
                        "Analyze success for URL: $secureUrl, title='${info.title}', formats=${info.formats.size}",
                    )
                    val choiceBundle = buildChoices(info)
                    val selectedSelector = firstSelectorForStreamType(
                        streamType = uiState.value.selectedStreamType,
                        videoAudioChoices = choiceBundle.videoAudioChoices,
                        videoOnlyChoices = choiceBundle.videoOnlyChoices,
                        audioOnlyChoices = choiceBundle.audioOnlyChoices,
                    )
                    _uiState.update { state ->
                        val playlistItems = buildPlaylistItemStates(
                            info = info,
                            streamType = state.selectedStreamType,
                            container = state.selectedContainer,
                            audioFormat = state.selectedAudioFormat,
                            audioBitrateKbps = state.audioBitrateKbps,
                        )
                        state.copy(
                            isAnalyzing = false,
                            messageScope = FormatMessageScope.BROWSER,
                            videoInfo = info,
                            availableVideoAudioChoices = choiceBundle.videoAudioChoices,
                            availableVideoOnlyChoices = choiceBundle.videoOnlyChoices,
                            availableAudioOnlyChoices = choiceBundle.audioOnlyChoices,
                            playlistItems = playlistItems,
                            selectedFormatSelector = selectedSelector,
                            customFileName = info.title,
                            enablePlaylist = state.enablePlaylist || info.isPlaylist,
                            readyAnalyzedItems = upsertReadyRecord(
                                state.readyAnalyzedItems,
                                buildReadyRecord(info),
                            ),
                            restoringReadyItemUrl = null,
                            infoMessage = listOfNotNull(
                                upgradeNotice,
                                when {
                                    info.isPlaylist -> {
                                        val itemCount = info.playlistCount ?: info.playlistEntries.size
                                        "Playlist ready: $itemCount items will queue one by one."
                                    }
                                    else -> "Found ${info.formats.size} formats."
                                },
                            ).joinToString(" "),
                        )
                    }
                    persistReadyRecordIfNeeded(info)
                },
                onFailure = { error ->
                    logger.e("FormatViewModel", "Analyze failed for URL: $secureUrl", error)
                    val baseMessage = error.message?.takeIf { it.isNotBlank() } ?: "Failed to analyze URL."
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            messageScope = FormatMessageScope.BROWSER,
                            errorMessage = buildString {
                                append(baseMessage)
                                if (shouldShowRuntimeHint(baseMessage)) {
                                    append(" Ensure yt-dlp runtime is initialized and this device ABI is supported.")
                                }
                            },
                            restoringReadyItemUrl = null,
                        )
                    }
                },
            )
        }
    }

    fun onQualityChanged(quality: VideoQuality) {
        val currentState = uiState.value
        val shouldReenable = currentState.isDownloadButtonDisabled && currentState.lastQueuedQuality != quality
        _uiState.update { state -> 
            state.copy(
                selectedQuality = quality,
                isDownloadButtonDisabled = if (shouldReenable) false else state.isDownloadButtonDisabled,
                downloadButtonDisabledAt = if (shouldReenable) 0L else state.downloadButtonDisabledAt
            )
        }
    }

    fun onStreamTypeChanged(streamType: StreamType) {
        val currentState = uiState.value
        // Check if this change should re-enable the download button
        val shouldReenable = currentState.isDownloadButtonDisabled && 
            (currentState.lastQueuedStreamType != streamType ||
             currentState.lastQueuedContainer != currentState.selectedContainer ||
             currentState.lastQueuedAudioFormat != currentState.selectedAudioFormat ||
             currentState.lastQueuedAudioBitrate != currentState.audioBitrateKbps ||
             currentState.lastQueuedQuality != currentState.selectedQuality)
        
        _uiState.update { state ->
            val selector = firstSelectorForStreamType(
                streamType = streamType,
                videoAudioChoices = state.availableVideoAudioChoices,
                videoOnlyChoices = state.availableVideoOnlyChoices,
                audioOnlyChoices = state.availableAudioOnlyChoices,
            )
            state.copy(
                selectedStreamType = streamType, 
                selectedFormatSelector = selector,
                isDownloadButtonDisabled = if (shouldReenable) false else state.isDownloadButtonDisabled,
                downloadButtonDisabledAt = if (shouldReenable) 0L else state.downloadButtonDisabledAt
            )
        }
    }

    fun onFormatSelectorChanged(selector: String) {
        _uiState.update { state -> state.copy(selectedFormatSelector = selector) }
    }

    fun onContainerChanged(container: String) {
        val currentState = uiState.value
        val shouldReenable = currentState.isDownloadButtonDisabled && currentState.lastQueuedContainer != container.lowercase()
        _uiState.update { state -> 
            state.copy(
                selectedContainer = container.lowercase(),
                isDownloadButtonDisabled = if (shouldReenable) false else state.isDownloadButtonDisabled,
                downloadButtonDisabledAt = if (shouldReenable) 0L else state.downloadButtonDisabledAt
            )
        }
    }

    fun onAudioFormatChanged(value: String) {
        val currentState = uiState.value
        val shouldReenable = currentState.isDownloadButtonDisabled && currentState.lastQueuedAudioFormat != value.lowercase()
        _uiState.update { state -> 
            state.copy(
                selectedAudioFormat = value.lowercase(),
                isDownloadButtonDisabled = if (shouldReenable) false else state.isDownloadButtonDisabled,
                downloadButtonDisabledAt = if (shouldReenable) 0L else state.downloadButtonDisabledAt
            )
        }
    }

    fun onAudioBitrateChanged(value: Int) {
        val currentState = uiState.value
        val shouldReenable = currentState.isDownloadButtonDisabled && currentState.lastQueuedAudioBitrate != value.coerceAtLeast(64)
        _uiState.update { state -> 
            state.copy(
                audioBitrateKbps = value.coerceAtLeast(64),
                isDownloadButtonDisabled = if (shouldReenable) false else state.isDownloadButtonDisabled,
                downloadButtonDisabledAt = if (shouldReenable) 0L else state.downloadButtonDisabledAt
            )
        }
    }

    fun onDownloadSubtitlesChanged(value: Boolean) {
        _uiState.update { state ->
            state.copy(
                downloadSubtitles = value,
                embedSubtitles = if (value) state.embedSubtitles else false,
            )
        }
    }

    fun onEmbedSubtitlesChanged(value: Boolean) {
        _uiState.update { state ->
            state.copy(
                embedSubtitles = value,
                downloadSubtitles = if (value) true else state.downloadSubtitles,
            )
        }
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

    fun onAutoRemoveMissingFilesFromLibraryChanged(value: Boolean) {
        _uiState.update { state -> state.copy(autoRemoveMissingFilesFromLibrary = value) }
        persistSettingsSilently()
    }

    fun onDeleteFromStorageWhenRemovedInAppChanged(value: Boolean) {
        _uiState.update { state -> state.copy(deleteFromStorageWhenRemovedInApp = value) }
        persistSettingsSilently()
    }

    fun onNotifyCompletedDownloadsChanged(value: Boolean) {
        _uiState.update { state -> state.copy(notifyCompletedDownloads = value) }
        persistSettingsSilently()
    }

    fun onNotifyDownloadErrorsChanged(value: Boolean) {
        _uiState.update { state -> state.copy(notifyDownloadErrors = value) }
        persistSettingsSilently()
    }

    fun onNotifyCanceledDownloadsChanged(value: Boolean) {
        _uiState.update { state -> state.copy(notifyCanceledDownloads = value) }
        persistSettingsSilently()
    }

    fun onNotifyPromotionsChanged(value: Boolean) {
        _uiState.update { state -> state.copy(notifyPromotions = value) }
        persistSettingsSilently()
    }

    fun onPlaylistEnabledChanged(value: Boolean) {
        _uiState.update { state -> state.copy(enablePlaylist = value) }
    }

    fun onPlaylistSelectAllChanged(value: Boolean) {
        _uiState.update { state ->
            state.copy(
                playlistItems = state.playlistItems.map { item ->
                    item.copy(isSelected = value)
                },
            )
        }
    }

    fun onPlaylistItemSelectedChanged(index: Int, value: Boolean) {
        updatePlaylistItem(index) { item -> item.copy(isSelected = value) }
    }

    fun onPlaylistItemExpandedChanged(index: Int, value: Boolean) {
        updatePlaylistItem(index) { item -> item.copy(isExpanded = value) }
    }

    fun onPlaylistItemUseGlobalChanged(index: Int, value: Boolean) {
        val state = uiState.value
        val item = state.playlistItems.getOrNull(index) ?: return
        updatePlaylistItem(index) {
            if (value) {
                item.copy(useGlobalSettings = true)
            } else {
                seedPlaylistOverrideFromGlobal(item = item, state = state).copy(useGlobalSettings = false)
            }
        }
    }

    fun onPlaylistItemStreamTypeChanged(index: Int, streamType: StreamType) {
        updatePlaylistItem(index) { item ->
            item.copy(
                useGlobalSettings = false,
                selectedStreamType = streamType,
                selectedFormatSelector = firstSelectorForStreamType(
                    streamType = streamType,
                    videoAudioChoices = item.availableVideoAudioChoices,
                    videoOnlyChoices = item.availableVideoOnlyChoices,
                    audioOnlyChoices = item.availableAudioOnlyChoices,
                ),
            )
        }
    }

    fun onPlaylistItemFormatSelectorChanged(index: Int, selector: String) {
        updatePlaylistItem(index) { item ->
            item.copy(
                useGlobalSettings = false,
                selectedFormatSelector = selector,
            )
        }
    }

    fun onPlaylistItemContainerChanged(index: Int, container: String) {
        updatePlaylistItem(index) { item ->
            item.copy(
                useGlobalSettings = false,
                selectedContainer = container.lowercase(),
            )
        }
    }

    fun onPlaylistItemAudioFormatChanged(index: Int, value: String) {
        updatePlaylistItem(index) { item ->
            item.copy(
                useGlobalSettings = false,
                selectedAudioFormat = value.lowercase(),
            )
        }
    }

    fun onPlaylistItemAudioBitrateChanged(index: Int, value: Int) {
        updatePlaylistItem(index) { item ->
            item.copy(
                useGlobalSettings = false,
                audioBitrateKbps = value.coerceAtLeast(64),
            )
        }
    }

    fun onOutputTemplateChanged(value: String) {
        _uiState.update { state -> state.copy(outputTemplate = value) }
    }

    fun onAudioOutputTemplateChanged(value: String) {
        _uiState.update { state -> state.copy(audioOutputTemplate = value) }
    }

    fun onCustomFileNameChanged(value: String) {
        _uiState.update { state -> state.copy(customFileName = value) }
    }

    fun onPlaylistItemFileNameChanged(index: Int, value: String) {
        updatePlaylistItem(index) { item -> item.copy(customFileName = value) }
    }

    fun onDefaultVideoOutputTemplateChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                outputTemplate = normalizeTemplateValue(
                    value = value,
                    fallback = AppSettings().defaultOutputTemplate,
                ),
            )
        }
        persistSettingsSilently()
    }

    fun onDefaultAudioOutputTemplateChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                audioOutputTemplate = normalizeTemplateValue(
                    value = value,
                    fallback = AppSettings().defaultAudioOutputTemplate,
                ),
            )
        }
        persistSettingsSilently()
    }

    fun onDefaultVideoContainerChanged(container: String) {
        onContainerChanged(container)
        persistSettingsSilently()
    }

    fun onDefaultAudioFormatChanged(value: String) {
        onAudioFormatChanged(value)
        persistSettingsSilently()
    }

    fun onDefaultDownloadSubtitlesChanged(value: Boolean) {
        _uiState.update { state ->
            state.copy(
                downloadSubtitles = value,
                embedSubtitles = if (value) state.embedSubtitles else false,
            )
        }
        persistSettingsSilently()
    }

    fun onDefaultEmbedSubtitlesChanged(value: Boolean) {
        _uiState.update { state ->
            state.copy(
                embedSubtitles = value,
                downloadSubtitles = state.downloadSubtitles || value,
            )
        }
        persistSettingsSilently()
    }

    fun onDefaultEmbedMetadataChanged(value: Boolean) {
        _uiState.update { state -> state.copy(embedMetadata = value) }
        persistSettingsSilently()
    }

    fun onDefaultEmbedThumbnailChanged(value: Boolean) {
        _uiState.update { state -> state.copy(embedThumbnail = value) }
        persistSettingsSilently()
    }

    fun onLanguageChanged(value: String) {
        _uiState.update { state -> state.copy(languageTag = value.ifBlank { SYSTEM_LANGUAGE_TAG }) }
        persistSettingsSilently()
    }

    fun onThemeModeChanged(value: ThemeMode) {
        _uiState.update { state ->
            state.copy(
                themeMode = value,
                isDarkTheme = when (value) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> state.isDarkTheme
                },
            )
        }
        persistSettingsSilently()
    }

    fun onAccentPresetChanged(value: AccentPreset) {
        _uiState.update { state -> state.copy(accentPreset = value) }
        persistSettingsSilently()
    }

    fun onContrastModeChanged(value: ContrastMode) {
        _uiState.update { state -> state.copy(contrastMode = value) }
        persistSettingsSilently()
    }

    fun onDownloadsRootFolderNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(downloadsRootFolderName = fileUtils.normalizeDownloadsRootSetting(value))
        }
        persistSettingsSilently()
    }

    fun onVideoSubfolderNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(videoSubfolderName = fileUtils.normalizeSubfolderSetting(value))
        }
        persistSettingsSilently()
    }

    fun onAudioSubfolderNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(audioSubfolderName = fileUtils.normalizeSubfolderSetting(value))
        }
        persistSettingsSilently()
    }

    fun onOtherSubfolderNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(otherSubfolderName = fileUtils.normalizeSubfolderSetting(value))
        }
        persistSettingsSilently()
    }

    fun onMaxConcurrentDownloadsChanged(value: Int) {
        _uiState.update { state ->
            state.copy(maxConcurrentDownloads = value.coerceIn(1, 4))
        }
        persistSettingsSilently()
    }

    fun onKeepAnalyzedLinkHistoryChanged(value: Boolean) {
        _uiState.update { state -> state.copy(appSettings = state.appSettings.copy(keepAnalyzedLinkHistory = value)) }
        persistSettingsSilently()
        if (!value) {
            viewModelScope.launch { analyzedLinkHistoryStore.clear() }
        } else {
            viewModelScope.launch {
                val state = uiState.value
                val persisted = analyzedLinkHistoryStore.replaceAll(
                    records = state.readyAnalyzedItems,
                    retentionDays = state.appSettings.analyzedLinkHistoryRetentionDays,
                )
                _uiState.update { current ->
                    current.copy(readyAnalyzedItems = upsertReadyRecords(current.readyAnalyzedItems, persisted))
                }
            }
        }
    }

    fun onAnalyzedLinkHistoryRetentionDaysChanged(value: Int) {
        val normalized = value.coerceAtLeast(1)
        _uiState.update { state ->
            state.copy(
                appSettings = state.appSettings.copy(analyzedLinkHistoryRetentionDays = normalized),
                readyAnalyzedItems = pruneReadyHistory(state.readyAnalyzedItems, normalized),
            )
        }
        persistSettingsSilently()
        if (uiState.value.appSettings.keepAnalyzedLinkHistory) {
            viewModelScope.launch {
                val state = uiState.value
                val persisted = analyzedLinkHistoryStore.replaceAll(
                    records = state.readyAnalyzedItems,
                    retentionDays = normalized,
                )
                _uiState.update { current ->
                    current.copy(readyAnalyzedItems = upsertReadyRecords(current.readyAnalyzedItems, persisted))
                }
            }
        }
    }

    fun onAllowMeteredDownloadsChanged(value: Boolean) {
        _uiState.update { state -> state.copy(allowMeteredDownloads = value) }
        persistSettingsSilently()
    }

    fun onCookiesEnabledChanged(value: Boolean) {
        _uiState.update { state -> state.copy(cookiesEnabled = value) }
        persistSettings("Cookie preference saved.", FormatMessageScope.COOKIES)
    }

    fun onCookieUserAgentEnabledChanged(value: Boolean) {
        _uiState.update { state -> state.copy(cookieUserAgentEnabled = value) }
        persistSettings("Cookie header preference saved.", FormatMessageScope.COOKIES)
    }

    fun importCookieText(rawText: String) {
        val inferredUrl = CookieTextCodec.inferUrl(rawText)
        if (inferredUrl.isNullOrBlank()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.COOKIES,
                    errorMessage = "Could not detect a site URL from the copied cookie text.",
                )
            }
            return
        }
        saveCookieProfile(
            profileId = null,
            url = inferredUrl,
            cookiesText = rawText,
            successMessage = "Cookie imported.",
        )
    }

    fun saveCookieProfile(
        profileId: String?,
        url: String,
        cookiesText: String,
        successMessage: String = "Cookie saved.",
    ) {
        val normalizedUrl = CookieTextCodec.normalizeUrl(url)
        if (normalizedUrl.isNullOrBlank()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.COOKIES,
                    errorMessage = "Enter a valid website URL for this cookie.",
                )
            }
            return
        }
        if (cookiesText.isBlank()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.COOKIES,
                    errorMessage = "Cookie text cannot be empty.",
                )
            }
            return
        }

        viewModelScope.launch {
            val cookieId = profileId ?: UUID.randomUUID().toString()
            val storedText = CookieTextCodec.buildStoredText(normalizedUrl, cookiesText)
            runCatching {
                val localPath = fileUtils.writeTextToInternalFile(
                    subDirectoryName = "cookies",
                    targetFileName = "cookie-$cookieId.txt",
                    content = storedText,
                )
                val updatedProfile = CookieProfile(
                    id = cookieId,
                    url = normalizedUrl,
                    cookiesText = storedText,
                    localFilePath = localPath,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
                val current = uiState.value
                val updatedProfiles = current.cookieProfiles
                    .filterNot { it.id == cookieId }
                    .plus(updatedProfile)
                    .sortedByDescending { it.updatedAtEpochMs }
                val newSettings = current.appSettings.copy(
                    cookiesEnabled = true,
                    cookieProfiles = updatedProfiles,
                    cookieUserAgentEnabled = current.cookieUserAgentEnabled,
                )
                repository.updateSettings(newSettings)
                _uiState.update { state ->
                    state.copy(
                        appSettings = newSettings,
                        cookiesEnabled = true,
                        cookieProfiles = updatedProfiles,
                        messageScope = FormatMessageScope.COOKIES,
                        infoMessage = successMessage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    scopedMessageState(
                        state = state,
                        scope = FormatMessageScope.COOKIES,
                        errorMessage = error.message ?: "Unable to save cookie.",
                    )
                }
            }
        }
    }

    fun deleteCookieProfile(profileId: String) {
        viewModelScope.launch {
            runCatching {
                val current = uiState.value
                val profilePath = current.cookieProfiles
                    .firstOrNull { it.id == profileId }
                    ?.localFilePath
                    ?.takeIf { it.isNotBlank() }
                val updatedProfiles = current.cookieProfiles.filterNot { it.id == profileId }
                val newSettings = current.appSettings.copy(cookieProfiles = updatedProfiles)
                repository.updateSettings(newSettings)
                profilePath?.let(fileUtils::deleteManagedFile)
                _uiState.update { state ->
                    state.copy(
                        appSettings = newSettings,
                        cookieProfiles = updatedProfiles,
                        messageScope = FormatMessageScope.COOKIES,
                        infoMessage = "Cookie removed.",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    scopedMessageState(
                        state = state,
                        scope = FormatMessageScope.COOKIES,
                        errorMessage = error.message ?: "Unable to remove cookie.",
                    )
                }
            }
        }
    }

    fun clearAllCookieProfiles() {
        viewModelScope.launch {
            runCatching {
                val current = uiState.value
                val profilePaths = current.cookieProfiles
                    .mapNotNull { profile -> profile.localFilePath.takeIf { it.isNotBlank() } }
                val newSettings = current.appSettings.copy(cookieProfiles = emptyList())
                repository.updateSettings(newSettings)
                profilePaths.forEach(fileUtils::deleteManagedFile)
                _uiState.update { state ->
                    state.copy(
                        appSettings = newSettings,
                        cookieProfiles = emptyList(),
                        messageScope = FormatMessageScope.COOKIES,
                        infoMessage = "All cookies removed.",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    scopedMessageState(
                        state = state,
                        scope = FormatMessageScope.COOKIES,
                        errorMessage = error.message ?: "Unable to clear cookies.",
                    )
                }
            }
        }
    }

    fun replaceCookieFromBrowser(
        profileId: String?,
        url: String,
        cookieText: String,
    ) {
        val normalizedUrl = CookieTextCodec.normalizeUrl(url)
        if (normalizedUrl.isNullOrBlank()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.COOKIES,
                    errorMessage = "That site URL could not be used for cookies.",
                )
            }
            return
        }
        if (cookieText.isBlank()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.COOKIES,
                    errorMessage = "No cookies were captured from that page yet.",
                )
            }
            return
        }
        saveCookieProfile(
            profileId = profileId,
            url = normalizedUrl,
            cookiesText = if (cookieText.trimStart().startsWith("# Netscape HTTP Cookie File")) {
                CookieTextCodec.buildStoredText(normalizedUrl, cookieText)
            } else {
                CookieTextCodec.fromCookieHeader(normalizedUrl, cookieText)
            },
            successMessage = "Cookies updated from browser.",
        )
    }

    fun saveYoutubeAuthSession(
        cookieText: String,
        authConfig: YoutubeAuthConfig,
    ) {
        if (cookieText.isBlank()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.YOUTUBE_ACCESS,
                    errorMessage = "No YouTube cookies were captured from the login page.",
                )
            }
            return
        }
        if (!authConfig.isConfigured()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.YOUTUBE_ACCESS,
                    errorMessage = "No YouTube session hints were captured yet. Let the sample video load, then try Save Access again.",
                )
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                val current = uiState.value
                val updatedProfiles = upsertYoutubeCookieProfile(
                    existingProfiles = current.cookieProfiles,
                    cookieText = cookieText,
                )
                val savedConfig = authConfig.copy(
                    enabled = true,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
                val newSettings = current.appSettings.copy(
                    cookiesEnabled = true,
                    cookieProfiles = updatedProfiles,
                    youtubeAuthConfig = savedConfig,
                )
                repository.updateSettings(newSettings)
                _uiState.update { state ->
                    state.copy(
                        appSettings = newSettings,
                        cookiesEnabled = true,
                        cookieProfiles = updatedProfiles,
                        youtubeAuthConfig = savedConfig,
                        messageScope = FormatMessageScope.YOUTUBE_ACCESS,
                        infoMessage = "YouTube access saved. Long-form downloads can now retry with your account session.",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    scopedMessageState(
                        state = state,
                        scope = FormatMessageScope.YOUTUBE_ACCESS,
                        errorMessage = error.message ?: "Unable to save YouTube access.",
                    )
                }
            }
        }
    }

    fun setYoutubeAuthEnabled(enabled: Boolean) {
        val current = uiState.value.youtubeAuthConfig
        if (enabled && !current.isConfigured()) {
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.YOUTUBE_ACCESS,
                    errorMessage = "Save YouTube access first, then turn it on.",
                )
            }
            return
        }
        _uiState.update { state ->
            state.copy(
                youtubeAuthConfig = state.youtubeAuthConfig.copy(enabled = enabled),
            )
        }
        persistSettings(
            if (enabled) {
                "YouTube access enabled."
            } else {
                "YouTube access disabled."
            },
            FormatMessageScope.YOUTUBE_ACCESS,
        )
    }

    fun clearYoutubeAuthConfig() {
        viewModelScope.launch {
            runCatching {
                val current = uiState.value
                val newSettings = current.appSettings.copy(
                    youtubeAuthConfig = YoutubeAuthConfig(),
                )
                repository.updateSettings(newSettings)
                _uiState.update { state ->
                    state.copy(
                        appSettings = newSettings,
                        youtubeAuthConfig = YoutubeAuthConfig(),
                        messageScope = FormatMessageScope.YOUTUBE_ACCESS,
                        infoMessage = "Saved YouTube access was cleared.",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    scopedMessageState(
                        state = state,
                        scope = FormatMessageScope.YOUTUBE_ACCESS,
                        errorMessage = error.message ?: "Unable to clear YouTube access.",
                    )
                }
            }
        }
    }

    fun saveSettings() {
        persistSettings("Settings saved locally.", FormatMessageScope.SETTINGS)
    }

    fun dismissDownloadSetupNotice() {
        val current = uiState.value
        if (current.appSettings.hasSeenDownloadSetupNotice) return

        val updatedSettings = current.appSettings.copy(hasSeenDownloadSetupNotice = true)
        _uiState.update { state -> state.copy(appSettings = updatedSettings) }

        viewModelScope.launch {
            runCatching { repository.updateSettings(updatedSettings) }
                .onFailure { error ->
                    _uiState.update { state ->
                        scopedMessageState(
                            state = state,
                            scope = FormatMessageScope.BROWSER,
                            errorMessage = error.message ?: "Unable to save the setup reminder state.",
                        )
                    }
                }
        }
    }

    fun resetSettingsToDefaults() {
        val defaults = AppSettings()
        _uiState.update { state ->
            state.copy(
                appSettings = defaults,
                selectedContainer = defaults.defaultMergeContainer,
                selectedAudioFormat = defaults.defaultAudioFormat,
                outputTemplate = defaults.defaultOutputTemplate,
                audioOutputTemplate = defaults.defaultAudioOutputTemplate,
                downloadSubtitles = defaults.autoDownloadSubtitles,
                embedSubtitles = defaults.autoEmbedSubtitles,
                embedMetadata = defaults.autoEmbedMetadata,
                embedThumbnail = defaults.autoEmbedThumbnail,
                autoRemoveMissingFilesFromLibrary = defaults.autoRemoveMissingFilesFromLibrary,
                deleteFromStorageWhenRemovedInApp = defaults.deleteFromStorageWhenRemovedInApp,
                cookiesEnabled = defaults.cookiesEnabled,
                cookieUserAgentEnabled = defaults.cookieUserAgentEnabled,
                cookieProfiles = defaults.cookieProfiles,
                youtubeAuthConfig = defaults.youtubeAuthConfig,
                notifyCompletedDownloads = defaults.notifyCompletedDownloads,
                notifyDownloadErrors = defaults.notifyDownloadErrors,
                notifyCanceledDownloads = defaults.notifyCanceledDownloads,
                notifyPromotions = defaults.notifyPromotions,
                hasLoadedSettings = true,
                languageTag = defaults.languageTag,
                themeMode = defaults.themeMode,
                accentPreset = defaults.accentPreset,
                contrastMode = defaults.contrastMode,
                downloadsRootFolderName = defaults.downloadsRootFolderName,
                videoSubfolderName = defaults.videoSubfolderName,
                audioSubfolderName = defaults.audioSubfolderName,
                otherSubfolderName = defaults.otherSubfolderName,
                maxConcurrentDownloads = defaults.maxConcurrentDownloads,
                allowMeteredDownloads = defaults.allowMeteredDownloads,
                isDarkTheme = false,
                infoMessage = null,
                errorMessage = null,
                showMeteredNetworkDialog = false,
            )
        }
        persistSettings("Settings reset to defaults.", FormatMessageScope.SETTINGS)
    }

    private fun persistSettings(
        successMessage: String,
        scope: FormatMessageScope = FormatMessageScope.SETTINGS,
    ) {
        persistSettingsInternal(
            successMessage = successMessage,
            clearSuccessIfSilent = false,
            scope = scope,
        )
    }

    private fun persistSettingsSilently() {
        persistSettingsInternal(
            successMessage = null,
            clearSuccessIfSilent = true,
            scope = FormatMessageScope.SETTINGS,
        )
    }

    private fun persistSettingsInternal(
        successMessage: String?,
        clearSuccessIfSilent: Boolean,
        scope: FormatMessageScope,
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val newSettings = state.appSettings.copy(
                languageTag = state.languageTag,
                themeMode = state.themeMode,
                accentPreset = state.accentPreset,
                contrastMode = state.contrastMode,
                defaultOutputTemplate = state.outputTemplate,
                defaultAudioOutputTemplate = state.audioOutputTemplate,
                defaultMergeContainer = state.selectedContainer,
                defaultAudioFormat = state.selectedAudioFormat,
                downloadsRootFolderName = state.downloadsRootFolderName,
                videoSubfolderName = state.videoSubfolderName,
                audioSubfolderName = state.audioSubfolderName,
                otherSubfolderName = state.otherSubfolderName,
                autoDownloadSubtitles = state.downloadSubtitles,
                autoEmbedSubtitles = state.embedSubtitles,
                autoEmbedMetadata = state.embedMetadata,
                autoEmbedThumbnail = state.embedThumbnail,
                autoRemoveMissingFilesFromLibrary = state.autoRemoveMissingFilesFromLibrary,
                deleteFromStorageWhenRemovedInApp = state.deleteFromStorageWhenRemovedInApp,
                maxConcurrentDownloads = state.maxConcurrentDownloads,
                allowMeteredDownloads = state.allowMeteredDownloads,
                cookiesEnabled = state.cookiesEnabled,
                cookieUserAgentEnabled = state.cookieUserAgentEnabled,
                cookieProfiles = state.cookieProfiles,
                youtubeAuthConfig = state.youtubeAuthConfig,
                notifyCompletedDownloads = state.notifyCompletedDownloads,
                notifyDownloadErrors = state.notifyDownloadErrors,
                notifyCanceledDownloads = state.notifyCanceledDownloads,
                notifyPromotions = state.notifyPromotions,
                keepAnalyzedLinkHistory = state.appSettings.keepAnalyzedLinkHistory,
                analyzedLinkHistoryRetentionDays = state.appSettings.analyzedLinkHistoryRetentionDays,
                hasSeenDownloadSetupNotice = state.appSettings.hasSeenDownloadSetupNotice,
                darkTheme = state.isDarkTheme,
            )
            runCatching { repository.updateSettings(newSettings) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            messageScope = scope,
                            infoMessage = successMessage ?: if (clearSuccessIfSilent) null else it.infoMessage,
                            appSettings = newSettings,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        scopedMessageState(
                            state = it,
                            scope = scope,
                            errorMessage = error.message ?: "Failed to save settings.",
                        )
                    }
                }
        }
    }

    fun queueDownload() {
        if (uiState.value.videoInfo == null) {
            viewModelScope.launch {
                enqueueCurrentDownload()
            }
            return
        }

        if (shouldPromptForMeteredNetwork()) {
            _uiState.update { state ->
                state.copy(
                    showMeteredNetworkDialog = true,
                    messageScope = FormatMessageScope.BROWSER,
                    infoMessage = null,
                    errorMessage = null,
                )
            }
            return
        }

        viewModelScope.launch {
            enqueueCurrentDownload()
        }
    }

    fun dismissMeteredNetworkDialog() {
        _uiState.update { state -> state.copy(showMeteredNetworkDialog = false) }
    }

    fun queueDownloadWhenWifiAvailable() {
        _uiState.update { state -> state.copy(showMeteredNetworkDialog = false) }
        viewModelScope.launch {
            enqueueCurrentDownload(
                queuedMessageSuffix = "It will start automatically when Wi-Fi is available.",
            )
        }
    }

    fun allowCellularDownloadsAndQueue() {
        viewModelScope.launch {
            val current = uiState.value
            val previousSettings = current.appSettings
            val updatedSettings = previousSettings.copy(allowMeteredDownloads = true)
            _uiState.update { state ->
                state.copy(
                    allowMeteredDownloads = true,
                    appSettings = updatedSettings,
                    showMeteredNetworkDialog = false,
                )
            }

            val persisted = runCatching { repository.updateSettings(updatedSettings) }
            if (persisted.isFailure) {
                _uiState.update { state ->
                    scopedMessageState(
                        state = state.copy(
                            allowMeteredDownloads = previousSettings.allowMeteredDownloads,
                            appSettings = previousSettings,
                            showMeteredNetworkDialog = false,
                        ),
                        scope = FormatMessageScope.BROWSER,
                        errorMessage = persisted.exceptionOrNull()?.message ?: "Unable to update the download network setting.",
                    )
                }
                return@launch
            }

            enqueueCurrentDownload()
        }
    }

    private suspend fun enqueueCurrentDownload(
        queuedMessageSuffix: String? = null,
    ) {
        val state = uiState.value
        val info = state.videoInfo
        logger.i(
            "FormatViewModel",
            "Queue requested. hasVideoInfo=${info != null}, quality=${state.selectedQuality}, streamType=${state.selectedStreamType}",
        )
        if (info == null) {
            logger.w("FormatViewModel", "Queue rejected: analyze missing")
            _uiState.update { current ->
                scopedMessageState(
                    state = current,
                    scope = FormatMessageScope.BROWSER,
                    errorMessage = "Analyze a URL first.",
                )
            }
            return
        }
        if (info.isPlaylist && state.selectedPlaylistItemCount == 0) {
            _uiState.update { current ->
                scopedMessageState(
                    state = current,
                    scope = FormatMessageScope.BROWSER,
                    errorMessage = "Select at least one playlist item first.",
                )
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                isQueueing = true,
                errorMessage = null,
                infoMessage = null,
                showMeteredNetworkDialog = false,
            )
        }

        val (downloadExtractorArgs, fallbackExtractorArgs) = resolveDownloadExtractorArgs(
            info = info,
            cookiesAvailable = resolveRuntimeCookiesPathForUrl(info.webpageUrl) != null,
        )
        val youtubeAuthConfig = state.youtubeAuthConfig.takeIf { it.enabled && it.isConfigured() }

        fun buildOptionsForSelection(
            sourceUrl: String,
            sourceThumbnailUrl: String?,
            sourceDurationSeconds: Long?,
            streamType: StreamType,
            selectedSelector: String?,
            container: String,
            audioFormat: String,
            audioBitrateKbps: Int,
            customFileName: String,
            choiceBundle: ChoiceBundle,
            targetCategory: FileUtils.MediaFolderCategory,
        ): BuiltSelectionOptions {
            val selectedChoice = resolveSelectedChoice(
                streamType = streamType,
                selectedSelector = selectedSelector,
                choiceBundle = choiceBundle,
            )
            val formatSelector = when {
                selectedChoice != null -> selectedChoice.selector
                isYoutubeUrl(sourceUrl) -> {
                    val boundedHeight = state.selectedQuality.maxHeight?.let { "[height<=$it]" }.orEmpty()
                    when (streamType) {
                        StreamType.AUDIO_ONLY -> "bestaudio/best"
                        StreamType.VIDEO_ONLY -> "bestvideo$boundedHeight/bestvideo"
                        StreamType.VIDEO_AUDIO -> "bestvideo$boundedHeight+bestaudio/best$boundedHeight/best"
                    }
                }
                else -> buildFormatSelector(
                    quality = state.selectedQuality,
                    streamType = streamType,
                    container = container,
                )
            }
            val isAudioOnly = streamType == StreamType.AUDIO_ONLY
            val shouldBypassMediaPostProcessing = !info.isPlaylist && selectedChoice?.isImageLike == true
            val runtimeCookiesPath = resolveRuntimeCookiesPathForUrl(sourceUrl)
            val mergeContainer = when {
                isAudioOnly -> null
                selectedChoice == null -> container.ifBlank { null }
                selectedChoice.streamType == StreamType.VIDEO_AUDIO && selectedChoice.selector.contains("+") ->
                    container.ifBlank { selectedChoice.container.ifBlank { null } }
                selectedChoice.isMerged -> selectedChoice.container.ifBlank { container.ifBlank { null } }
                else -> container.ifBlank { null }
            }
            val mergeCompatibility = resolveMergeContainerCompatibility(
                requestedContainer = mergeContainer,
                selectedChoice = selectedChoice,
            )
            val activeOutputTemplate = if (isAudioOnly) {
                state.audioOutputTemplate
            } else {
                state.outputTemplate
            }
            val resolvedOutputTemplate = if (File(activeOutputTemplate).isAbsolute) {
                activeOutputTemplate
            } else {
                fileUtils.createOutputTemplateWithDirectory(
                    template = activeOutputTemplate,
                    category = targetCategory,
                )
            }
            val namedOutputTemplate = applyRequestedFileName(
                outputTemplate = resolvedOutputTemplate,
                requestedFileName = customFileName,
            )
            return BuiltSelectionOptions(
                options = DownloadOptions(
                    url = sourceUrl,
                    formatId = formatSelector,
                    outputTemplate = namedOutputTemplate,
                    thumbnailUrl = sourceThumbnailUrl,
                    extractorArgs = downloadExtractorArgs,
                    fallbackExtractorArgs = fallbackExtractorArgs,
                    loadInfoJsonPath = null,
                    userAgentHeader = if (state.cookiesEnabled && state.cookieUserAgentEnabled) {
                        CookieTextCodec.COOKIE_USER_AGENT
                    } else {
                        null
                    },
                    youtubeAuthEnabled = runtimeCookiesPath != null && youtubeAuthConfig != null,
                    youtubeCookiesPath = runtimeCookiesPath,
                    youtubePoToken = youtubeAuthConfig?.buildPoTokenValue(),
                    youtubePoTokenClientHint = youtubeAuthConfig?.clientHint ?: "web.gvs",
                    youtubeDataSyncId = youtubeAuthConfig?.dataSyncId?.ifBlank { null },
                    mergeOutputFormat = mergeCompatibility.resolvedContainer,
                    preferredVideoHeight = selectedChoice?.height ?: state.selectedQuality.maxHeight,
                    expectedDurationSeconds = sourceDurationSeconds,
                    downloadVideoOnly = streamType == StreamType.VIDEO_ONLY,
                    isPlaylistEnabled = info.isPlaylist || state.enablePlaylist,
                    shouldDownloadSubtitles = state.downloadSubtitles || state.embedSubtitles,
                    shouldEmbedSubtitles = state.embedSubtitles && !isAudioOnly && !shouldBypassMediaPostProcessing,
                    shouldEmbedMetadata = state.embedMetadata && !shouldBypassMediaPostProcessing,
                    shouldEmbedThumbnail = state.embedThumbnail && !shouldBypassMediaPostProcessing,
                    shouldWriteThumbnail = state.writeThumbnail,
                    extractAudio = isAudioOnly,
                    audioFormat = if (isAudioOnly) audioFormat.ifBlank { null } else null,
                    audioBitrateKbps = if (isAudioOnly) audioBitrateKbps else null,
                ),
                queueNote = mergeCompatibility.queueNote,
            )
        }

        if (info.isPlaylist) {
            val builtRequests = state.playlistItems
                .filter { it.isSelected }
                .map { item ->
                    val itemStreamType = if (item.useGlobalSettings) state.selectedStreamType else item.selectedStreamType
                    val itemSelector = if (item.useGlobalSettings) state.selectedFormatSelector else item.selectedFormatSelector
                    val itemContainer = if (item.useGlobalSettings) state.selectedContainer else item.selectedContainer
                    val itemAudioFormat = if (item.useGlobalSettings) state.selectedAudioFormat else item.selectedAudioFormat
                    val itemAudioBitrate = if (item.useGlobalSettings) state.audioBitrateKbps else item.audioBitrateKbps
                    val itemChoiceBundle = ChoiceBundle(
                        videoAudioChoices = item.availableVideoAudioChoices,
                        videoOnlyChoices = item.availableVideoOnlyChoices,
                        audioOnlyChoices = item.availableAudioOnlyChoices,
                    )
                    val builtSelection = buildOptionsForSelection(
                        sourceUrl = item.entry.webpageUrl,
                        sourceThumbnailUrl = item.entry.thumbnailUrl ?: info.thumbnailUrl,
                        sourceDurationSeconds = item.entry.durationSeconds ?: info.durationSeconds,
                        streamType = itemStreamType,
                        selectedSelector = itemSelector,
                        container = itemContainer,
                        audioFormat = itemAudioFormat,
                        audioBitrateKbps = itemAudioBitrate,
                        customFileName = item.customFileName,
                        choiceBundle = itemChoiceBundle,
                        targetCategory = FileUtils.MediaFolderCategory.PLAYLIST,
                    )
                    val options = builtSelection.options
                    logger.i(
                        "FormatViewModel",
                        "Queueing playlist item index=${item.entry.playlistItemIndex} url=${item.entry.webpageUrl} formatSelector=${options.formatId} extractAudio=${options.extractAudio}",
                    )
                    BuiltPlaylistRequest(
                        request = PlaylistDownloadRequest(
                            entry = item.entry,
                            options = options,
                            titleHint = item.customFileName.trim().ifBlank { item.entry.title },
                        ),
                        queueNote = builtSelection.queueNote,
                    )
                }
            val requests = builtRequests.map { it.request }
            val queueNotes = builtRequests.mapNotNull { it.queueNote }.distinct()
            val playlistResult = runCatching {
                repository.enqueuePlaylistDownload(
                    playlistTitle = info.title,
                    requests = requests,
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
                            messageScope = FormatMessageScope.BROWSER,
                            infoMessage = buildString {
                                append("Queued ${taskIds.size} playlist items in order.")
                                queuedMessageSuffix?.takeIf { it.isNotBlank() }?.let {
                                    append(" ")
                                    append(it)
                                }
                                if (queueNotes.isNotEmpty()) {
                                    append(" ")
                                    append(queueNotes.joinToString(" "))
                                }
                            },
                            lastQueuedStreamType = current.selectedStreamType,
                            lastQueuedFormatSelector = current.selectedFormatSelector,
                            lastQueuedContainer = current.selectedContainer,
                            lastQueuedAudioFormat = current.selectedAudioFormat,
                            lastQueuedAudioBitrate = current.audioBitrateKbps,
                            lastQueuedQuality = current.selectedQuality,
                            isDownloadButtonDisabled = true,
                            downloadButtonDisabledAt = System.currentTimeMillis(),
                        )
                    }
                },
                onFailure = { error ->
                    logger.e("FormatViewModel", "Playlist queue failed", error)
                    _uiState.update { current ->
                        current.copy(
                            isQueueing = false,
                            messageScope = FormatMessageScope.BROWSER,
                            errorMessage = error.message ?: "Unable to queue playlist.",
                        )
                    }
                },
            )
            return
        }

        val builtSelection = buildOptionsForSelection(
            sourceUrl = info.webpageUrl,
            sourceThumbnailUrl = info.thumbnailUrl,
            sourceDurationSeconds = info.durationSeconds,
            streamType = state.selectedStreamType,
            selectedSelector = state.selectedFormatSelector,
            container = state.selectedContainer,
            audioFormat = state.selectedAudioFormat,
            audioBitrateKbps = state.audioBitrateKbps,
            customFileName = state.customFileName,
            choiceBundle = ChoiceBundle(
                videoAudioChoices = state.availableVideoAudioChoices,
                videoOnlyChoices = state.availableVideoOnlyChoices,
                audioOnlyChoices = state.availableAudioOnlyChoices,
            ),
            targetCategory = when {
                state.selectedStreamType == StreamType.AUDIO_ONLY -> FileUtils.MediaFolderCategory.AUDIO
                state.selectedStreamType == StreamType.VIDEO_ONLY || state.selectedStreamType == StreamType.VIDEO_AUDIO ->
                    FileUtils.MediaFolderCategory.VIDEO
                else -> FileUtils.MediaFolderCategory.OTHER
            },
        )
        val options = builtSelection.options
        logger.i(
            "FormatViewModel",
            "Queueing download for URL=${options.url}, formatSelector=${options.formatId}, extractAudio=${options.extractAudio}",
        )

        val queueResult = runCatching {
            repository.enqueueDownload(
                options,
                state.customFileName.trim().ifBlank { info.title },
            )
        }
            .getOrElse { throwable ->
                Result.failure(IllegalStateException(throwable.message ?: "Unable to queue download.", throwable))
            }

        queueResult.fold(
            onSuccess = { taskId ->
                logger.i("FormatViewModel", "Queue success. taskId=$taskId")
                _uiState.update { current ->
                    current.copy(
                        isQueueing = false,
                        messageScope = FormatMessageScope.BROWSER,
                        infoMessage = buildString {
                            append("Added to queue. Task: $taskId")
                            queuedMessageSuffix?.takeIf { it.isNotBlank() }?.let {
                                append(" ")
                                append(it)
                            }
                            builtSelection.queueNote?.takeIf { it.isNotBlank() }?.let {
                                append(" ")
                                append(it)
                            }
                        },
                        lastQueuedStreamType = current.selectedStreamType,
                        lastQueuedFormatSelector = current.selectedFormatSelector,
                        lastQueuedContainer = current.selectedContainer,
                        lastQueuedAudioFormat = current.selectedAudioFormat,
                        lastQueuedAudioBitrate = current.audioBitrateKbps,
                        lastQueuedQuality = current.selectedQuality,
                        isDownloadButtonDisabled = true,
                        downloadButtonDisabledAt = System.currentTimeMillis(),
                    )
                }
            },
            onFailure = { error ->
                logger.e("FormatViewModel", "Queue failed", error)
                _uiState.update { current ->
                    current.copy(
                        isQueueing = false,
                        messageScope = FormatMessageScope.BROWSER,
                        errorMessage = error.message ?: "Unable to queue download.",
                    )
                }
            },
        )
    }

    private fun shouldPromptForMeteredNetwork(): Boolean {
        val state = uiState.value
        return !state.allowMeteredDownloads && networkStatusMonitor.isConnectedToMeteredNetwork()
    }

    fun dismissMessage() {
        _uiState.update { state -> state.copy(errorMessage = null, infoMessage = null) }
    }

    fun showSettingsMessage(message: String, isError: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                messageScope = FormatMessageScope.SETTINGS,
                infoMessage = if (isError) null else message,
                errorMessage = if (isError) message else null,
            )
        }
    }

    /**
     * Check if download button should be enabled.
     * Disabled if:
     * 1. Already disabled AND 6 seconds haven't passed yet
     * 2. Currently queueing
     */
    fun isDownloadButtonEnabled(): Boolean {
        val state = uiState.value
        if (state.isQueueing) return false
        
        if (state.isDownloadButtonDisabled) {
            val elapsed = System.currentTimeMillis() - state.downloadButtonDisabledAt
            if (elapsed < 6000) { // 6 seconds
                return false
            } else {
                // Timeout expired, re-enable
                _uiState.update { it.copy(isDownloadButtonDisabled = false, downloadButtonDisabledAt = 0L) }
            }
        }
        return true
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isDarkTheme = enabled,
                themeMode = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT,
            )
        }
        viewModelScope.launch {
            val state = uiState.value
            repository.updateSettings(
                state.appSettings.copy(
                    darkTheme = enabled,
                    themeMode = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT,
                ),
            )
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

    private fun updatePlaylistItem(
        index: Int,
        transform: (PlaylistItemUiState) -> PlaylistItemUiState,
    ) {
        _uiState.update { state ->
            if (index !in state.playlistItems.indices) return@update state
            val updatedItems = state.playlistItems.toMutableList()
            updatedItems[index] = transform(updatedItems[index])
            state.copy(playlistItems = updatedItems)
        }
    }

    private fun seedPlaylistOverrideFromGlobal(
        item: PlaylistItemUiState,
        state: FormatUiState,
    ): PlaylistItemUiState {
        return item.copy(
            useGlobalSettings = false,
            isExpanded = true,
            selectedStreamType = state.selectedStreamType,
            selectedFormatSelector = firstSelectorForStreamType(
                streamType = state.selectedStreamType,
                videoAudioChoices = item.availableVideoAudioChoices,
                videoOnlyChoices = item.availableVideoOnlyChoices,
                audioOnlyChoices = item.availableAudioOnlyChoices,
            ),
            selectedContainer = state.selectedContainer,
            selectedAudioFormat = state.selectedAudioFormat,
            audioBitrateKbps = state.audioBitrateKbps,
        )
    }

    private fun applyRequestedFileName(
        outputTemplate: String,
        requestedFileName: String,
    ): String {
        val trimmed = requestedFileName.trim()
        if (trimmed.isBlank()) return outputTemplate
        val templateFile = File(outputTemplate)
        val parent = templateFile.parentFile ?: return outputTemplate
        val sanitized = fileUtils.sanitizeFileName(trimmed)
        if (sanitized.isBlank()) return outputTemplate
        val suffix = if (templateFile.name.contains(".%(ext)s")) ".%(ext)s" else ""
        return File(parent, "$sanitized$suffix").absolutePath
    }

    private fun findChoice(
        streamType: StreamType,
        choiceBundle: ChoiceBundle,
        selector: String?,
    ): FormatChoice? {
        if (selector.isNullOrBlank()) return null
        val choices = when (streamType) {
            StreamType.VIDEO_AUDIO -> choiceBundle.videoAudioChoices
            StreamType.VIDEO_ONLY -> choiceBundle.videoOnlyChoices
            StreamType.AUDIO_ONLY -> choiceBundle.audioOnlyChoices
        }
        return choices.firstOrNull { it.selector == selector }
    }

    private fun resolveSelectedChoice(
        streamType: StreamType,
        selectedSelector: String?,
        choiceBundle: ChoiceBundle,
    ): FormatChoice? {
        val explicitChoice = findChoice(streamType, choiceBundle, selectedSelector)
        if (explicitChoice != null) return explicitChoice

        return when (streamType) {
            StreamType.VIDEO_AUDIO -> choiceBundle.videoAudioChoices.firstOrNull()
                ?: choiceBundle.videoOnlyChoices.firstOrNull()
            StreamType.VIDEO_ONLY -> choiceBundle.videoOnlyChoices.firstOrNull()
            StreamType.AUDIO_ONLY -> choiceBundle.audioOnlyChoices.firstOrNull()
        }
    }

    private fun firstSelectorForStreamType(
        streamType: StreamType,
        videoAudioChoices: List<FormatChoice>,
        videoOnlyChoices: List<FormatChoice>,
        audioOnlyChoices: List<FormatChoice>,
    ): String? {
        return when (streamType) {
            StreamType.VIDEO_AUDIO -> videoAudioChoices.firstOrNull()?.selector
                ?: videoOnlyChoices.firstOrNull()?.selector
            StreamType.VIDEO_ONLY -> videoOnlyChoices.firstOrNull()?.selector
            StreamType.AUDIO_ONLY -> audioOnlyChoices.firstOrNull()?.selector
        }
    }

    private data class ChoiceBundle(
        val videoAudioChoices: List<FormatChoice>,
        val videoOnlyChoices: List<FormatChoice>,
        val audioOnlyChoices: List<FormatChoice>,
    )

    private data class BuiltSelectionOptions(
        val options: DownloadOptions,
        val queueNote: String?,
    )

    private data class BuiltPlaylistRequest(
        val request: PlaylistDownloadRequest,
        val queueNote: String?,
    )

    private fun buildPlaylistItemStates(
        info: VideoInfo,
        streamType: StreamType,
        container: String,
        audioFormat: String,
        audioBitrateKbps: Int,
    ): List<PlaylistItemUiState> {
        return info.playlistEntries.map { entry ->
            val itemChoiceBundle = buildChoiceBundle(
                formats = entry.formats.ifEmpty { info.formats },
                durationSeconds = entry.durationSeconds ?: info.durationSeconds,
            )
            PlaylistItemUiState(
                entry = entry,
                customFileName = entry.title,
                selectedStreamType = streamType,
                selectedFormatSelector = firstSelectorForStreamType(
                    streamType = streamType,
                    videoAudioChoices = itemChoiceBundle.videoAudioChoices,
                    videoOnlyChoices = itemChoiceBundle.videoOnlyChoices,
                    audioOnlyChoices = itemChoiceBundle.audioOnlyChoices,
                ),
                selectedContainer = container,
                selectedAudioFormat = audioFormat,
                audioBitrateKbps = audioBitrateKbps,
                availableVideoAudioChoices = itemChoiceBundle.videoAudioChoices,
                availableVideoOnlyChoices = itemChoiceBundle.videoOnlyChoices,
                availableAudioOnlyChoices = itemChoiceBundle.audioOnlyChoices,
            )
        }
    }

    private fun buildChoices(info: VideoInfo): ChoiceBundle {
        return buildChoiceBundle(
            formats = info.formats,
            durationSeconds = info.durationSeconds,
        )
    }

    private fun buildChoiceBundle(
        formats: List<MediaFormat>,
        durationSeconds: Long?,
    ): ChoiceBundle {
        val audioOnly = formats.filter { it.isAudioOnly }
        val videoOnly = formats.filter { it.isVideoOnly }
        val muxed = formats.filter { !it.isAudioOnly && !it.isVideoOnly }

        val audioChoices = audioOnly.map { audio ->
            val bitrate = audio.bitrateKbps?.let { "${it}kbps" } ?: ""
            val label = listOf("audio", audio.extension, bitrate, audio.note.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" ")
            FormatChoice(
                selector = FormatSelectorBuilder.buildAudioOnlySelector(audio),
                label = label,
                streamType = StreamType.AUDIO_ONLY,
                container = audio.extension,
                height = null,
                isMerged = false,
                isImageLike = false,
                fileSizeBytes = audio.fileSizeBytes,
                estimatedSizeBytes = audio.fileSizeBytes
                    ?: estimateFormatSizeBytes(durationSeconds, audio.bitrateKbps),
                videoCodec = null,
                audioCodec = audio.audioCodec,
                fps = null,
                bitrateKbps = audio.bitrateKbps,
                note = audio.note,
            )
        }.sortedByDescending { extractBitrate(it.label) }

        val videoOnlyChoices = videoOnly.map { video ->
            val fps = video.fps?.let { "${it.toInt()}fps" } ?: ""
            val bitrate = video.bitrateKbps?.let { "${it}kbps" } ?: ""
            val label = listOf(video.resolution ?: "video", video.extension, fps, bitrate, video.note.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" ")
            FormatChoice(
                selector = FormatSelectorBuilder.buildVideoOnlySelector(video),
                label = label,
                streamType = StreamType.VIDEO_ONLY,
                container = video.extension,
                height = parseHeight(video.resolution),
                isMerged = false,
                isImageLike = video.isImageLike,
                fileSizeBytes = video.fileSizeBytes,
                estimatedSizeBytes = video.fileSizeBytes
                    ?: estimateFormatSizeBytes(durationSeconds, video.bitrateKbps),
                videoCodec = video.videoCodec,
                audioCodec = null,
                fps = video.fps,
                bitrateKbps = video.bitrateKbps,
                note = video.note,
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
                val combinedSizeBytes = combineFormatSizes(video.fileSizeBytes, audio.fileSizeBytes)
                val estimatedSizeBytes = combinedSizeBytes
                    ?: estimateFormatSizeBytes(
                        durationSeconds = durationSeconds,
                        bitrateKbps = listOfNotNull(video.bitrateKbps, audio.bitrateKbps)
                            .takeIf { it.isNotEmpty() }
                            ?.sum(),
                    )
                FormatChoice(
                    selector = FormatSelectorBuilder.buildMergedSelector(video, audio),
                    label = label,
                    streamType = StreamType.VIDEO_AUDIO,
                    container = video.extension,
                    height = parseHeight(video.resolution),
                    isMerged = true,
                    isImageLike = false,
                    fileSizeBytes = combinedSizeBytes,
                    estimatedSizeBytes = estimatedSizeBytes,
                    videoCodec = video.videoCodec,
                    audioCodec = audio.audioCodec,
                    fps = video.fps,
                    bitrateKbps = audio.bitrateKbps ?: video.bitrateKbps,
                    note = video.note ?: audio.note,
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
                selector = FormatSelectorBuilder.buildMuxedSelector(item),
                label = label,
                streamType = StreamType.VIDEO_AUDIO,
                container = item.extension,
                height = parseHeight(item.resolution),
                isMerged = false,
                isImageLike = item.isImageLike,
                fileSizeBytes = item.fileSizeBytes,
                estimatedSizeBytes = item.fileSizeBytes
                    ?: estimateFormatSizeBytes(durationSeconds, item.bitrateKbps),
                videoCodec = item.videoCodec,
                audioCodec = item.audioCodec,
                fps = item.fps,
                bitrateKbps = item.bitrateKbps,
                note = item.note,
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

    private fun combineFormatSizes(videoSizeBytes: Long?, audioSizeBytes: Long?): Long? {
        return when {
            videoSizeBytes != null && audioSizeBytes != null -> videoSizeBytes + audioSizeBytes
            else -> videoSizeBytes ?: audioSizeBytes
        }
    }

    private fun resolveDownloadExtractorArgs(
        info: VideoInfo,
        cookiesAvailable: Boolean,
    ): Pair<String?, String?> {
        val analysisExtractorArgs = info.extractorArgs?.trim()?.ifBlank { null }
        if (!isYoutubeUrl(info.webpageUrl)) {
            return analysisExtractorArgs to null
        }

        val fallbackExtractorArgs = YoutubeRequestPlanner.recoveryCandidates(
            selectedExtractorArgs = analysisExtractorArgs,
            cookiesAvailable = cookiesAvailable,
        ).firstOrNull()

        return analysisExtractorArgs to fallbackExtractorArgs
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val normalized = url.lowercase()
        return normalized.contains("youtube.com") || normalized.contains("youtu.be")
    }

    private fun resolveRuntimeCookiesPathForUrl(url: String): String? {
        val state = uiState.value
        if (!state.cookiesEnabled) return null
        val relevantProfiles = CookieTextCodec.findRelevantProfiles(state.cookieProfiles, url)
            .map(::ensureCookieProfileFile)
        if (relevantProfiles.isEmpty()) return null
        return fileUtils.writeTextToInternalFile(
            subDirectoryName = "cookies",
            targetFileName = "runtime-cookies.txt",
            content = CookieTextCodec.buildRuntimeCookieFile(relevantProfiles),
        )
    }

    private fun ensureCookieProfileFile(profile: CookieProfile): CookieProfile {
        val normalizedText = CookieTextCodec.buildStoredText(profile.url, profile.cookiesText)
        val targetFileName = "cookie-${profile.id}.txt"
        val targetPath = profile.localFilePath.ifBlank {
            fileUtils.writeTextToInternalFile(
                subDirectoryName = "cookies",
                targetFileName = targetFileName,
                content = normalizedText,
            )
        }
        val file = File(targetPath)
        val shouldRewrite = !file.exists() || runCatching {
            file.useLines { lines ->
                lines.firstOrNull()?.trim() != "# Netscape HTTP Cookie File"
            }
        }.getOrDefault(true)

        if (shouldRewrite) {
            val rewrittenPath = fileUtils.writeTextToInternalFile(
                subDirectoryName = "cookies",
                targetFileName = targetFileName,
                content = normalizedText,
            )
            return profile.copy(
                cookiesText = normalizedText,
                localFilePath = rewrittenPath,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }

        return if (profile.cookiesText != normalizedText) {
            profile.copy(cookiesText = normalizedText)
        } else {
            profile
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
                hasLoadedSettings = true,
                languageTag = settings.languageTag,
                themeMode = settings.themeMode,
                accentPreset = settings.accentPreset,
                contrastMode = settings.contrastMode,
                selectedContainer = settings.defaultMergeContainer,
                selectedAudioFormat = settings.defaultAudioFormat,
                outputTemplate = settings.defaultOutputTemplate,
                audioOutputTemplate = settings.defaultAudioOutputTemplate,
                downloadsRootFolderName = settings.downloadsRootFolderName,
                videoSubfolderName = settings.videoSubfolderName,
                audioSubfolderName = settings.audioSubfolderName,
                otherSubfolderName = settings.otherSubfolderName,
                maxConcurrentDownloads = settings.maxConcurrentDownloads,
                allowMeteredDownloads = settings.allowMeteredDownloads,
                downloadSubtitles = settings.autoDownloadSubtitles,
                embedSubtitles = settings.autoEmbedSubtitles,
                embedMetadata = settings.autoEmbedMetadata,
                embedThumbnail = settings.autoEmbedThumbnail,
                autoRemoveMissingFilesFromLibrary = settings.autoRemoveMissingFilesFromLibrary,
                deleteFromStorageWhenRemovedInApp = settings.deleteFromStorageWhenRemovedInApp,
                cookiesEnabled = settings.cookiesEnabled,
                cookieUserAgentEnabled = settings.cookieUserAgentEnabled,
                cookieProfiles = settings.cookieProfiles,
                youtubeAuthConfig = settings.youtubeAuthConfig,
                notifyCompletedDownloads = settings.notifyCompletedDownloads,
                notifyDownloadErrors = settings.notifyDownloadErrors,
                notifyCanceledDownloads = settings.notifyCanceledDownloads,
                notifyPromotions = settings.notifyPromotions,
                readyAnalyzedItems = if (settings.keepAnalyzedLinkHistory) {
                    pruneReadyHistory(state.readyAnalyzedItems, settings.analyzedLinkHistoryRetentionDays)
                } else {
                    state.readyAnalyzedItems
                },
                showMeteredNetworkDialog = false,
                isDarkTheme = when (settings.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> settings.darkTheme
                },
            )
        }
    }

    private fun normalizeTemplateValue(value: String, fallback: String): String {
        return value.trim().ifBlank { fallback }
    }

    private fun upsertYoutubeCookieProfile(
        existingProfiles: List<CookieProfile>,
        cookieText: String,
    ): List<CookieProfile> {
        val youtubeUrl = "https://www.youtube.com"
        val storedText = if (cookieText.trimStart().startsWith("# Netscape HTTP Cookie File")) {
            CookieTextCodec.buildStoredText(youtubeUrl, cookieText)
        } else {
            CookieTextCodec.fromCookieHeader(youtubeUrl, cookieText)
        }
        val existingProfile = CookieTextCodec.findBestMatch(existingProfiles, youtubeUrl)
        val profileId = existingProfile?.id ?: UUID.randomUUID().toString()
        val localPath = fileUtils.writeTextToInternalFile(
            subDirectoryName = "cookies",
            targetFileName = "cookie-$profileId.txt",
            content = storedText,
        )
        val updatedProfile = CookieProfile(
            id = profileId,
            url = youtubeUrl,
            cookiesText = storedText,
            localFilePath = localPath,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        return existingProfiles
            .filterNot { it.id == profileId }
            .plus(updatedProfile)
            .sortedByDescending { it.updatedAtEpochMs }
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

    private fun buildReadyRecord(info: VideoInfo): AnalyzedLinkRecord {
        return AnalyzedLinkRecord(
            webpageUrl = info.webpageUrl,
            title = info.title,
            uploader = info.uploader,
            durationSeconds = info.durationSeconds,
            thumbnailUrl = info.thumbnailUrl,
            isPlaylist = info.isPlaylist,
            playlistCount = info.playlistCount ?: info.playlistEntries.size.takeIf { info.isPlaylist },
            formatCount = info.formats.size,
            analyzedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun upsertReadyRecord(
        existing: List<AnalyzedLinkRecord>,
        record: AnalyzedLinkRecord,
    ): List<AnalyzedLinkRecord> {
        return pruneReadyHistory(
            listOf(record) + existing.filterNot { it.webpageUrl == record.webpageUrl },
            uiState.value.appSettings.analyzedLinkHistoryRetentionDays,
        )
    }

    private fun pruneReadyHistory(
        items: List<AnalyzedLinkRecord>,
        retentionDays: Int,
    ): List<AnalyzedLinkRecord> {
        val cutoff = System.currentTimeMillis() - retentionDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
        return items
            .filter { it.analyzedAtEpochMs >= cutoff }
            .sortedByDescending { it.analyzedAtEpochMs }
    }

    private fun persistReadyRecordIfNeeded(info: VideoInfo) {
        val settings = uiState.value.appSettings
        if (!settings.keepAnalyzedLinkHistory) return
        viewModelScope.launch {
            val updated = analyzedLinkHistoryStore.upsert(
                record = buildReadyRecord(info),
                retentionDays = settings.analyzedLinkHistoryRetentionDays,
            )
            _uiState.update { state ->
                state.copy(readyAnalyzedItems = upsertReadyRecords(state.readyAnalyzedItems, updated))
            }
        }
    }

    private suspend fun syncPersistedReadyHistory(settings: AppSettings) {
        if (!settings.keepAnalyzedLinkHistory) return
        val persisted = analyzedLinkHistoryStore.load(settings.analyzedLinkHistoryRetentionDays)
        _uiState.update { state ->
            state.copy(
                readyAnalyzedItems = upsertReadyRecords(state.readyAnalyzedItems, persisted),
            )
        }
    }

    private fun upsertReadyRecords(
        current: List<AnalyzedLinkRecord>,
        incoming: List<AnalyzedLinkRecord>,
    ): List<AnalyzedLinkRecord> {
        return (incoming + current)
            .distinctBy { it.webpageUrl }
            .sortedByDescending { it.analyzedAtEpochMs }
    }
}
