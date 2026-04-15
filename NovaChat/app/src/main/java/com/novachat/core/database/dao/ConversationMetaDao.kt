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

    @Query("SELECT * FROM conversation_meta WHERE isMuted = 1 OR (muteUntil IS NOT NULL AND muteUntil > 0)")
    fun getMutedConversations(): Flow<List<ConversationMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: ConversationMetaEntity)

    @Query("UPDATE conversation_meta SET isPinned = :pinned WHERE threadId = :threadId")
    suspend fun setPinned(threadId: Long, pinned: Boolean)

    @Query("UPDATE conversation_meta SET isArchived = :archived WHERE threadId = :threadId")
    suspend fun setArchived(threadId: Long, archived: Boolean)

    @Query("UPDATE conversation_meta SET isMuted = :muted WHERE threadId = :threadId")
    suspend fun setMuted(threadId: Long, muted: Boolean)

    @Query("UPDATE conversation_meta SET muteUntil = :muteUntil WHERE threadId = :threadId")
    suspend fun setMuteUntil(threadId: Long, muteUntil: Long?)

    @Query("UPDATE conversation_meta SET isFavorite = :favorite WHERE threadId = :threadId")
    suspend fun setFavorite(threadId: Long, favorite: Boolean)

    @Query("UPDATE conversation_meta SET isLocked = :locked WHERE threadId = :threadId")
    suspend fun setLocked(threadId: Long, locked: Boolean)

    @Query("UPDATE conversation_meta SET customThemeId = :themeId WHERE threadId = :threadId")
    suspend fun setCustomTheme(threadId: Long, themeId: Long?)

    @Query("UPDATE conversation_meta SET autoDeleteAfterMs = :durationMs WHERE threadId = :threadId")
    suspend fun setAutoDelete(threadId: Long, durationMs: Long?)

    @Query("UPDATE conversation_meta SET customCategory = :category WHERE threadId = :threadId")
    suspend fun setCustomCategory(threadId: Long, category: String?)

    @Query("UPDATE conversation_meta SET lastReadTimestamp = :timestamp WHERE threadId = :threadId")
    suspend fun setLastReadTimestamp(threadId: Long, timestamp: Long)

    @Query("UPDATE conversation_meta SET lastReadMessageCount = :count WHERE threadId = :threadId")
    suspend fun setLastReadMessageCount(threadId: Long, count: Int)

    @Query("SELECT * FROM conversation_meta WHERE isArchived = 1")
    suspend fun getArchivedConversationsOnce(): List<ConversationMetaEntity>

    @Query("SELECT * FROM conversation_meta")
    suspend fun getAllMetas(): List<ConversationMetaEntity>
}
