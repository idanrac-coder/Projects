package com.novachat.core.database.financial.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novachat.core.database.financial.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM cards WHERE last4 = :last4")
    suspend fun getByLast4(last4: String): CardEntity?

    @Query("SELECT * FROM cards ORDER BY sortOrder ASC, lastSeenTimestamp DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards")
    fun getCardCount(): Flow<Int>

    @Query("UPDATE cards SET nickname = :nickname WHERE last4 = :last4")
    suspend fun updateNickname(last4: String, nickname: String)

    @Query("UPDATE cards SET isHidden = :isHidden WHERE last4 = :last4")
    suspend fun setHidden(last4: String, isHidden: Boolean)

    @Query("UPDATE cards SET sortOrder = :sortOrder WHERE last4 = :last4")
    suspend fun updateSortOrder(last4: String, sortOrder: Int)

    @Query("DELETE FROM cards WHERE last4 = :last4")
    suspend fun delete(last4: String)

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM financial_transactions t WHERE t.cardLast4 = :cardLast4 AND t.isConversion = 0")
    suspend fun getTotalSpentForCard(cardLast4: String): Double

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}
