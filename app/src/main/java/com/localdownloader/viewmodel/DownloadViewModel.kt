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
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeDownloadQueue().collect { tasks ->
                logger.d("DownloadViewModel", "Queue update received: count=${tasks.size}")
                _uiState.update { state -> state.copy(tasks = tasks) }
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

    fun toggleDebug(taskId: String) {
        _uiState.update { state ->
            val ids = state.expandedDebugTaskIds
            state.copy(expandedDebugTaskIds = if (taskId in ids) ids - taskId else ids + taskId)
        }
    }

    fun dismissError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }
}
