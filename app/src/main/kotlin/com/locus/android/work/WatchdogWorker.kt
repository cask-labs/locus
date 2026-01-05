package com.locus.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WatchdogWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            // Stub implementation for Phase 1
            return Result.success()
        }
    }
