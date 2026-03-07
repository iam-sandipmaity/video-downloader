package com.localdownloader.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class WorkerInitFailureWorker(
    appContext: Context,
    params: WorkerParameters,
    private val failureReason: String,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return Result.failure(workDataOf(WorkerKeys.ERROR_MESSAGE to failureReason))
    }
}
