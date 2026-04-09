package com.novachat.core.sms.financial

import com.novachat.core.analytics.financial.AnomalyDetector
import com.novachat.core.database.financial.dao.CardDao
import com.novachat.core.database.financial.dao.FinancialSenderDao
import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.MerchantDao
import com.novachat.core.database.financial.entity.CardEntity
import com.novachat.core.database.financial.entity.FinancialSenderEntity
import com.novachat.core.database.financial.entity.FinancialTransactionEntity
import com.novachat.core.database.financial.entity.MerchantEntity
import com.novachat.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialSmsParser @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val regexEngine: RegexParsingEngine,
    private val mlKitExtractor: MlKitFinancialExtractor,
    private val categoryClassifier: CategoryClassifier,
    private val recurrenceDetector: RecurrenceDetector,
    private val transactionDao: FinancialTransactionDao,
    private val merchantDao: MerchantDao,
    private val cardDao: CardDao,
    private val senderDao: FinancialSenderDao,
    private val anomalyDetector: AnomalyDetector
) {
    suspend fun parseIfEnabled(smsId: Long, address: String, body: String, timestamp: Long) {
        val isPremium = userPreferencesRepository.isPremium.first()
        val isEnabled = userPreferencesRepository.financialIntelligenceEnabled.first()
        if (!isPremium || !isEnabled) return
        parse(smsId, address, body, timestamp)
    }

    suspend fun parse(smsId: Long, address: String, body: String, timestamp: Long) {
        // Step 2: Sender disable check
        val existingSender = senderDao.getByAddress(address)
        if (existingSender != null && !existingSender.isEnabled) return

        // Step 3: Body pre-filter
        if (!regexEngine.isFinancialSms(body)) return

        // Step 4: Sender auto-registration
        if (existingSender == null) {
            senderDao.insert(
                FinancialSenderEntity(
                    address = address,
                    source = "AUTO",
                    createdAt = timestamp,
                    lastSeenTimestamp = timestamp
                )
            )
        } else {
            senderDao.update(existingSender.copy(
                lastSeenTimestamp = timestamp,
                transactionCount = existingSender.transactionCount + 1
            ))
        }

        // Step 5: Extract data
        val regexData = regexEngine.parse(body, address)
        val amount = regexData.amount ?: run {
            val mlResult = mlKitExtractor.extract(body)
            mlResult.amount
        } ?: return

        val currency = regexData.currency
        val merchantName = regexData.merchantName
        val cardLast4 = regexData.cardLast4

        // Step 6: Card disable check
        if (cardLast4 != null) {
            val card = cardDao.getByLast4(cardLast4)
            if (card != null && card.isHidden) return
            if (card == null) {
                val issuer = regexEngine.detectIssuer(address)
                cardDao.insert(
                    CardEntity(
                        last4 = cardLast4,
                        issuer = issuer,
                        createdAt = timestamp,
                        lastSeenTimestamp = timestamp
                    )
                )
            } else {
                cardDao.update(card.copy(
                    lastSeenTimestamp = timestamp,
                    transactionCount = card.transactionCount + 1
                ))
            }
        }

        // Classify category
        val isRecurring = false // Will be updated by recurrence detector
        val category = categoryClassifier.classify(
            isRecurring = isRecurring,
            hasDueDate = regexData.hasDueDate,
            hasPaymentKeyword = regexData.hasPaymentKeyword,
            body = body
        )

        val confidenceScore = when {
            regexData.amount != null && merchantName != null -> 0.9f
            regexData.amount != null -> 0.7f
            else -> 0.4f
        }

        // Step 7: Persist transaction
        val entity = FinancialTransactionEntity(
            smsId = smsId,
            sender = address,
            merchantName = merchantName,
            amount = amount,
            currency = currency,
            category = category.name,
            timestamp = timestamp,
            smsTimestamp = timestamp,
            isRecurring = false,
            confidenceScore = confidenceScore,
            rawBody = body,
            cardLast4 = cardLast4
        )

        val insertedId = transactionDao.insert(entity)
        if (insertedId == -1L) return // Duplicate smsId

        // Update merchant stats
        if (merchantName != null) {
            val merchant = merchantDao.getByName(merchantName)
            if (merchant != null) {
                val newCount = merchant.transactionCount + 1
                val newAvg = ((merchant.averageAmount * merchant.transactionCount) + amount) / newCount
                merchantDao.update(merchant.copy(
                    averageAmount = newAvg,
                    transactionCount = newCount,
                    lastSeenTimestamp = timestamp,
                    category = merchant.category ?: category.name
                ))
            } else {
                merchantDao.insert(
                    MerchantEntity(
                        name = merchantName,
                        averageAmount = amount,
                        transactionCount = 1,
                        firstSeenTimestamp = timestamp,
                        lastSeenTimestamp = timestamp,
                        category = category.name
                    )
                )
            }

            // Run recurrence detection
            recurrenceDetector.detectAndUpdate(
                merchantName = merchantName,
                amount = amount,
                currency = currency,
                timestamp = timestamp,
                cardLast4 = cardLast4,
                transactionId = insertedId
            )
        }

        // Foreign conversion check
        if (merchantName != null) {
            checkForeignConversion(insertedId, merchantName, amount, currency, timestamp)
        }

        // Run anomaly detection (if alerts enabled for this sender)
        val sender = senderDao.getByAddress(address)
        if (sender?.alertsEnabled != false) {
            anomalyDetector.evaluate(entity.copy(id = insertedId))
        }
    }

    private suspend fun checkForeignConversion(
        transactionId: Long,
        merchantName: String,
        amount: Double,
        currency: String,
        timestamp: Long
    ) {
        if (currency == "ILS") {
            val recentForeign = transactionDao.getTransactionsByMerchant(merchantName, timestamp - 48 * 60 * 60 * 1000)
            val match = recentForeign.find { it.id != transactionId && it.currency != "ILS" }
            if (match != null) {
                val tx = transactionDao.getById(transactionId) ?: return
                transactionDao.update(tx.copy(isConversion = true))
            }
        }
    }
}
