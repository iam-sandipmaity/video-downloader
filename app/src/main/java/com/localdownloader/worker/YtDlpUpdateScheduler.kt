package com.localdownloader.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.localdownloader.updates.UpdatePreferencesStore
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpUpdateScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updatePreferencesStore: UpdatePreferencesStore,
    private val updateStateStore: YtDlpUpdateStateStore,
    private val logger: Logger,
) {

    fun scheduleIfDue(nowEpochMs: Long = System.currentTimeMillis()) {
        val preferences = updatePreferencesStore.currentPreferences()
        if (!preferences.autoUpdateYtDlp) {
            logger.i("YtDlpUpdateScheduler", "Skipping yt-dlp update schedule because auto-update is disabled")
            return
        }

        val lastAttemptStartedAt = updateStateStore.lastAttemptStartedAtEpochMs()
        if (lastAttemptStartedAt > 0L && nowEpochMs - lastAttemptStartedAt < UPDATE_CHECK_INTERVAL_MS) {
            logger.i(
                "YtDlpUpdateScheduler",
                "Skipping yt-dlp update schedule; last attempt was ${nowEpochMs - lastAttemptStartedAt}ms ago",
            )
            return
        }

        val request = OneTimeWorkRequestBuilder<YtDlpUpdateWorker>()
            .setInputData(
                workDataOf(
                    YtDlpUpdateWorker.KEY_CHANNEL_ID to preferences.ytDlpChannel.id,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                UPDATE_RETRY_BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }.onSuccess {
            logger.i("YtDlpUpdateScheduler", "Enqueued yt-dlp runtime update check")
        }.onFailure { error ->
            logger.w("YtDlpUpdateScheduler", "Skipping yt-dlp update schedule because WorkManager was unavailable", error)
        }
    }

    fun cancelScheduled() {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }.onSuccess {
            logger.i("YtDlpUpdateScheduler", "Cancelled scheduled yt-dlp update work")
        }.onFailure { error ->
            logger.w("YtDlpUpdateScheduler", "Failed cancelling yt-dlp update work", error)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "ytdlp-runtime-update"
        private val UPDATE_CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(12)
        private const val UPDATE_RETRY_BACKOFF_MINUTES = 30L
    }
}
