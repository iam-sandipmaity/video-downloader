package com.localdownloader.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.localdownloader.data.DownloadTaskStore
import com.localdownloader.domain.models.blocksRuntimeUpdates
import com.localdownloader.updates.UpdatePreferencesStore
import com.localdownloader.updates.YtDlpReleaseChannel
import com.localdownloader.updates.YtDlpUpdateManager
import com.localdownloader.utils.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class YtDlpUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadTaskStore: DownloadTaskStore,
    private val updatePreferencesStore: UpdatePreferencesStore,
    private val updateStateStore: YtDlpUpdateStateStore,
    private val ytDlpUpdateManager: YtDlpUpdateManager,
    private val logger: Logger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val attemptNumber = runAttemptCount + 1
        updateStateStore.markAttemptStarted()
        logger.i("YtDlpUpdateWorker", "Starting yt-dlp runtime update attempt=$attemptNumber")

        downloadTaskStore.awaitInitialLoad()
        val blockingDownloadCount = downloadTaskStore.getAllTasks().count { task ->
            task.status.blocksRuntimeUpdates()
        }
        if (blockingDownloadCount > 0) {
            updateStateStore.markAttemptDeferred("active_downloads")
            logger.i(
                "YtDlpUpdateWorker",
                "Deferring yt-dlp update because $blockingDownloadCount download(s) are queued, running, or paused",
            )
            return retryOrFinish(
                reason = "downloads are still queued, running, or paused",
                attemptNumber = attemptNumber,
            )
        }

        val channel = YtDlpReleaseChannel.fromId(
            inputData.getString(KEY_CHANNEL_ID)
                ?: updatePreferencesStore.currentPreferences().ytDlpChannel.id,
        )

        return runCatching {
            val result = ytDlpUpdateManager.installUpdate(channel)
            updateStateStore.markAttemptSucceeded(result.message)
            logger.i("YtDlpUpdateWorker", "yt-dlp runtime update finished with status=${result.message}")
            Result.success()
        }.getOrElse { error ->
            val errorMessage = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
            updateStateStore.markAttemptFailed(errorMessage)
            logger.e("YtDlpUpdateWorker", "yt-dlp runtime update failed attempt=$attemptNumber", error)
            retryOrFinish(reason = errorMessage, attemptNumber = attemptNumber)
        }
    }

    private fun retryOrFinish(reason: String, attemptNumber: Int): Result {
        return if (attemptNumber < MAX_UPDATE_ATTEMPTS) {
            logger.i(
                "YtDlpUpdateWorker",
                "Retrying yt-dlp update later (attempt=$attemptNumber/$MAX_UPDATE_ATTEMPTS, reason=$reason)",
            )
            Result.retry()
        } else {
            logger.w(
                "YtDlpUpdateWorker",
                "Stopping yt-dlp update retries after $attemptNumber attempt(s): $reason",
            )
            Result.success()
        }
    }

    companion object {
        private const val MAX_UPDATE_ATTEMPTS = 4
        const val KEY_CHANNEL_ID = "channel_id"
    }
}
