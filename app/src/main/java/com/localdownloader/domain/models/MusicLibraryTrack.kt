package com.localdownloader.domain.models

enum class MusicSourceType(
    val storageKey: String,
    val label: String,
) {
    APP_DOWNLOADS("app_downloads", "App downloads"),
    DEVICE_AUDIO("device_audio", "Device audio"),
    SELECTED_FOLDER("selected_folder", "Selected folder");

    companion object {
        fun fromStorageKey(value: String?): MusicSourceType {
            return values().firstOrNull { it.storageKey == value } ?: APP_DOWNLOADS
        }
    }
}

data class MusicLibraryTrack(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val playbackUri: String,
    val filePath: String? = null,
    val fileName: String? = null,
    val folderName: String? = null,
    val displaySize: String = "",
    val sizeBytes: Long? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val durationMs: Long? = null,
    val sourceType: MusicSourceType,
    val sourceLabel: String = sourceType.label,
    val sourceUrl: String = "",
    val appTaskId: String? = null,
) {
    val canRename: Boolean
        get() = appTaskId != null && filePath != null

    val canTrim: Boolean
        get() = playbackUri.isNotBlank()

    val extension: String
        get() = fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.uppercase()
            .orEmpty()
}
