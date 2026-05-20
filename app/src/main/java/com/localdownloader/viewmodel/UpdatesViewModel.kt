package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.DownloadTaskStore
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.updates.AppUpdateManager
import com.localdownloader.updates.ComponentUpdateCheck
import com.localdownloader.updates.FfmpegReleaseChannel
import com.localdownloader.updates.FfmpegUpdateManager
import com.localdownloader.updates.UpdatePreferences
import com.localdownloader.updates.UpdatePreferencesStore
import com.localdownloader.updates.YtDlpReleaseChannel
import com.localdownloader.updates.YtDlpUpdateManager
import com.localdownloader.worker.YtDlpUpdateScheduler
import com.localdownloader.worker.YtDlpUpdateStateStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    private val ytDlpUpdateManager: YtDlpUpdateManager,
    private val ffmpegUpdateManager: FfmpegUpdateManager,
    private val updatePreferencesStore: UpdatePreferencesStore,
    private val downloadTaskStore: DownloadTaskStore,
    private val ytDlpUpdateScheduler: YtDlpUpdateScheduler,
    private val ytDlpUpdateStateStore: YtDlpUpdateStateStore,
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(UpdatesUiState())
    val uiState = _uiState.asStateFlow()

    private var initialized = false

    fun initialize() {
        val preferences = updatePreferencesStore.currentPreferences()
        _uiState.value = _uiState.value.copy(
            preferences = preferences,
            app = _uiState.value.app.copy(currentVersion = appUpdateManager.currentVersionLabel()),
        )
        if (!initialized) {
            initialized = true
        }
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            refreshAppInternal()
            refreshYtDlpInternal()
            refreshFfmpegInternal()
        }
    }

    fun refreshApp() {
        viewModelScope.launch { refreshAppInternal() }
    }

    fun refreshYtDlp() {
        viewModelScope.launch { refreshYtDlpInternal() }
    }

    fun refreshFfmpeg() {
        viewModelScope.launch { refreshFfmpegInternal() }
    }

    fun setIncludePrereleaseAppReleases(enabled: Boolean) {
        updatePreferencesStore.setIncludePrereleaseAppReleases(enabled)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(includePrereleaseAppReleases = enabled),
        )
        refreshApp()
    }

    fun setAutoUpdateYtDlp(enabled: Boolean) {
        updatePreferencesStore.setAutoUpdateYtDlp(enabled)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(autoUpdateYtDlp = enabled),
        )
        if (enabled) {
            ytDlpUpdateScheduler.cancelScheduled()
            ytDlpUpdateScheduler.scheduleIfDue()
        } else {
            ytDlpUpdateScheduler.cancelScheduled()
        }
    }

    fun setYtDlpChannel(channel: YtDlpReleaseChannel) {
        updatePreferencesStore.setYtDlpChannel(channel)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(ytDlpChannel = channel),
        )
        if (_uiState.value.preferences.autoUpdateYtDlp) {
            ytDlpUpdateScheduler.cancelScheduled()
            ytDlpUpdateScheduler.scheduleIfDue()
        }
        refreshYtDlp()
    }

    fun setFfmpegChannel(channel: FfmpegReleaseChannel) {
        updatePreferencesStore.setFfmpegChannel(channel)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(ffmpegChannel = channel),
        )
        refreshFfmpeg()
    }

    fun installYtDlpUpdate() {
        val channel = _uiState.value.preferences.ytDlpChannel
        viewModelScope.launch {
            if (hasActiveDownloads()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Pause or finish active downloads before updating yt-dlp.",
                    infoMessage = null,
                )
                return@launch
            }
            ytDlpUpdateStateStore.markAttemptStarted()
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.copy(isInstalling = true, progressPercent = 0),
                infoMessage = null,
                errorMessage = null,
            )
            runCatching {
                ytDlpUpdateManager.installUpdate(channel) { progress ->
                    _uiState.value = _uiState.value.copy(
                        ytDlp = _uiState.value.ytDlp.copy(progressPercent = progress),
                    )
                }
            }.onSuccess { result ->
                ytDlpUpdateStateStore.markAttemptSucceeded(result.message)
                _uiState.value = _uiState.value.copy(
                    ytDlp = _uiState.value.ytDlp.copy(isInstalling = false, progressPercent = null),
                    infoMessage = result.message,
                )
                refreshYtDlp()
            }.onFailure { error ->
                ytDlpUpdateStateStore.markAttemptFailed(error.message ?: "manual_update_failed")
                _uiState.value = _uiState.value.copy(
                    ytDlp = _uiState.value.ytDlp.copy(isInstalling = false, progressPercent = null),
                    errorMessage = error.message ?: "Failed to update yt-dlp.",
                )
            }
        }
    }

    fun installFfmpegUpdate() {
        val channel = _uiState.value.preferences.ffmpegChannel
        viewModelScope.launch {
            if (hasActiveDownloads()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Pause or finish active downloads before updating FFmpeg.",
                    infoMessage = null,
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                ffmpeg = _uiState.value.ffmpeg.copy(isInstalling = true, progressPercent = 0),
                infoMessage = null,
                errorMessage = null,
            )
            runCatching {
                ffmpegUpdateManager.installUpdate(channel) { progress ->
                    _uiState.value = _uiState.value.copy(
                        ffmpeg = _uiState.value.ffmpeg.copy(progressPercent = progress),
                    )
                }
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    ffmpeg = _uiState.value.ffmpeg.copy(isInstalling = false, progressPercent = null),
                    infoMessage = result.message,
                )
                refreshFfmpeg()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    ffmpeg = _uiState.value.ffmpeg.copy(isInstalling = false, progressPercent = null),
                    errorMessage = error.message ?: "Failed to update FFmpeg.",
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null, errorMessage = null)
    }

    private suspend fun refreshAppInternal() {
        _uiState.value = _uiState.value.copy(app = _uiState.value.app.copy(isChecking = true))
        runCatching {
            appUpdateManager.checkForUpdate(_uiState.value.preferences.includePrereleaseAppReleases)
        }.onSuccess { check ->
            _uiState.value = _uiState.value.copy(
                app = _uiState.value.app.fromCheck(check).copy(
                    isChecking = false,
                    currentVersion = appUpdateManager.currentVersionLabel(),
                ),
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                app = _uiState.value.app.copy(
                    isChecking = false,
                    currentVersion = appUpdateManager.currentVersionLabel(),
                    summary = error.message ?: "Failed to check app updates.",
                ),
            )
        }
    }

    private suspend fun refreshYtDlpInternal() {
        _uiState.value = _uiState.value.copy(ytDlp = _uiState.value.ytDlp.copy(isChecking = true))
        runCatching {
            ytDlpUpdateManager.check(_uiState.value.preferences.ytDlpChannel)
        }.onSuccess { check ->
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.fromCheck(check).copy(
                    isChecking = false,
                    lastStatus = ytDlpUpdateStateStore.lastStatus(),
                ),
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.copy(
                    isChecking = false,
                    summary = error.message ?: "Failed to check yt-dlp updates.",
                    lastStatus = ytDlpUpdateStateStore.lastStatus(),
                ),
            )
        }
    }

    private suspend fun refreshFfmpegInternal() {
        _uiState.value = _uiState.value.copy(ffmpeg = _uiState.value.ffmpeg.copy(isChecking = true))
        runCatching {
            ffmpegUpdateManager.check(_uiState.value.preferences.ffmpegChannel)
        }.onSuccess { check ->
            _uiState.value = _uiState.value.copy(
                ffmpeg = _uiState.value.ffmpeg.fromCheck(check).copy(isChecking = false),
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                ffmpeg = _uiState.value.ffmpeg.copy(
                    isChecking = false,
                    summary = error.message ?: "Failed to check FFmpeg updates.",
                ),
            )
        }
    }

    private fun hasActiveDownloads(): Boolean {
        return downloadTaskStore.getAllTasks().any { task ->
            task.status == DownloadStatus.RUNNING
        }
    }
}

data class UpdatesUiState(
    val preferences: UpdatePreferences = UpdatePreferences(),
    val app: UpdateSectionUiState = UpdateSectionUiState(
        title = "App update",
        subtitle = "Check the installed app against the latest GitHub release and open a newer APK in your browser when available.",
    ),
    val ytDlp: UpdateSectionUiState = UpdateSectionUiState(
        title = "yt-dlp update",
        subtitle = "Switch sources and install newer downloader builds when extractor changes are needed.",
    ),
    val ffmpeg: UpdateSectionUiState = UpdateSectionUiState(
        title = "FFmpeg update",
        subtitle = "Install a stronger app-owned FFmpeg runtime overlay for newer media processing support.",
    ),
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

data class UpdateSectionUiState(
    val title: String,
    val subtitle: String,
    val currentVersion: String? = null,
    val latestVersion: String? = null,
    val summary: String = "Not checked yet.",
    val releaseNotes: String? = null,
    val releasePageUrl: String? = null,
    val isChecking: Boolean = false,
    val isInstalling: Boolean = false,
    val updateAvailable: Boolean = false,
    val progressPercent: Int? = null,
    val latestCheck: ComponentUpdateCheck? = null,
    val lastStatus: String? = null,
)

private fun UpdateSectionUiState.fromCheck(check: ComponentUpdateCheck): UpdateSectionUiState {
    return copy(
        currentVersion = check.currentVersion,
        latestVersion = check.latestVersion,
        summary = check.summary,
        releaseNotes = check.releaseNotes,
        releasePageUrl = check.releasePageUrl,
        updateAvailable = check.updateAvailable,
        latestCheck = check,
    )
}
