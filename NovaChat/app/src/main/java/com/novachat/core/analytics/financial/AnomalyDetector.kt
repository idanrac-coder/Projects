package com.novachat.core.analytics.financial

import android.content.Context
import com.novachat.core.database.financial.dao.FinancialAlertDao
import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.MerchantDao
import com.novachat.core.database.financial.dao.SubscriptionDao
import com.novachat.core.database.financial.entity.FinancialAlertEntity
import com.novachat.core.database.financial.entity.FinancialTransactionEntity
import com.novachat.core.notification.FinancialNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnomalyDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: FinancialTransactionDao,
    private val merchantDao: MerchantDao,
    private val subscriptionDao: SubscriptionDao,
    private val alertDao: FinancialAlertDao
) {
    suspend fun evaluate(transaction: FinancialTransactionEntity) {
        checkUnusualAmount(transaction)
        checkNewMerchantHigh(transaction)
        checkDuplicateCharge(transaction)
    }

    suspend fun checkSubscriptionPriceIncrease(
        merchantName: String,
        newAmount: Double,
        previousAmount: Double,
        currency: String
    ) {
        if (previousAmount <= 0 || newAmount <= previousAmount) return
        val pct = ((newAmount - previousAmount) / previousAmount * 100).toInt()
        val message = "$merchantName increased from $currency${"%.2f".format(previousAmount)} to $currency${"%.2f".format(newAmount)} (+$pct%)"
        val alert = FinancialAlertEntity(
            type = "SUBSCRIPTION_PRICE_INCREASE",
            transactionId = 0,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        val id = alertDao.insert(alert)
        FinancialNotificationHelper.showAlert(context, alert.copy(id = id))
    }

    private suspend fun checkUnusualAmount(tx: FinancialTransactionEntity) {
        val merchant = tx.merchantName?.let { merchantDao.getByName(it) } ?: return
        if (merchant.transactionCount < 3) return
        if (tx.amount > merchant.averageAmount * 2.0) {
            val msg = "₪${"%.2f".format(tx.amount)} at ${tx.merchantName} is ${"%.1f".format(tx.amount / merchant.averageAmount)}x higher than your average spend (₪${"%.2f".format(merchant.averageAmount)})"
            insertAlert("UNUSUAL_AMOUNT", tx.id, msg)
        }
    }

    private suspend fun checkNewMerchantHigh(tx: FinancialTransactionEntity) {
        val merchant = tx.merchantName?.let { merchantDao.getByName(it) } ?: return
        if (merchant.transactionCount != 1) return
        val globalAvg = transactionDao.getGlobalAverageAmount()
        if (globalAvg > 0 && tx.amount > globalAvg * 1.5) {
            val msg = "First transaction at ${tx.merchantName} for ₪${"%.2f".format(tx.amount)} - this is above your typical spending"
            insertAlert("NEW_MERCHANT_HIGH", tx.id, msg)
        }
    }

    private suspend fun checkDuplicateCharge(tx: FinancialTransactionEntity) {
        if (tx.merchantName == null) return
        val window24h = 24 * 60 * 60 * 1000L
        val dup = transactionDao.findDuplicate(
            tx.merchantName, tx.amount,
            tx.timestamp - window24h, tx.timestamp + window24h, tx.id
        )
        if (dup != null) {
            val msg = "₪${"%.2f".format(tx.amount)} charged twice at ${tx.merchantName} within 24 hours"
            insertAlert("DUPLICATE_CHARGE", tx.id, msg)
        }
    }

    private suspend fun insertAlert(type: String, transactionId: Long, message: String) {
        val alert = FinancialAlertEntity(
            type = type,
            transactionId = transactionId,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        val id = alertDao.insert(alert)
        FinancialNotificationHelper.showAlert(context, alert.copy(id = id))
    }
}
