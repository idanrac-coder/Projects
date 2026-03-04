package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.SenderAllowlistEntity
import kotlinx.coroutines.flow.Flow
import com.novachat.core.database.entity.SpamKeywordWeightEntity
import com.novachat.core.database.entity.SpamLearningEntity
import com.novachat.core.database.entity.SpamSenderReputationEntity

@Dao
interface SpamLearningDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningEntry(entry: SpamLearningEntity)

    @Query("SELECT * FROM spam_learning ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int = 500): List<SpamLearningEntity>

    @Query("SELECT COUNT(*) FROM spam_learning WHERE isSpam = 1")
    suspend fun getSpamTrainingCount(): Int

    @Query("SELECT COUNT(*) FROM spam_learning WHERE isSpam = 0")
    suspend fun getHamTrainingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSenderReputation(reputation: SpamSenderReputationEntity)

    @Query("SELECT * FROM spam_sender_reputation WHERE address = :address")
    suspend fun getSenderReputation(address: String): SpamSenderReputationEntity?

    @Query("SELECT * FROM spam_sender_reputation WHERE spamCount > 0 ORDER BY spamCount DESC")
    suspend fun getKnownSpamSenders(): List<SpamSenderReputationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeywordWeight(weight: SpamKeywordWeightEntity)

    @Query("SELECT * FROM spam_keyword_weights WHERE keyword = :keyword")
    suspend fun getKeywordWeight(keyword: String): SpamKeywordWeightEntity?

    @Query("SELECT * FROM spam_keyword_weights WHERE weight > 0.3 ORDER BY weight DESC LIMIT 200")
    suspend fun getSpamKeywords(): List<SpamKeywordWeightEntity>

    @Query("SELECT * FROM spam_keyword_weights ORDER BY weight DESC")
    suspend fun getAllKeywordWeights(): List<SpamKeywordWeightEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToAllowlist(entry: SenderAllowlistEntity)

    @Query("DELETE FROM sender_allowlist WHERE address = :address")
    suspend fun removeFromAllowlist(address: String)

    @Query("SELECT EXISTS(SELECT 1 FROM sender_allowlist WHERE address = :address)")
    suspend fun isAllowlisted(address: String): Boolean

    @Query("SELECT * FROM sender_allowlist ORDER BY createdAt DESC")
    suspend fun getAllAllowlisted(): List<SenderAllowlistEntity>

    @Query("SELECT * FROM sender_allowlist ORDER BY createdAt DESC")
    fun observeAllowlist(): Flow<List<SenderAllowlistEntity>>
}
