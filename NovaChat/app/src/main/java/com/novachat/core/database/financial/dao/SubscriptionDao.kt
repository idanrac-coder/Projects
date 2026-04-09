package com.novachat.core.database.financial.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novachat.core.database.financial.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Query("SELECT * FROM subscriptions WHERE merchantName = :merchantName AND ((:cardLast4 IS NULL AND cardLast4 IS NULL) OR cardLast4 = :cardLast4)")
    suspend fun getByMerchantAndCard(merchantName: String, cardLast4: String?): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) ORDER BY lastSeenTimestamp DESC")
    fun getActiveSubscriptions(cardLast4: String?): Flow<List<SubscriptionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM subscriptions WHERE isActive = 1 AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4)")
    fun getActiveTotal(cardLast4: String?): Flow<Double>

    @Query("SELECT COUNT(*) FROM subscriptions WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>

    @Query("UPDATE subscriptions SET isActive = 0 WHERE id = :id")
    suspend fun markInactive(id: Long)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}
