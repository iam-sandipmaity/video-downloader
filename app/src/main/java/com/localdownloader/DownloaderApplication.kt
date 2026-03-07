package com.localdownloader

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import com.localdownloader.utils.Logger
import com.localdownloader.worker.LoggingWorkerFactory
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@HiltAndroidApp
class DownloaderApplication : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()
        installUncaughtExceptionLogging()
        initializeWorkManager()
        getLoggerSafely()?.let { safeLogger ->
            safeLogger.ensureLogFilesExist()
            safeLogger.i("DownloaderApplication", "App started")
            safeLogger.i("DownloaderApplication", "File logs path: ${safeLogger.logFilePath()}")
            safeLogger.i("DownloaderApplication", "Crash logs path: ${safeLogger.crashLogFilePath()}")
            safeLogger.externalLogFilePathOrNull()?.let { path ->
                safeLogger.i("DownloaderApplication", "External logs path: $path")
            }
            safeLogger.externalCrashLogFilePathOrNull()?.let { path ->
                safeLogger.i("DownloaderApplication", "External crash logs path: $path")
            }
        }
        runCatching {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            getLoggerSafely()?.i("DownloaderApplication", "yt-dlp runtime initialization successful")
        }.onFailure { error ->
            getLoggerSafely()?.e("DownloaderApplication", "Failed to initialize yt-dlp runtime", error)
                ?: Log.e("DownloaderApplication", "Failed to initialize yt-dlp runtime", error)
        }
    }

    private fun initializeWorkManager() {
        val safeLogger = getLoggerSafely()
        val hiltFactory = getHiltWorkerFactorySafely()
        val delegateFactory = hiltFactory ?: FALLBACK_WORKER_FACTORY

        val configuration = androidx.work.Configuration.Builder()
            .setWorkerFactory(
                LoggingWorkerFactory(
                    delegate = delegateFactory,
                    logger = safeLogger,
                ),
            )
            .setMinimumLoggingLevel(Log.INFO)
            .build()

        runCatching {
            WorkManager.initialize(this, configuration)
            safeLogger?.i("DownloaderApplication", "WorkManager initialized with LoggingWorkerFactory")
                ?: Log.i("DownloaderApplication", "WorkManager initialized with LoggingWorkerFactory")
        }.onFailure { error ->
            if (error is IllegalStateException && error.message?.contains("already initialized", ignoreCase = true) == true) {
                safeLogger?.w("DownloaderApplication", "WorkManager already initialized")
                    ?: Log.w("DownloaderApplication", "WorkManager already initialized")
            } else {
                safeLogger?.e("DownloaderApplication", "Failed to initialize WorkManager", error)
                    ?: Log.e("DownloaderApplication", "Failed to initialize WorkManager", error)
            }
        }
    }

    private fun installUncaughtExceptionLogging() {
        val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            getLoggerSafely()?.e(
                "Crash",
                "Uncaught exception in thread '${thread.name}'",
                throwable,
            ) ?: Log.e("Crash", "Uncaught exception in thread '${thread.name}'", throwable)
            if (existingHandler != null) {
                existingHandler.uncaughtException(thread, throwable)
            } else {
                throw RuntimeException(throwable)
            }
        }
    }

    private fun getLoggerSafely(): Logger? {
        if (::logger.isInitialized) return logger
        return runCatching {
            EntryPointAccessors.fromApplication(
                this,
                StartupEntryPoint::class.java,
            ).logger()
        }.getOrNull()
    }

    private fun getHiltWorkerFactorySafely(): HiltWorkerFactory? {
        if (::workerFactory.isInitialized) return workerFactory
        return runCatching {
            EntryPointAccessors.fromApplication(
                this,
                StartupEntryPoint::class.java,
            ).hiltWorkerFactory()
        }.getOrNull()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StartupEntryPoint {
        fun hiltWorkerFactory(): HiltWorkerFactory
        fun logger(): Logger
    }

    private companion object {
        val FALLBACK_WORKER_FACTORY: WorkerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? = null
        }
    }
}
