package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.repositories.DownloaderRepository
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

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()
    private var settingsLoaded = false
    private var initialLibrarySyncCompleted = false

    init {
        viewModelScope.launch {
            repository.observeDownloadQueue().collect { tasks ->
                logger.d("DownloadViewModel", "Queue update received: count=${tasks.size}")
                _uiState.update { state -> state.copy(tasks = tasks) }
                maybeAutoSyncMissingFiles(tasks)
            }
        }

        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                settingsLoaded = true
                _uiState.update { state ->
                    state.copy(
                        autoRemoveMissingFilesFromLibrary = settings.autoRemoveMissingFilesFromLibrary,
                        deleteFromStorageWhenRemovedInApp = settings.deleteFromStorageWhenRemovedInApp,
                        downloadHistoryRetentionDays = settings.downloadHistoryRetentionDays,
                    )
                }
                maybeAutoSyncMissingFiles(uiState.value.tasks)
            }
        }
    }

    fun pause(taskId: String) {
        logger.i("DownloadViewModel", "pause requested taskId=$taskId")
        viewModelScope.launch {
            runCatching { repository.pauseDownload(taskId) }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "pause failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun resume(taskId: String) {
        logger.i("DownloadViewModel", "resume requested taskId=$taskId")
        viewModelScope.launch {
            repository.resumeDownload(taskId)
                .onFailure { error ->
                    logger.e("DownloadViewModel", "resume failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun retry(taskId: String) {
        logger.i("DownloadViewModel", "retry requested taskId=$taskId")
        viewModelScope.launch {
            repository.retryDownload(taskId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = "Retry queued for this item.",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "retry failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun cancel(taskId: String) {
        logger.i("DownloadViewModel", "cancel requested taskId=$taskId")
        viewModelScope.launch {
            runCatching { repository.cancelDownload(taskId) }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "cancel failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun pauseTasks(taskIds: List<String>) {
        val ids = taskIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return
        logger.i("DownloadViewModel", "batch pause requested count=${ids.size}")
        viewModelScope.launch {
            var failure: Throwable? = null
            ids.forEach { taskId ->
                runCatching { repository.pauseDownload(taskId) }
                    .onFailure { error ->
                        if (failure == null) failure = error
                        logger.e("DownloadViewModel", "batch pause failed taskId=$taskId", error)
                    }
            }
            _uiState.update { state ->
                state.copy(
                    infoMessage = if (failure == null) {
                        "Paused ${ids.size} queued item(s)."
                    } else {
                        state.infoMessage
                    },
                    errorMessage = failure?.message,
                )
            }
        }
    }

    fun resumeTasks(taskIds: List<String>) {
        val ids = taskIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return
        logger.i("DownloadViewModel", "batch resume requested count=${ids.size}")
        viewModelScope.launch {
            var failure: Throwable? = null
            ids.forEach { taskId ->
                repository.resumeDownload(taskId)
                    .onFailure { error ->
                        if (failure == null) failure = error
                        logger.e("DownloadViewModel", "batch resume failed taskId=$taskId", error)
                    }
            }
            _uiState.update { state ->
                state.copy(
                    infoMessage = if (failure == null) {
                        "Resumed ${ids.size} scheduled item(s)."
                    } else {
                        state.infoMessage
                    },
                    errorMessage = failure?.message,
                )
            }
        }
    }

    fun cancelTasks(taskIds: List<String>) {
        val ids = taskIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return
        logger.i("DownloadViewModel", "batch cancel requested count=${ids.size}")
        viewModelScope.launch {
            var failure: Throwable? = null
            ids.forEach { taskId ->
                runCatching { repository.cancelDownload(taskId) }
                    .onFailure { error ->
                        if (failure == null) failure = error
                        logger.e("DownloadViewModel", "batch cancel failed taskId=$taskId", error)
                    }
            }
            _uiState.update { state ->
                state.copy(
                    infoMessage = if (failure == null) {
                        "Canceled ${ids.size} queued item(s)."
                    } else {
                        state.infoMessage
                    },
                    errorMessage = failure?.message,
                )
            }
        }
    }

    fun retryTasks(taskIds: List<String>) {
        val ids = taskIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return
        logger.i("DownloadViewModel", "batch retry requested count=${ids.size}")
        viewModelScope.launch {
            var failure: Throwable? = null
            ids.forEach { taskId ->
                repository.retryDownload(taskId)
                    .onFailure { error ->
                        if (failure == null) failure = error
                        logger.e("DownloadViewModel", "batch retry failed taskId=$taskId", error)
                    }
            }
            _uiState.update { state ->
                state.copy(
                    infoMessage = if (failure == null) {
                        "Queued retry for ${ids.size} failed item(s)."
                    } else {
                        state.infoMessage
                    },
                    errorMessage = failure?.message,
                )
            }
        }
    }

    fun moveQueuedTaskEarlier(taskId: String) {
        moveQueuedTask(taskId = taskId, earlier = true)
    }

    fun moveQueuedTaskLater(taskId: String) {
        moveQueuedTask(taskId = taskId, earlier = false)
    }

    private fun moveQueuedTask(taskId: String, earlier: Boolean) {
        logger.i("DownloadViewModel", "queue reorder requested taskId=$taskId earlier=$earlier")
        viewModelScope.launch {
            repository.moveQueuedDownload(taskId, earlier)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (earlier) "Moved item earlier in the queue." else "Moved item later in the queue.",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "queue reorder failed taskId=$taskId earlier=$earlier", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun renameDownloadedFile(taskId: String, newName: String) {
        logger.i("DownloadViewModel", "rename requested taskId=$taskId")
        viewModelScope.launch {
            repository.renameDownloadedFile(taskId, newName)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(infoMessage = "Saved file renamed.", errorMessage = null)
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "rename failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun deleteDownloadedFile(taskId: String) {
        logger.i("DownloadViewModel", "delete requested taskId=$taskId")
        viewModelScope.launch {
            repository.deleteDownloadedFile(taskId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (state.deleteFromStorageWhenRemovedInApp) {
                                "Saved file removed."
                            } else {
                                "Item removed from the app library."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "delete failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun deleteDownloadedFiles(taskIds: List<String>) {
        logger.i("DownloadViewModel", "bulk delete requested count=${taskIds.size}")
        viewModelScope.launch {
            repository.deleteDownloadedFiles(taskIds)
                .onSuccess { removedCount ->
                    val actionLabel = if (uiState.value.deleteFromStorageWhenRemovedInApp) {
                        "Deleted $removedCount saved item(s)."
                    } else {
                        "Removed $removedCount item(s) from the app library."
                    }
                    _uiState.update { state ->
                        state.copy(infoMessage = actionLabel, errorMessage = null)
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "bulk delete failed count=${taskIds.size}", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun removeDownloadedFilesFromLibrary(taskIds: List<String>) {
        logger.i("DownloadViewModel", "remove from library requested count=${taskIds.size}")
        viewModelScope.launch {
            repository.removeDownloadedFilesFromLibrary(taskIds)
                .onSuccess { removedCount ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (removedCount == 0) {
                                "No saved items selected."
                            } else {
                                "Removed $removedCount item(s) from the app library."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e(
                        "DownloadViewModel",
                        "remove from library failed count=${taskIds.size}",
                        error,
                    )
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun permanentlyDeleteDownloadedFiles(taskIds: List<String>) {
        logger.i("DownloadViewModel", "permanent delete requested count=${taskIds.size}")
        viewModelScope.launch {
            repository.permanentlyDeleteDownloadedFiles(taskIds)
                .onSuccess { removedCount ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (removedCount == 0) {
                                "No saved media files selected."
                            } else {
                                "Deleted $removedCount saved media file(s) from the app and device."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e(
                        "DownloadViewModel",
                        "permanent delete failed count=${taskIds.size}",
                        error,
                    )
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun clearCompletedDownloads() {
        logger.i("DownloadViewModel", "clear completed requested")
        viewModelScope.launch {
            repository.clearCompletedDownloads()
                .onSuccess { removedCount ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (removedCount == 0) {
                                "No saved items to clear."
                            } else if (state.deleteFromStorageWhenRemovedInApp) {
                                "Cleared $removedCount saved item(s)."
                            } else {
                                "Removed $removedCount item(s) from the app library."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "clear completed failed", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun clearCompletedLibraryEntries() {
        logger.i("DownloadViewModel", "clear completed library entries requested")
        viewModelScope.launch {
            repository.clearCompletedLibraryEntries()
                .onSuccess { removedCount ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (removedCount == 0) {
                                "No saved items in the video tab."
                            } else {
                                "Removed $removedCount item(s) from the video tab. Files stay on device."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "clear completed library entries failed", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun deleteAllCompletedMedia() {
        logger.i("DownloadViewModel", "delete all completed media requested")
        viewModelScope.launch {
            repository.deleteAllCompletedMedia()
                .onSuccess { removedCount ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (removedCount == 0) {
                                "No saved media files to delete."
                            } else {
                                "Deleted $removedCount saved media file(s) from the app and device."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "delete all completed media failed", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun setDownloadHistoryRetentionDays(value: Int) {
        val normalized = value.coerceIn(MIN_DOWNLOAD_HISTORY_RETENTION_DAYS, MAX_DOWNLOAD_HISTORY_RETENTION_DAYS)
        logger.i("DownloadViewModel", "download history retention update requested days=$normalized")
        viewModelScope.launch {
            runCatching {
                val settings = repository.observeSettings().first()
                if (settings.downloadHistoryRetentionDays == normalized) return@runCatching normalized
                repository.updateSettings(settings.copy(downloadHistoryRetentionDays = normalized))
                normalized
            }.onSuccess { days ->
                _uiState.update { state ->
                    state.copy(
                        infoMessage = "Failed and canceled history will auto-clean after $days day(s).",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                logger.e("DownloadViewModel", "download history retention update failed", error)
                _uiState.update { state -> state.copy(errorMessage = error.message) }
            }
        }
    }

    fun clearFailedAndCanceledHistory() {
        logger.i("DownloadViewModel", "clear failed and canceled history requested")
        viewModelScope.launch {
            repository.clearFailedAndCanceledHistory()
                .onSuccess { removedCount ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (removedCount == 0) {
                                "No failed or canceled history items to clear."
                            } else {
                                "Cleared $removedCount failed or canceled history item(s)."
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "clear failed and canceled history failed", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun refreshLibrary() {
        logger.i("DownloadViewModel", "media library refresh requested")
        viewModelScope.launch {
            repository.syncDownloadedMedia(removeMissingEntries = true)
                .onSuccess { result ->
                    val message = when {
                        result.checkedItems == 0 -> "No saved items to sync."
                        result.missingItems == 0 -> "Library is already in sync."
                        result.removedEntries > 0 -> "Removed ${result.removedEntries} missing item(s) from the library."
                        else -> "Found ${result.missingItems} missing item(s)."
                    }
                    _uiState.update { state -> state.copy(infoMessage = message, errorMessage = null) }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "media library refresh failed", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun toggleDebug(taskId: String) {
        _uiState.update { state ->
            val ids = state.expandedDebugTaskIds
            state.copy(expandedDebugTaskIds = if (taskId in ids) ids - taskId else ids + taskId)
        }
    }

    fun dismissError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    fun dismissMessage() {
        _uiState.update { state -> state.copy(infoMessage = null, errorMessage = null) }
    }

    fun moveToVault(taskId: String, vaultId: String = "default") {
        logger.i("DownloadViewModel", "move to vault requested taskId=$taskId vaultId=$vaultId")
        viewModelScope.launch {
            repository.moveToVault(taskId, vaultId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(infoMessage = "Moved to vault.", errorMessage = null)
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "move to vault failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    fun moveFromVault(taskId: String) {
        logger.i("DownloadViewModel", "move from vault requested taskId=$taskId")
        viewModelScope.launch {
            repository.moveFromVault(taskId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(infoMessage = "Moved to downloads.", errorMessage = null)
                    }
                }
                .onFailure { error ->
                    logger.e("DownloadViewModel", "move from vault failed taskId=$taskId", error)
                    _uiState.update { state -> state.copy(errorMessage = error.message) }
                }
        }
    }

    private fun maybeAutoSyncMissingFiles(tasks: List<com.localdownloader.domain.models.DownloadTask>) {
        if (!settingsLoaded) return
        val state = uiState.value
        if (!state.autoRemoveMissingFilesFromLibrary) return

        val hasMissingCompletedFiles = tasks.any { task ->
            task.status == com.localdownloader.domain.models.DownloadStatus.COMPLETED &&
                task.outputPath?.takeIf { it.isNotBlank() }?.let { path -> !File(path).exists() } != false
        }
        if (!hasMissingCompletedFiles && initialLibrarySyncCompleted) return
        initialLibrarySyncCompleted = true

        viewModelScope.launch {
            repository.syncDownloadedMedia(removeMissingEntries = true)
                .onFailure { error ->
                    logger.e("DownloadViewModel", "auto sync missing media failed", error)
                }
        }
    }

    private companion object {
        const val MIN_DOWNLOAD_HISTORY_RETENTION_DAYS = 7
        const val MAX_DOWNLOAD_HISTORY_RETENTION_DAYS = 180
    }
}
