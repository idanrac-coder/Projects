package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.novachat.core.database.entity.MessageEditEntity

@Dao
interface MessageEditDao {
    @Insert
    suspend fun insert(edit: MessageEditEntity): Long

    @Query("SELECT * FROM message_edits WHERE messageId = :messageId ORDER BY timestamp ASC")
    suspend fun getEditsForMessage(messageId: Long): List<MessageEditEntity>

    @Query("SELECT DISTINCT messageId FROM message_edits WHERE messageId IN (:messageIds)")
    suspend fun getEditedMessageIds(messageIds: List<Long>): List<Long>

    @Query("DELETE FROM message_edits WHERE messageId = :messageId")
    suspend fun deleteEditsForMessage(messageId: Long)
}
