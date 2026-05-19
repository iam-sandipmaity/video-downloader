package com.localdownloader.worker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpUpdateStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun lastAttemptStartedAtEpochMs(): Long {
        return prefs.getLong(KEY_LAST_ATTEMPT_STARTED_AT_EPOCH_MS, 0L)
    }

    fun lastAttemptFinishedAtEpochMs(): Long {
        return prefs.getLong(KEY_LAST_ATTEMPT_FINISHED_AT_EPOCH_MS, 0L)
    }

    fun lastStatus(): String? {
        return prefs.getString(KEY_LAST_STATUS, null)
    }

    fun markAttemptStarted(nowEpochMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_ATTEMPT_STARTED_AT_EPOCH_MS, nowEpochMs)
            .putString(KEY_LAST_STATUS, STATUS_RUNNING)
            .apply()
    }

    fun markAttemptDeferred(reason: String, nowEpochMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_ATTEMPT_FINISHED_AT_EPOCH_MS, nowEpochMs)
            .putString(KEY_LAST_STATUS, "deferred:$reason")
            .apply()
    }

    fun markAttemptSucceeded(status: String, nowEpochMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_ATTEMPT_FINISHED_AT_EPOCH_MS, nowEpochMs)
            .putLong(KEY_LAST_SUCCESS_AT_EPOCH_MS, nowEpochMs)
            .putString(KEY_LAST_STATUS, status)
            .apply()
    }

    fun markAttemptFailed(reason: String, nowEpochMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_ATTEMPT_FINISHED_AT_EPOCH_MS, nowEpochMs)
            .putString(KEY_LAST_STATUS, "failed:$reason")
            .apply()
    }

    private val prefs by lazy(LazyThreadSafetyMode.NONE) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        private const val PREFS_NAME = "ytdlp_update_state"
        private const val KEY_LAST_ATTEMPT_STARTED_AT_EPOCH_MS = "last_attempt_started_at_epoch_ms"
        private const val KEY_LAST_ATTEMPT_FINISHED_AT_EPOCH_MS = "last_attempt_finished_at_epoch_ms"
        private const val KEY_LAST_SUCCESS_AT_EPOCH_MS = "last_success_at_epoch_ms"
        private const val KEY_LAST_STATUS = "last_status"
        private const val STATUS_RUNNING = "running"
    }
}
