package com.localdownloader.domain.usecases

import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.repositories.DownloaderRepository
import javax.inject.Inject

class ConvertMediaUseCase @Inject constructor(
    private val repository: DownloaderRepository,
) {
    suspend fun convert(request: ConversionRequest, onProgress: ((Float) -> Unit)? = null): Result<String> =
        repository.convertMedia(request, onProgress)

    suspend fun compress(request: CompressionRequest, onProgress: ((Float) -> Unit)? = null): Result<String> =
        repository.compressMedia(request, onProgress)
}
