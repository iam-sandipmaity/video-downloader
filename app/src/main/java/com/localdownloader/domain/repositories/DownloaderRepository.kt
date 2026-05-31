package com.localdownloader.domain.repositories

import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.MediaSyncResult
import com.localdownloader.domain.models.PlaylistDownloadRequest
import com.localdownloader.domain.models.VideoInfo
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level contract for all downloader and media-processing operations.
 */
interface DownloaderRepository {
    suspend fun analyzeUrl(
        url: String,
        cookiesPath: String? = null,
        userAgent: String? = null,
        preferredExtractorArgs: String? = null,
    ): Result<VideoInfo>
    suspend fun enqueueDownload(options: DownloadOptions, titleHint: String): Result<String>
    suspend fun enqueuePlaylistDownload(
        playlistTitle: String,
        requests: List<PlaylistDownloadRequest>,
    ): Result<List<String>>
    suspend fun pauseDownload(taskId: String)
    suspend fun resumeDownload(taskId: String): Result<String>
    suspend fun retryDownload(taskId: String): Result<String>
    suspend fun moveQueuedDownload(taskId: String, earlier: Boolean): Result<Unit>
    suspend fun cancelDownload(taskId: String)
    suspend fun renameDownloadedFile(taskId: String, newName: String): Result<Unit>
    suspend fun deleteDownloadedFile(taskId: String): Result<Unit>
    suspend fun deleteDownloadedFiles(taskIds: List<String>): Result<Int>
    suspend fun removeDownloadedFilesFromLibrary(taskIds: List<String>): Result<Int>
    suspend fun permanentlyDeleteDownloadedFiles(taskIds: List<String>): Result<Int>
    suspend fun clearCompletedDownloads(): Result<Int>
    suspend fun clearCompletedLibraryEntries(): Result<Int>
    suspend fun deleteAllCompletedMedia(): Result<Int>
    suspend fun clearFailedAndCanceledHistory(): Result<Int>
    suspend fun syncDownloadedMedia(removeMissingEntries: Boolean? = null): Result<MediaSyncResult>
    fun observeDownloadQueue(): Flow<List<DownloadTask>>

    suspend fun convertMedia(request: ConversionRequest, onProgress: ((Float) -> Unit)?): Result<String>
    suspend fun compressMedia(request: CompressionRequest, onProgress: ((Float) -> Unit)?): Result<String>

    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun refillQueuedDownloads()
}
