package com.localdownloader.domain.usecases

import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.repositories.DownloaderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDownloadQueueUseCase @Inject constructor(
    private val repository: DownloaderRepository,
) {
    operator fun invoke(): Flow<List<DownloadTask>> = repository.observeDownloadQueue()
}
