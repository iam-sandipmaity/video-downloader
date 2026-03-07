package com.localdownloader.domain.usecases

import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.repositories.DownloaderRepository
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    private val repository: DownloaderRepository,
) {
    suspend operator fun invoke(options: DownloadOptions, titleHint: String): Result<String> {
        return repository.enqueueDownload(options, titleHint)
    }
}
