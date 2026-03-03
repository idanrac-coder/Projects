package com.novachat.core.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent != null) {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        context.startActivity(intent)
    }
    if (context is Activity) {
        context.finishAffinity()
    }
    Runtime.getRuntime().exit(0)
}

suspend fun exportDatabaseBackup(context: Context, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val db = context.getDatabasePath("novachat.db")
        val wal = File("${db.path}-wal")
        val shm = File("${db.path}-shm")

        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                addFileToZip(zip, db, "novachat.db")
                addFileToZip(zip, wal, "novachat.db-wal")
                addFileToZip(zip, shm, "novachat.db-shm")
            }
        } ?: error("Unable to open destination")
    }.isSuccess
}

suspend fun importDatabaseBackup(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val dbFile = context.getDatabasePath("novachat.db")
        val walFile = File("${dbFile.path}-wal")
        val shmFile = File("${dbFile.path}-shm")

        val tempDir = File(context.cacheDir, "restore_temp")
        tempDir.mkdirs()

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                var foundDb = false
                while (entry != null) {
                    val name = entry.name
                    if (name == "novachat.db" || name == "novachat.db-wal" || name == "novachat.db-shm") {
                        val outFile = File(tempDir, name)
                        outFile.outputStream().use { out -> zip.copyTo(out) }
                        if (name == "novachat.db") foundDb = true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                if (!foundDb) error("Invalid backup: novachat.db not found in archive")
            }
        } ?: error("Unable to open backup file")

        walFile.delete()
        shmFile.delete()

        val tempDb = File(tempDir, "novachat.db")
        val tempWal = File(tempDir, "novachat.db-wal")
        val tempShm = File(tempDir, "novachat.db-shm")

        tempDb.copyTo(dbFile, overwrite = true)
        if (tempWal.exists()) tempWal.copyTo(walFile, overwrite = true)
        if (tempShm.exists()) tempShm.copyTo(shmFile, overwrite = true)

        tempDir.deleteRecursively()
    }.isSuccess
}

private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
    if (!file.exists()) return
    zip.putNextEntry(ZipEntry(entryName))
    file.inputStream().use { input -> input.copyTo(zip) }
    zip.closeEntry()
}
