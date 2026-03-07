package com.localdownloader.viewmodel

import com.localdownloader.domain.models.DownloadTask

data class DownloadUiState(
    val tasks: List<DownloadTask> = emptyList(),
    val expandedDebugTaskIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)
