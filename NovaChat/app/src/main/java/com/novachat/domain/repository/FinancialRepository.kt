package com.novachat.domain.repository

import com.novachat.domain.model.AlertInfo
import com.novachat.domain.model.CardInfo
import com.novachat.domain.model.CategoryBreakdown
import com.novachat.domain.model.DailySpending
import com.novachat.domain.model.MonthlySummary
import com.novachat.domain.model.SenderInfo
import com.novachat.domain.model.SubscriptionInfo
import com.novachat.domain.model.TopMerchant
import com.novachat.domain.model.TransactionInfo
import kotlinx.coroutines.flow.Flow

interface FinancialRepository {
    fun getMonthlySummary(year: Int, month: Int, cardLast4: String?): Flow<MonthlySummary>
    fun getCategoryBreakdown(year: Int, month: Int, cardLast4: String?): Flow<List<CategoryBreakdown>>
    fun getDailySpending(year: Int, month: Int, cardLast4: String?): Flow<List<DailySpending>>
    fun getTopMerchants(year: Int, month: Int, cardLast4: String?, limit: Int = 5): Flow<List<TopMerchant>>
    fun getRecentTransactions(limit: Int, cardLast4: String?): Flow<List<TransactionInfo>>
    fun getRecentTransactionsForMonth(year: Int, month: Int, limit: Int, cardLast4: String?): Flow<List<TransactionInfo>>

    fun getActiveSubscriptions(cardLast4: String?): Flow<List<SubscriptionInfo>>
    fun getSubscriptionTotal(cardLast4: String?): Flow<Double>
    suspend fun markSubscriptionInactive(id: Long)

    fun getActiveAlerts(): Flow<List<AlertInfo>>
    fun getAlertCount(): Flow<Int>
    suspend fun dismissAlert(id: Long)

    fun getAllCards(): Flow<List<CardInfo>>
    fun getCardCount(): Flow<Int>
    suspend fun updateCardNickname(last4: String, nickname: String)
    suspend fun setCardHidden(last4: String, isHidden: Boolean)
    suspend fun addCard(last4: String, nickname: String?)
    suspend fun deleteCard(last4: String)
    suspend fun reorderCards(orderedLast4s: List<String>)

    fun getAllSenders(): Flow<List<SenderInfo>>
    suspend fun setSenderEnabled(id: Long, enabled: Boolean)
    suspend fun setSenderAlertsEnabled(id: Long, enabled: Boolean)
    suspend fun addSender(address: String, displayName: String?)
    suspend fun removeSender(id: Long)

    suspend fun clearAllFinancialData()
    fun getCategoryCounts(): Flow<Map<String, Int>>

    suspend fun updateMerchantCategory(merchantName: String, category: String)
}
