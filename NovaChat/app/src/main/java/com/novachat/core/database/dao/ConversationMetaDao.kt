package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.ConversationMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMetaDao {

    @Query("SELECT * FROM conversation_meta WHERE threadId = :threadId")
    suspend fun getMetaForThread(threadId: Long): ConversationMetaEntity?

    @Query("SELECT * FROM conversation_meta WHERE isPinned = 1")
    fun getPinnedConversations(): Flow<List<ConversationMetaEntity>>

    @Query("SELECT * FROM conversation_meta WHERE isArchived = 1")
    fun getArchivedConversations(): Flow<List<ConversationMetaEntity>>

    @Query("SELECT * FROM conversation_meta WHERE isMuted = 1")
    fun getMutedConversations(): Flow<List<ConversationMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: ConversationMetaEntity)

    @Query("UPDATE conversation_meta SET isPinned = :pinned WHERE threadId = :threadId")
    suspend fun setPinned(threadId: Long, pinned: Boolean)

    @Query("UPDATE conversation_meta SET isArchived = :archived WHERE threadId = :threadId")
    suspend fun setArchived(threadId: Long, archived: Boolean)

    @Query("UPDATE conversation_meta SET isMuted = :muted WHERE threadId = :threadId")
    suspend fun setMuted(threadId: Long, muted: Boolean)
}
