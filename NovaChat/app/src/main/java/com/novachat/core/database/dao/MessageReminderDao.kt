package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.novachat.core.database.entity.MessageReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReminderDao {

    @Query("SELECT * FROM message_reminders WHERE isTriggered = 0 ORDER BY reminderTime ASC")
    fun getPendingReminders(): Flow<List<MessageReminderEntity>>

    @Query("SELECT * FROM message_reminders WHERE isTriggered = 0 AND reminderTime <= :currentTime")
    suspend fun getDueReminders(currentTime: Long): List<MessageReminderEntity>

    @Insert
    suspend fun insert(reminder: MessageReminderEntity): Long

    @Query("UPDATE message_reminders SET isTriggered = 1 WHERE id = :id")
    suspend fun markAsTriggered(id: Long)

    @Query("DELETE FROM message_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
