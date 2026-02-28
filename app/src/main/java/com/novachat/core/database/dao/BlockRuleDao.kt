package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.BlockRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {

    @Query("SELECT * FROM block_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<BlockRuleEntity>>

    @Query("SELECT * FROM block_rules WHERE type = :type ORDER BY createdAt DESC")
    fun getRulesByType(type: String): Flow<List<BlockRuleEntity>>

    @Query("SELECT * FROM block_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): BlockRuleEntity?

    @Query("SELECT COUNT(*) FROM block_rules")
    fun getRuleCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: BlockRuleEntity): Long

    @Delete
    suspend fun deleteRule(rule: BlockRuleEntity)

    @Query("DELETE FROM block_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
