package com.novachat.core.sms.financial

import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.SubscriptionDao
import com.novachat.core.database.financial.entity.SubscriptionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurrenceDetector @Inject constructor(
    private val transactionDao: FinancialTransactionDao,
    private val subscriptionDao: SubscriptionDao
) {
    companion object {
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
        private const val MIN_WINDOW_MS = 25L * 24 * 60 * 60 * 1000
        private const val MAX_WINDOW_MS = 35L * 24 * 60 * 60 * 1000
    }

    suspend fun detectAndUpdate(
        merchantName: String,
        amount: Double,
        currency: String,
        timestamp: Long,
        cardLast4: String?,
        transactionId: Long
    ): Boolean {
        val since = timestamp - (MAX_WINDOW_MS * 3)
        val history = transactionDao.getTransactionsByMerchant(merchantName, since)

        if (history.size < 2) return false

        val sorted = history.sortedByDescending { it.timestamp }
        val latest = sorted[0]
        val previous = sorted[1]
        val gap = latest.timestamp - previous.timestamp

        if (gap < MIN_WINDOW_MS || gap > MAX_WINDOW_MS) return false

        transactionDao.markRecurring(transactionId)

        val existing = subscriptionDao.getByMerchantAndCard(merchantName, cardLast4)
        if (existing != null) {
            val previousAmount = if (existing.amount != amount) existing.amount else existing.previousAmount
            subscriptionDao.update(
                existing.copy(
                    amount = amount,
                    previousAmount = previousAmount,
                    currency = currency,
                    lastSeenTimestamp = timestamp,
                    transactionCount = existing.transactionCount + 1,
                    cardLast4 = cardLast4 ?: existing.cardLast4
                )
            )
        } else {
            subscriptionDao.insert(
                SubscriptionEntity(
                    merchantName = merchantName,
                    amount = amount,
                    currency = currency,
                    frequency = "MONTHLY",
                    lastSeenTimestamp = timestamp,
                    firstSeenTimestamp = previous.timestamp,
                    transactionCount = history.size,
                    cardLast4 = cardLast4
                )
            )
        }
        return true
    }

    suspend fun detectAll() {
        // Re-scan all transactions for recurring patterns — run by worker periodically
        // This is a simplified version; full implementation would query distinct merchants
        // and run detectAndUpdate for each
    }
}
