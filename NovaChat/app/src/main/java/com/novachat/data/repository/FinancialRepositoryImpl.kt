package com.novachat.data.repository

import com.novachat.core.analytics.financial.SpendingAnalyzer
import com.novachat.core.database.financial.dao.CardDao
import com.novachat.core.database.financial.dao.FinancialAlertDao
import com.novachat.core.database.financial.dao.FinancialSenderDao
import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.MerchantDao
import com.novachat.core.database.financial.dao.SubscriptionDao
import com.novachat.core.database.financial.entity.FinancialSenderEntity
import com.novachat.core.sms.financial.FinancialCategory
import com.novachat.domain.model.AlertInfo
import com.novachat.domain.model.CardInfo
import com.novachat.domain.model.CategoryBreakdown
import com.novachat.domain.model.DailySpending
import com.novachat.domain.model.MonthlySummary
import com.novachat.domain.model.SenderInfo
import com.novachat.domain.model.SubscriptionInfo
import com.novachat.domain.model.SubscriptionSummary
import com.novachat.domain.model.TopMerchant
import com.novachat.domain.model.TransactionInfo
import com.novachat.domain.repository.FinancialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialRepositoryImpl @Inject constructor(
    private val transactionDao: FinancialTransactionDao,
    private val subscriptionDao: SubscriptionDao,
    private val merchantDao: MerchantDao,
    private val cardDao: CardDao,
    private val senderDao: FinancialSenderDao,
    private val alertDao: FinancialAlertDao,
    private val spendingAnalyzer: SpendingAnalyzer
) : FinancialRepository {

    override fun getMonthlySummary(year: Int, month: Int, cardLast4: String?): Flow<MonthlySummary> =
        spendingAnalyzer.getMonthlySummary(year, month, cardLast4)

    override fun getCategoryBreakdown(year: Int, month: Int, cardLast4: String?): Flow<List<CategoryBreakdown>> =
        spendingAnalyzer.getCategoryBreakdown(year, month, cardLast4)

    override fun getDailySpending(year: Int, month: Int, cardLast4: String?): Flow<List<DailySpending>> =
        spendingAnalyzer.getDailySpending(year, month, cardLast4)

    override fun getTopMerchants(year: Int, month: Int, cardLast4: String?, limit: Int): Flow<List<TopMerchant>> =
        spendingAnalyzer.getTopMerchants(year, month, cardLast4, limit)

    override fun getRecentTransactionsForMonth(year: Int, month: Int, limit: Int, cardLast4: String?): Flow<List<TransactionInfo>> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis
        return combine(
            transactionDao.getRecentTransactionsInRange(startMs, endMs, limit, cardLast4),
            cardDao.getAllCards()
        ) { transactions, cards ->
            val cardMap = cards.associate { it.last4 to it.nickname }
            transactions.map { tx ->
                TransactionInfo(
                    id = tx.id,
                    merchantName = tx.merchantName,
                    amount = tx.amount,
                    currency = tx.currency,
                    category = tx.category,
                    timestamp = tx.timestamp,
                    cardLast4 = tx.cardLast4,
                    cardNickname = tx.cardLast4?.let { cardMap[it] },
                    isRecurring = tx.isRecurring,
                    senderAddress = tx.sender
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRecentTransactions(limit: Int, cardLast4: String?): Flow<List<TransactionInfo>> =
        combine(
            transactionDao.getRecentTransactions(limit, cardLast4),
            cardDao.getAllCards()
        ) { transactions, cards ->
            val cardMap = cards.associate { it.last4 to it.nickname }
            transactions.map { tx ->
                TransactionInfo(
                    id = tx.id,
                    merchantName = tx.merchantName,
                    amount = tx.amount,
                    currency = tx.currency,
                    category = tx.category,
                    timestamp = tx.timestamp,
                    cardLast4 = tx.cardLast4,
                    cardNickname = tx.cardLast4?.let { cardMap[it] },
                    isRecurring = tx.isRecurring,
                    senderAddress = tx.sender
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun getActiveSubscriptions(cardLast4: String?): Flow<List<SubscriptionInfo>> =
        combine(
            subscriptionDao.getActiveSubscriptions(cardLast4),
            cardDao.getAllCards()
        ) { subs, cards ->
            val cardMap = cards.associate { it.last4 to it.nickname }
            subs.map { sub ->
                SubscriptionInfo(
                    id = sub.id,
                    merchantName = sub.merchantName,
                    amount = sub.amount,
                    previousAmount = sub.previousAmount,
                    currency = sub.currency,
                    frequency = sub.frequency,
                    lastCharged = sub.lastSeenTimestamp,
                    cardLast4 = sub.cardLast4,
                    cardNickname = sub.cardLast4?.let { cardMap[it] },
                    isActive = sub.isActive
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun getSubscriptionTotal(cardLast4: String?): Flow<Double> =
        subscriptionDao.getActiveTotal(cardLast4)

    override suspend fun markSubscriptionInactive(id: Long) =
        subscriptionDao.markInactive(id)

    override fun getSubscriptionsFromCategory(cardLast4: String?): Flow<List<SubscriptionSummary>> =
        combine(
            transactionDao.getSubscriptionMerchants(cardLast4),
            subscriptionDao.getActiveSubscriptions(null),
            cardDao.getAllCards()
        ) { merchants, subs, cards ->
            val cardMap = cards.associate { it.last4 to it.nickname }
            val subMap = subs.associateBy { "${it.merchantName}|${it.cardLast4}" }
            merchants.map { m ->
                val sub = subMap["${m.merchantName}|${m.cardLast4}"]
                val freq = sub?.frequency ?: "MONTHLY"
                val freqMs = if (freq == "YEARLY") 365L * 24 * 60 * 60 * 1000
                             else 30L * 24 * 60 * 60 * 1000
                SubscriptionSummary(
                    merchantName = m.merchantName,
                    amount = m.latestAmount,
                    currency = m.currency,
                    lastCharged = m.lastCharged,
                    cardLast4 = m.cardLast4,
                    cardNickname = m.cardLast4?.let { cardMap[it] },
                    previousAmount = sub?.previousAmount,
                    frequency = freq,
                    nextChargeEstimate = m.lastCharged + freqMs
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun getSubscriptionTotalFromCategory(cardLast4: String?): Flow<Double> =
        transactionDao.getSubscriptionTotalByCategory(cardLast4)

    override fun getActiveAlerts(): Flow<List<AlertInfo>> =
        alertDao.getActiveAlerts().map { alerts ->
            alerts.map { a ->
                AlertInfo(
                    id = a.id, type = a.type, message = a.message,
                    timestamp = a.timestamp, isDismissed = a.isDismissed,
                    transactionId = a.transactionId
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun getAlertCount(): Flow<Int> = alertDao.getActiveCount()

    override suspend fun dismissAlert(id: Long) = alertDao.dismiss(id)

    override fun getAllCards(): Flow<List<CardInfo>> =
        cardDao.getAllCards().map { cards ->
            cards.map { c ->
                CardInfo(
                    last4 = c.last4,
                    nickname = c.nickname,
                    issuer = c.issuer,
                    transactionCount = c.transactionCount,
                    totalSpent = 0.0, // Computed lazily in VM
                    isHidden = c.isHidden
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun getCardCount(): Flow<Int> = cardDao.getCardCount()

    override suspend fun updateCardNickname(last4: String, nickname: String) =
        cardDao.updateNickname(last4, nickname)

    override suspend fun setCardHidden(last4: String, isHidden: Boolean) =
        cardDao.setHidden(last4, isHidden)

    override suspend fun addCard(last4: String, nickname: String?) {
        val now = System.currentTimeMillis()
        cardDao.insert(
            com.novachat.core.database.financial.entity.CardEntity(
                last4 = last4,
                nickname = nickname,
                createdAt = now,
                lastSeenTimestamp = now
            )
        )
    }

    override suspend fun deleteCard(last4: String) = cardDao.delete(last4)

    override suspend fun reorderCards(orderedLast4s: List<String>) {
        orderedLast4s.forEachIndexed { index, last4 ->
            cardDao.updateSortOrder(last4, index)
        }
    }

    override fun getAllSenders(): Flow<List<SenderInfo>> =
        senderDao.getAllSenders().map { senders ->
            senders.map { s ->
                SenderInfo(
                    id = s.id, address = s.address, displayName = s.displayName,
                    isEnabled = s.isEnabled, alertsEnabled = s.alertsEnabled,
                    source = s.source, transactionCount = s.transactionCount
                )
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun setSenderEnabled(id: Long, enabled: Boolean) =
        senderDao.setEnabled(id, enabled)

    override suspend fun setSenderAlertsEnabled(id: Long, enabled: Boolean) =
        senderDao.setAlertsEnabled(id, enabled)

    override suspend fun addSender(address: String, displayName: String?) {
        senderDao.insert(
            FinancialSenderEntity(
                address = address,
                displayName = displayName,
                source = "MANUAL",
                createdAt = System.currentTimeMillis(),
                lastSeenTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeSender(id: Long) {
        val sender = senderDao.getById(id) ?: return
        transactionDao.deleteBySender(sender.address)
        senderDao.delete(id)
    }

    override suspend fun clearAllFinancialData() {
        transactionDao.deleteAll()
        subscriptionDao.deleteAll()
        merchantDao.deleteAll()
        cardDao.deleteAll()
        senderDao.deleteAll()
        alertDao.deleteAll()
    }

    override fun getCategoryCounts(): Flow<Map<String, Int>> =
        combine(
            transactionDao.getCategoryCount(FinancialCategory.BILL.name),
            transactionDao.getCategoryCount(FinancialCategory.SUBSCRIPTION.name),
            transactionDao.getCategoryCount(FinancialCategory.PAYMENT.name),
            transactionDao.getCategoryCount(FinancialCategory.EXPENSE.name)
        ) { bills, subs, payments, expenses ->
            mapOf(
                FinancialCategory.BILL.name to bills,
                FinancialCategory.SUBSCRIPTION.name to subs,
                FinancialCategory.PAYMENT.name to payments,
                FinancialCategory.EXPENSE.name to expenses
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun updateMerchantCategory(merchantName: String, category: String) {
        merchantDao.updateCategory(merchantName, category)
        transactionDao.updateCategoryByMerchant(merchantName, category)
    }
}
