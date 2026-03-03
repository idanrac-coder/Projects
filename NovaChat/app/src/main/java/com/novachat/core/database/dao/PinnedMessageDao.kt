package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.PinnedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedMessageDao {

    @Query("SELECT * FROM pinned_messages WHERE threadId = :threadId ORDER BY pinnedAt DESC")
    fun getPinnedForThread(threadId: Long): Flow<List<PinnedMessageEntity>>

    @Query("SELECT messageId FROM pinned_messages WHERE threadId = :threadId")
    suspend fun getPinnedMessageIds(threadId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pin(entity: PinnedMessageEntity)

    @Query("DELETE FROM pinned_messages WHERE messageId = :messageId")
    suspend fun unpin(messageId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_messages WHERE messageId = :messageId)")
    suspend fun isPinned(messageId: Long): Boolean
}
