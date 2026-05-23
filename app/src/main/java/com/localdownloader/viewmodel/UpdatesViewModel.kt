package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.DownloadTaskStore
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.updates.AppUpdateManager
import com.localdownloader.updates.ComponentUpdateCheck
import com.localdownloader.updates.FfmpegReleaseChannel
import com.localdownloader.updates.FfmpegUpdateManager
import com.localdownloader.updates.PreparedAppUpdate
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
        val storedPreferences = updatePreferencesStore.currentPreferences()
        val preferences = if (YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_ENABLED) {
            storedPreferences
        } else {
            updatePreferencesStore.disableYtDlpAutoUpdateIfEnabled()
            ytDlpUpdateScheduler.cancelScheduled()
            storedPreferences.copy(autoUpdateYtDlp = false)
        }
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
        if (!YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_ENABLED) {
            updatePreferencesStore.disableYtDlpAutoUpdateIfEnabled()
            ytDlpUpdateScheduler.cancelScheduled()
            _uiState.value = _uiState.value.copy(
                preferences = _uiState.value.preferences.copy(autoUpdateYtDlp = false),
                infoMessage = YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_DISABLED_REASON,
                errorMessage = null,
            )
            return
        }

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

    fun installAppUpdate() {
        viewModelScope.launch {
            if (hasBlockingDownloads()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Finish, cancel, or resume queued downloads before installing app updates.",
                    infoMessage = null,
                )
                return@launch
            }
            val existingPrepared = _uiState.value.pendingAppInstall
                ?.let(appUpdateManager::refreshPreparedInstall)
            if (existingPrepared != null) {
                _uiState.value = _uiState.value.copy(
                    app = _uiState.value.app.copy(isInstalling = false, progressPercent = 100),
                    pendingAppInstall = existingPrepared,
                    pendingAppInstallRequestId = _uiState.value.pendingAppInstallRequestId + 1,
                    infoMessage = if (existingPrepared.requiresInstallPermission) {
                        "Allow installs from this app, then tap Install app update again."
                    } else {
                        "App update is already downloaded. Opening the installer now."
                    },
                    errorMessage = null,
                )
                return@launch
            }
            val check = _uiState.value.app.latestCheck ?: return@launch
            _uiState.value = _uiState.value.copy(
                app = _uiState.value.app.copy(isInstalling = true, progressPercent = 0),
                infoMessage = null,
                errorMessage = null,
            )
            runCatching {
                appUpdateManager.prepareInstall(check) { progress ->
                    _uiState.value = _uiState.value.copy(
                        app = _uiState.value.app.copy(progressPercent = progress),
                    )
                }
            }.onSuccess { prepared ->
                _uiState.value = _uiState.value.copy(
                    app = _uiState.value.app.copy(isInstalling = false, progressPercent = 100),
                    pendingAppInstall = prepared,
                    pendingAppInstallRequestId = _uiState.value.pendingAppInstallRequestId + 1,
                    infoMessage = if (prepared.requiresInstallPermission) {
                        "Allow installs from this app, then tap Install app update again."
                    } else {
                        "App update downloaded. Opening the installer now."
                    },
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    app = _uiState.value.app.copy(isInstalling = false, progressPercent = null),
                    errorMessage = error.message ?: "Failed to prepare the app update.",
                )
            }
        }
    }

    fun installYtDlpUpdate() {
        if (!YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_ENABLED) {
            _uiState.value = _uiState.value.copy(
                infoMessage = null,
                errorMessage = YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_DISABLED_REASON,
            )
            return
        }

        val channel = _uiState.value.preferences.ytDlpChannel
        viewModelScope.launch {
            if (hasBlockingDownloads()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Finish, cancel, or resume queued downloads before updating yt-dlp.",
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
            if (hasBlockingDownloads()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Finish, cancel, or resume queued downloads before updating FFmpeg.",
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

    fun consumePendingAppInstall() {
        _uiState.value = _uiState.value.copy(pendingAppInstall = null)
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
                    lastStatus = if (YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_ENABLED) {
                        ytDlpUpdateStateStore.lastStatus()
                    } else {
                        null
                    },
                ),
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.copy(
                    isChecking = false,
                    summary = error.message ?: "Failed to check yt-dlp updates.",
                    lastStatus = if (YtDlpUpdateManager.IN_APP_RUNTIME_UPDATES_ENABLED) {
                        ytDlpUpdateStateStore.lastStatus()
                    } else {
                        null
                    },
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

    private fun hasBlockingDownloads(): Boolean {
        return downloadTaskStore.getAllTasks().any { task ->
            task.status == DownloadStatus.QUEUED ||
                task.status == DownloadStatus.RUNNING ||
                task.status == DownloadStatus.PAUSED
        }
    }
}

data class UpdatesUiState(
    val preferences: UpdatePreferences = UpdatePreferences(),
    val app: UpdateSectionUiState = UpdateSectionUiState(
        title = "App update",
        subtitle = "Check the installed app against the latest GitHub release and install a newer APK when available.",
    ),
    val ytDlp: UpdateSectionUiState = UpdateSectionUiState(
        title = "yt-dlp update",
        subtitle = "In-app yt-dlp installs are disabled until release authenticity can be verified independently.",
    ),
    val ffmpeg: UpdateSectionUiState = UpdateSectionUiState(
        title = "FFmpeg update",
        subtitle = "Install a stronger app-owned FFmpeg runtime overlay for newer media processing support.",
    ),
    val pendingAppInstall: PreparedAppUpdate? = null,
    val pendingAppInstallRequestId: Long = 0L,
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
