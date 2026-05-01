package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}
