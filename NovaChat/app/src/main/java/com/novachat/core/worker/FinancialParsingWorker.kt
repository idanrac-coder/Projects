package com.novachat.core.worker

import android.content.Context
import android.provider.Telephony
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.financial.FinancialSmsParser
import com.novachat.core.sms.financial.RecurrenceDetector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import java.util.concurrent.TimeUnit

@HiltWorker
class FinancialParsingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val financialSmsParser: FinancialSmsParser,
    private val recurrenceDetector: RecurrenceDetector,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            backfillSms()
            recurrenceDetector.detectAll()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun backfillSms() {
        val lastParsedId = userPreferencesRepository.financialLastParsedSmsId.first()
        val resolver = applicationContext.contentResolver
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms._ID} > ?"
        val selectionArgs = arrayOf(lastParsedId.toString())
        val sortOrder = "${Telephony.Sms._ID} ASC LIMIT 100"

        var maxId = lastParsedId

        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val smsId = cursor.getLong(idIdx)
                val address = cursor.getString(addressIdx) ?: continue
                val body = cursor.getString(bodyIdx) ?: continue
                val date = cursor.getLong(dateIdx)

                financialSmsParser.parse(smsId, address, body, date)
                maxId = maxOf(maxId, smsId)
                yield()
            }
        }

        if (maxId > lastParsedId) {
            userPreferencesRepository.setFinancialLastParsedSmsId(maxId)
        }
    }

    companion object {
        private const val WORK_NAME = "financial_parsing"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<FinancialParsingWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
