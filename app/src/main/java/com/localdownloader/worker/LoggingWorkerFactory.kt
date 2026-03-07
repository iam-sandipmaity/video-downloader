package com.localdownloader.worker

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.localdownloader.utils.Logger

/**
 * Adds visibility for worker creation failures that otherwise appear as generic WorkManager FAILED states.
 */
class LoggingWorkerFactory(
    private val delegate: WorkerFactory,
    private val logger: Logger?,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        val creationResult = runCatching {
            delegate.createWorker(appContext, workerClassName, workerParameters)
        }.onFailure { error ->
            logError(
                "Failed creating worker class=$workerClassName id=${workerParameters.id}",
                error,
            )
        }

        return creationResult.getOrNull()
            ?.also {
                logInfo("Created worker class=$workerClassName id=${workerParameters.id}")
            }
            ?: fallbackForDownloadWorker(
                appContext = appContext,
                workerClassName = workerClassName,
                workerParameters = workerParameters,
                creationError = creationResult.exceptionOrNull(),
            )
    }

    private fun fallbackForDownloadWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
        creationError: Throwable?,
    ): ListenableWorker? {
        if (workerClassName != DownloadWorker::class.java.name) return null
        val reasonSuffix = creationError?.message
            ?.takeIf { it.isNotBlank() }
            ?.let { ": $it" }
            ?: ""
        val reason = "Worker initialization failed for $workerClassName (id=${workerParameters.id})$reasonSuffix"
        logError(reason, creationError)
        return WorkerInitFailureWorker(
            appContext = appContext,
            params = workerParameters,
            failureReason = reason,
        )
    }

    private fun logInfo(message: String) {
        logger?.i("LoggingWorkerFactory", message) ?: Log.i("LoggingWorkerFactory", message)
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        logger?.e("LoggingWorkerFactory", message, throwable)
            ?: Log.e("LoggingWorkerFactory", message, throwable)
    }
}
