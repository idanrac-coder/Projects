package com.novachat.core.database.financial.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.novachat.core.database.financial.entity.FinancialAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialAlertDao {

    @Insert
    suspend fun insert(alert: FinancialAlertEntity): Long

    @Query("SELECT * FROM financial_alerts WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<FinancialAlertEntity>>

    @Query("SELECT COUNT(*) FROM financial_alerts WHERE isDismissed = 0")
    fun getActiveCount(): Flow<Int>

    @Query("UPDATE financial_alerts SET isDismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("DELETE FROM financial_alerts")
    suspend fun deleteAll()
}
