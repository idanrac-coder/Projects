package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.novachat.core.database.entity.MessageReactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReactionDao {

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId")
    fun getReactionsForMessage(messageId: Long): Flow<List<MessageReactionEntity>>

    @Query("SELECT * FROM message_reactions WHERE messageId IN (:messageIds)")
    suspend fun getReactionsForMessages(messageIds: List<Long>): List<MessageReactionEntity>

    @Insert
    suspend fun insert(reaction: MessageReactionEntity): Long

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND emoji = :emoji")
    suspend fun removeReaction(messageId: Long, emoji: String)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId")
    suspend fun removeAllForMessage(messageId: Long)
}
