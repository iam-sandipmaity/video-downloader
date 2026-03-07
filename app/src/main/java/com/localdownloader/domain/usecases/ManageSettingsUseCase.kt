package com.localdownloader.domain.usecases

import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.repositories.DownloaderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageSettingsUseCase @Inject constructor(
    private val repository: DownloaderRepository,
) {
    fun observe(): Flow<AppSettings> = repository.observeSettings()

    suspend fun update(settings: AppSettings) = repository.updateSettings(settings)
}
