package com.localdownloader.domain.models

/**
 * Summary of a media-library sync pass against device storage.
 */
data class MediaSyncResult(
    val checkedItems: Int,
    val missingItems: Int,
    val removedEntries: Int,
)
