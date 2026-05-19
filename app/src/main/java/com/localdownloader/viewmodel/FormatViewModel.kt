package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.downloader.FormatSelectorBuilder
import com.localdownloader.downloader.YoutubeRequestPlanner
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.domain.models.YoutubeAuthConfig
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.utils.CookieTextCodec
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
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
    private val fileUtils: FileUtils,
    private val urlValidator: UrlValidator,
    private val logger: Logger,
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
                selectedFormatSelector = null,
                infoMessage = null,
                errorMessage = null,
            )
        }
    }

    fun clearAnalyzedResult() {
        _uiState.update { state ->
            state.copy(
                isAnalyzing = false,
                isQueueing = false,
                videoInfo = null,
                availableVideoAudioChoices = emptyList(),
                availableVideoOnlyChoices = emptyList(),
                availableAudioOnlyChoices = emptyList(),
                selectedFormatSelector = null,
                infoMessage = null,
                errorMessage = null,
            )
        }
    }

    fun analyzeUrl() {
        val url = uiState.value.urlInput.trim()
        logger.i("FormatViewModel", "Analyze requested for URL: $url")
        if (!urlValidator.isValidHttpUrl(url)) {
            logger.w("FormatViewModel", "Rejected analyze request due to invalid URL: $url")
            _uiState.update { state ->
                scopedMessageState(
                    state = state,
                    scope = FormatMessageScope.BROWSER,
                    errorMessage = "Please enter a valid http/https URL.",
                )
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

            val runtimeCookiesPath = resolveRuntimeCookiesPathForUrl(url)
            val result = runCatching {
                repository.analyzeUrl(
                    url = url,
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
                        "Analyze success for URL: $url, title='${info.title}', formats=${info.formats.size}",
                    )
                    val choiceBundle = buildChoices(info)
                    val selectedSelector = firstSelectorForStreamType(
                        streamType = uiState.value.selectedStreamType,
                        videoAudioChoices = choiceBundle.videoAudioChoices,
                        videoOnlyChoices = choiceBundle.videoOnlyChoices,
                        audioOnlyChoices = choiceBundle.audioOnlyChoices,
                    )
                    _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            messageScope = FormatMessageScope.BROWSER,
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
                            messageScope = FormatMessageScope.BROWSER,
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

    fun onPlaylistEnabledChanged(value: Boolean) {
        _uiState.update { state -> state.copy(enablePlaylist = value) }
    }

    fun onOutputTemplateChanged(value: String) {
        _uiState.update { state -> state.copy(outputTemplate = value) }
    }

    fun onAudioOutputTemplateChanged(value: String) {
        _uiState.update { state -> state.copy(audioOutputTemplate = value) }
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
        _uiState.update { state -> state.copy(languageTag = value) }
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
                val updatedProfiles = current.cookieProfiles.filterNot { it.id == profileId }
                val newSettings = current.appSettings.copy(cookieProfiles = updatedProfiles)
                repository.updateSettings(newSettings)
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
                val newSettings = current.appSettings.copy(cookieProfiles = emptyList())
                repository.updateSettings(newSettings)
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
                    errorMessage = "PO token generation did not complete. Try loading the sample video again.",
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
                    errorMessage = "Generate YouTube access first, then turn it on.",
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
                        infoMessage = "Saved YouTube PO tokens were cleared.",
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
                hasLoadedSettings = true,
                languageTag = defaults.languageTag,
                themeMode = defaults.themeMode,
                accentPreset = defaults.accentPreset,
                contrastMode = defaults.contrastMode,
                downloadsRootFolderName = defaults.downloadsRootFolderName,
                videoSubfolderName = defaults.videoSubfolderName,
                audioSubfolderName = defaults.audioSubfolderName,
                otherSubfolderName = defaults.otherSubfolderName,
                isDarkTheme = false,
                infoMessage = null,
                errorMessage = null,
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
                cookiesEnabled = state.cookiesEnabled,
                cookieUserAgentEnabled = state.cookieUserAgentEnabled,
                cookieProfiles = state.cookieProfiles,
                youtubeAuthConfig = state.youtubeAuthConfig,
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

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isQueueing = true, errorMessage = null, infoMessage = null) }

            val selectedChoice = resolveSelectedChoice(state)
            val formatSelector = if (info.isPlaylist) {
                buildFormatSelector(
                    quality = state.selectedQuality,
                    streamType = state.selectedStreamType,
                    container = state.selectedContainer,
                )
            } else if (selectedChoice != null) {
                selectedChoice.selector
            } else if (isYoutubeUrl(info.webpageUrl)) {
                val st = state.selectedStreamType
                val boundedHeight = state.selectedQuality.maxHeight?.let { "[height<=$it]" }.orEmpty()
                when (st) {
                    StreamType.AUDIO_ONLY -> "bestaudio/best"
                    StreamType.VIDEO_ONLY -> "bestvideo$boundedHeight/bestvideo"
                    StreamType.VIDEO_AUDIO -> "bestvideo$boundedHeight+bestaudio/best$boundedHeight/best"
                }
            } else {
                buildFormatSelector(
                    quality = state.selectedQuality,
                    streamType = state.selectedStreamType,
                    container = state.selectedContainer,
                )
            }
            val isAudioOnly = state.selectedStreamType == StreamType.AUDIO_ONLY
            val shouldBypassMediaPostProcessing = !info.isPlaylist && selectedChoice?.isImageLike == true
            val runtimeCookiesPath = resolveRuntimeCookiesPathForUrl(info.webpageUrl)
            val mergeContainer = when {
                isAudioOnly -> null
                selectedChoice == null -> state.selectedContainer.ifBlank { null }
                selectedChoice.streamType == StreamType.VIDEO_AUDIO && selectedChoice.selector.contains("+") ->
                    state.selectedContainer.ifBlank { selectedChoice.container.ifBlank { null } }
                selectedChoice.isMerged -> selectedChoice.container.ifBlank { state.selectedContainer.ifBlank { null } }
                else -> state.selectedContainer.ifBlank { null }
            }
            val (downloadExtractorArgs, fallbackExtractorArgs) = resolveDownloadExtractorArgs(
                info = info,
                cookiesAvailable = runtimeCookiesPath != null,
            )
            val youtubeAuthConfig = state.youtubeAuthConfig.takeIf { it.enabled && it.isConfigured() }
            val targetCategory = when {
                info.isPlaylist -> FileUtils.MediaFolderCategory.PLAYLIST
                isAudioOnly -> FileUtils.MediaFolderCategory.AUDIO
                state.selectedStreamType == StreamType.VIDEO_ONLY || state.selectedStreamType == StreamType.VIDEO_AUDIO ->
                    FileUtils.MediaFolderCategory.VIDEO
                else -> FileUtils.MediaFolderCategory.OTHER
            }
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

            // Re-extract at download time so yt-dlp can refresh short-lived HLS/DASH URLs.
            val options = DownloadOptions(
                url = info.webpageUrl,
                formatId = formatSelector,
                outputTemplate = resolvedOutputTemplate,
                thumbnailUrl = info.thumbnailUrl,
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
                mergeOutputFormat = mergeContainer,
                preferredVideoHeight = selectedChoice?.height ?: state.selectedQuality.maxHeight,
                downloadVideoOnly = state.selectedStreamType == StreamType.VIDEO_ONLY,
                isPlaylistEnabled = info.isPlaylist || state.enablePlaylist,
                shouldDownloadSubtitles = state.downloadSubtitles || state.embedSubtitles,
                shouldEmbedSubtitles = state.embedSubtitles && !isAudioOnly && !shouldBypassMediaPostProcessing,
                shouldEmbedMetadata = state.embedMetadata && !shouldBypassMediaPostProcessing,
                shouldEmbedThumbnail = state.embedThumbnail && !shouldBypassMediaPostProcessing,
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
                                messageScope = FormatMessageScope.BROWSER,
                                infoMessage = "Queued ${taskIds.size} playlist items in order.",
                                // Store current settings and disable button for 6 seconds
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
                            messageScope = FormatMessageScope.BROWSER,
                            infoMessage = "Added to queue. Task: $taskId",
                            // Store current settings and disable button for 6 seconds
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

    private fun findChoice(state: FormatUiState, selector: String?): FormatChoice? {
        if (selector.isNullOrBlank()) return null
        val choices = when (state.selectedStreamType) {
            StreamType.VIDEO_AUDIO -> state.availableVideoAudioChoices
            StreamType.VIDEO_ONLY -> state.availableVideoOnlyChoices
            StreamType.AUDIO_ONLY -> state.availableAudioOnlyChoices
        }
        return choices.firstOrNull { it.selector == selector }
    }

    private fun resolveSelectedChoice(state: FormatUiState): FormatChoice? {
        val explicitChoice = findChoice(state, state.selectedFormatSelector)
        if (explicitChoice != null) return explicitChoice

        return when (state.selectedStreamType) {
            StreamType.VIDEO_AUDIO -> state.availableVideoAudioChoices.firstOrNull()
                ?: state.availableVideoOnlyChoices.firstOrNull()
            StreamType.VIDEO_ONLY -> state.availableVideoOnlyChoices.firstOrNull()
            StreamType.AUDIO_ONLY -> state.availableAudioOnlyChoices.firstOrNull()
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

    private fun buildChoices(info: VideoInfo): ChoiceBundle {
        if (info.isPlaylist) {
            return ChoiceBundle(
                videoAudioChoices = emptyList(),
                videoOnlyChoices = emptyList(),
                audioOnlyChoices = emptyList(),
            )
        }
        val formats = info.formats
        val durationSeconds = info.durationSeconds
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
}
