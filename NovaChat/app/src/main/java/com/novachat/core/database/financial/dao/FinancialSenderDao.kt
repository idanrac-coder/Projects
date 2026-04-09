package com.novachat.core.database.financial.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novachat.core.database.financial.entity.FinancialSenderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialSenderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sender: FinancialSenderEntity): Long

    @Update
    suspend fun update(sender: FinancialSenderEntity)

    @Query("SELECT * FROM financial_senders WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): FinancialSenderEntity?

    @Query("SELECT * FROM financial_senders ORDER BY lastSeenTimestamp DESC")
    fun getAllSenders(): Flow<List<FinancialSenderEntity>>

    @Query("UPDATE financial_senders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE financial_senders SET alertsEnabled = :enabled WHERE id = :id")
    suspend fun setAlertsEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM financial_senders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM financial_senders")
    suspend fun deleteAll()
}
