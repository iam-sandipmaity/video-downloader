package com.localdownloader.ui.model

import com.localdownloader.domain.models.DownloadTask
import java.io.File

data class VideoLibraryItem(
    val task: DownloadTask,
    val file: File?,
    val displayTitle: String,
    val displaySize: String,
    val exists: Boolean,
    val mediaKind: MediaKind,
)

enum class MediaKind {
    VIDEO,
    AUDIO,
    OTHER,
}
