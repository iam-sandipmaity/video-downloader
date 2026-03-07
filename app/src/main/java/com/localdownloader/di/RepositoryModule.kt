package com.localdownloader.di

import com.localdownloader.data.DownloadRepositoryImpl
import com.localdownloader.domain.repositories.DownloaderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDownloaderRepository(
        implementation: DownloadRepositoryImpl,
    ): DownloaderRepository
}
