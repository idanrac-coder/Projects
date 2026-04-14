package com.novachat.core.database.financial.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novachat.core.database.financial.entity.FinancialTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: FinancialTransactionEntity): Long

    @Update
    suspend fun update(transaction: FinancialTransactionEntity)

    @Query("SELECT * FROM financial_transactions WHERE id = :id")
    suspend fun getById(id: Long): FinancialTransactionEntity?

    @Query("SELECT COUNT(*) > 0 FROM financial_transactions WHERE smsId = :smsId")
    suspend fun existsBySmsId(smsId: Long): Boolean

    @Query("SELECT * FROM financial_transactions WHERE isConversion = 0 AND timestamp BETWEEN :startMs AND :endMs AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0) ORDER BY timestamp DESC")
    fun getTransactionsInRange(startMs: Long, endMs: Long, cardLast4: String?): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions WHERE isConversion = 0 AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0) ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int, cardLast4: String?): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions WHERE isConversion = 0 AND timestamp BETWEEN :startMs AND :endMs AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0) ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactionsInRange(startMs: Long, endMs: Long, limit: Int, cardLast4: String?): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM financial_transactions WHERE isConversion = 0 AND timestamp BETWEEN :startMs AND :endMs AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0)")
    fun getTotalInRange(startMs: Long, endMs: Long, cardLast4: String?): Flow<Double>

    @Query("SELECT COUNT(*) FROM financial_transactions WHERE isConversion = 0 AND timestamp BETWEEN :startMs AND :endMs AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0)")
    fun getCountInRange(startMs: Long, endMs: Long, cardLast4: String?): Flow<Int>

    @Query("SELECT category, COALESCE(SUM(amount), 0.0) as total, COUNT(*) as count FROM financial_transactions WHERE isConversion = 0 AND timestamp BETWEEN :startMs AND :endMs AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0) GROUP BY category")
    fun getCategoryBreakdown(startMs: Long, endMs: Long, cardLast4: String?): Flow<List<CategoryTotal>>

    @Query("SELECT CAST(strftime('%d', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) as dayOfMonth, COALESCE(SUM(amount), 0.0) as total FROM financial_transactions WHERE isConversion = 0 AND timestamp BETWEEN :startMs AND :endMs AND (:cardLast4 IS NULL OR cardLast4 = :cardLast4) AND sender NOT IN (SELECT address FROM financial_senders WHERE isEnabled = 0) GROUP BY dayOfMonth ORDER BY dayOfMonth")
    fun getDailySpending(startMs: Long, endMs: Long, cardLast4: String?): Flow<List<DailyTotal>>

    @Query("SELECT * FROM financial_transactions WHERE merchantName = :merchantName AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getTransactionsByMerchant(merchantName: String, since: Long): List<FinancialTransactionEntity>

    @Query("SELECT * FROM financial_transactions WHERE merchantName = :merchantName AND amount = :amount AND timestamp BETWEEN :start AND :end AND id != :excludeId LIMIT 1")
    suspend fun findDuplicate(merchantName: String, amount: Double, start: Long, end: Long, excludeId: Long): FinancialTransactionEntity?

    @Query("SELECT COALESCE(AVG(amount), 0.0) FROM financial_transactions WHERE isConversion = 0")
    suspend fun getGlobalAverageAmount(): Double

    @Query("SELECT COUNT(*) FROM financial_transactions WHERE category = :category AND isConversion = 0")
    fun getCategoryCount(category: String): Flow<Int>

    @Query("UPDATE financial_transactions SET isRecurring = 1 WHERE id = :id")
    suspend fun markRecurring(id: Long)

    @Query("DELETE FROM financial_transactions WHERE sender = :senderAddress")
    suspend fun deleteBySender(senderAddress: String)

    @Query("DELETE FROM financial_transactions")
    suspend fun deleteAll()
}

data class CategoryTotal(val category: String, val total: Double, val count: Int)
data class DailyTotal(val dayOfMonth: Int, val total: Double)
