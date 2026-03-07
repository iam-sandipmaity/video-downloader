package com.localdownloader.domain.usecases

import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.repositories.DownloaderRepository
import javax.inject.Inject

class AnalyzeUrlUseCase @Inject constructor(
    private val repository: DownloaderRepository,
) {
    suspend operator fun invoke(url: String): Result<VideoInfo> = repository.analyzeUrl(url)
}
