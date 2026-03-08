package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.ShortCodeWhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortCodeWhitelistDao {

    @Query("SELECT EXISTS(SELECT 1 FROM short_code_whitelist WHERE address = :address)")
    suspend fun isWhitelisted(address: String): Boolean

    @Query("SELECT * FROM short_code_whitelist ORDER BY address ASC")
    fun getAll(): Flow<List<ShortCodeWhitelistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ShortCodeWhitelistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ShortCodeWhitelistEntity>)

    @Query("DELETE FROM short_code_whitelist WHERE address = :address")
    suspend fun deleteByAddress(address: String)
}
