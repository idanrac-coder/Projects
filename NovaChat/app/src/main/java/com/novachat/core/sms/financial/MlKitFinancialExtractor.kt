package com.novachat.core.sms.financial

import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.nl.entityextraction.MoneyEntity
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class MlKitFinancialResult(
    val amount: Double? = null,
    val currency: String? = null,
    val dateTimeMs: Long? = null
)

@Singleton
class MlKitFinancialExtractor @Inject constructor() {

    private val extractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    suspend fun extract(text: String): MlKitFinancialResult {
        return try {
            val params = EntityExtractionParams.Builder(text).build()
            val annotations = extractor.annotate(params).await()

            var amount: Double? = null
            var currency: String? = null
            var dateTimeMs: Long? = null

            for (annotation in annotations) {
                for (entity in annotation.entities) {
                    when (entity.type) {
                        Entity.TYPE_MONEY -> {
                            val money = entity as MoneyEntity
                            if (amount == null) {
                                val intPart = money.integerPart
                                val fracPart = money.fractionalPart
                                amount = intPart.toDouble() + fracPart.toDouble() / 100.0
                                currency = money.unnormalizedCurrency
                            }
                        }
                        Entity.TYPE_DATE_TIME -> {
                            val dt = entity as DateTimeEntity
                            if (dateTimeMs == null) {
                                dateTimeMs = dt.timestampMillis
                            }
                        }
                    }
                }
            }

            MlKitFinancialResult(amount, currency, dateTimeMs)
        } catch (_: Exception) {
            MlKitFinancialResult()
        }
    }
}
