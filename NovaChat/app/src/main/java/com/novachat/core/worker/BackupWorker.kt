package com.novachat.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novachat.core.datastore.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "BackupWorker"
        private const val WORK_NAME = "novachat_auto_backup"
        private const val BACKUP_FILE_NAME = "novachat-backup.zip"

        fun schedule(context: Context, frequency: String) {
            val intervalHours = when (frequency) {
                "daily" -> 24L
                "weekly" -> 24L * 7
                "monthly" -> 24L * 30
                else -> 24L
            }

            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                intervalHours, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "Scheduled auto-backup: $frequency (every ${intervalHours}h)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled auto-backup")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val backupDir = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
            val backupFile = File(backupDir, BACKUP_FILE_NAME)

            val dbFile = applicationContext.getDatabasePath("novachat.db")
            val walFile = File("${dbFile.path}-wal")
            val shmFile = File("${dbFile.path}-shm")

            if (!dbFile.exists()) {
                Log.w(TAG, "Database file not found, skipping backup")
                return Result.success()
            }

            backupFile.outputStream().use { output ->
                ZipOutputStream(output).use { zip ->
                    addFileToZip(zip, dbFile, "novachat.db")
                    addFileToZip(zip, walFile, "novachat.db-wal")
                    addFileToZip(zip, shmFile, "novachat.db-shm")
                }
            }

            preferencesRepository.setLastBackupTime(System.currentTimeMillis())
            Log.d(TAG, "Auto-backup completed: ${backupFile.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto-backup failed", e)
            Result.retry()
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input -> input.copyTo(zip) }
        zip.closeEntry()
    }
}
