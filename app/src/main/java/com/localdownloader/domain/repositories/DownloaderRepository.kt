package com.localdownloader.domain.repositories

import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.VideoInfo
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level contract for all downloader and media-processing operations.
 */
interface DownloaderRepository {
    suspend fun analyzeUrl(url: String): Result<VideoInfo>
    suspend fun enqueueDownload(options: DownloadOptions, titleHint: String): Result<String>
    suspend fun pauseDownload(taskId: String)
    suspend fun resumeDownload(taskId: String): Result<String>
    suspend fun cancelDownload(taskId: String)
    fun observeDownloadQueue(): Flow<List<DownloadTask>>

    suspend fun convertMedia(request: ConversionRequest, onProgress: ((Float) -> Unit)?): Result<String>
    suspend fun compressMedia(request: CompressionRequest, onProgress: ((Float) -> Unit)?): Result<String>

    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
}
