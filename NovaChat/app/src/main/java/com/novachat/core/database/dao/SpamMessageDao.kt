package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.SpamMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamMessageDao {

    @Query("SELECT * FROM spam_messages ORDER BY timestamp DESC")
    fun getAllSpamMessages(): Flow<List<SpamMessageEntity>>

    @Query("SELECT COUNT(*) FROM spam_messages")
    fun getSpamCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamMessage(message: SpamMessageEntity): Long

    @Query("SELECT * FROM spam_messages WHERE id = :id")
    suspend fun getSpamMessageById(id: Long): SpamMessageEntity?

    @Query("DELETE FROM spam_messages WHERE id = :id")
    suspend fun deleteSpamMessageById(id: Long)

    @Query("SELECT smsId FROM spam_messages")
    suspend fun getReportedSmsIds(): List<Long>

    @Query("SELECT * FROM spam_messages WHERE matchedRuleId = :ruleId")
    suspend fun getSpamMessagesByRuleId(ruleId: Long): List<SpamMessageEntity>

    @Query("DELETE FROM spam_messages WHERE matchedRuleId = :ruleId")
    suspend fun deleteByRuleId(ruleId: Long)

    @Query("DELETE FROM spam_messages")
    suspend fun clearAll()
}
