package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.novachat.core.database.entity.ScheduledMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {

    @Query("SELECT * FROM scheduled_messages WHERE isSent = 0 AND isFailed = 0 ORDER BY scheduledTime ASC")
    fun getPendingMessages(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledTime DESC")
    fun getAllMessages(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE isSent = 0 AND isFailed = 0 AND scheduledTime <= :currentTime")
    suspend fun getDueMessages(currentTime: Long): List<ScheduledMessageEntity>

    @Insert
    suspend fun insert(message: ScheduledMessageEntity): Long

    @Query("UPDATE scheduled_messages SET isSent = 1 WHERE id = :id")
    suspend fun markAsSent(id: Long)

    @Query("UPDATE scheduled_messages SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("UPDATE scheduled_messages SET isFailed = 1 WHERE id = :id")
    suspend fun markAsFailed(id: Long)

    @Delete
    suspend fun delete(message: ScheduledMessageEntity)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
