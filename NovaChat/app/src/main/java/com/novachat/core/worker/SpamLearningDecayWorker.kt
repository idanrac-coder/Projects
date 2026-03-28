package com.novachat.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novachat.core.sms.ml.PersonalSpamAdapter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Runs once per day to apply exponential decay to keyword weights
 * and refresh the personal spam model.
 */
@HiltWorker
class SpamLearningDecayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val personalSpamAdapter: PersonalSpamAdapter
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        personalSpamAdapter.applyExponentialDecay()
        personalSpamAdapter.refresh()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "spam_learning_decay"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<SpamLearningDecayWorker>(
                1, TimeUnit.DAYS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
