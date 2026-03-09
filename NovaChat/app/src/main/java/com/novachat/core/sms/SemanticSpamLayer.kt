package com.novachat.core.sms

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.novachat.core.database.dao.ShortCodeWhitelistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SemanticSpamLayer"
private const val BOOST_SEMANTIC_SUSPICION = 20
private const val DOWNGRADE_WHITELISTED = -30

/**
 * Layer 3: Semantic analysis via ML Kit Entity Extraction + short-code whitelist.
 * For supported languages: detects Financial (MONEY, PAYMENT_CARD, IBAN) and
 * Package delivery (TRACKING_NUMBER, ADDRESS+DATE_TIME).
 * For Hebrew: uses keyword-based fallback (ML Kit Entity Extraction does not support Hebrew).
 * Whitelisted short-codes downgrade; unknown senders with Financial/Package boost.
 */
@Singleton
class SemanticSpamLayer @Inject constructor(
    private val shortCodeWhitelistDao: ShortCodeWhitelistDao
) {
    private var entityExtractor: com.google.mlkit.nl.entityextraction.EntityExtractor? = null
    private var extractorInitialized = false

    data class Result(
        val scoreAdjustment: Int,
        val isFinancial: Boolean,
        val isPackageDelivery: Boolean,
        val isWhitelisted: Boolean
    )

    suspend fun analyze(
        body: String,
        address: String,
        currentScore: Int
    ): Result = withContext(Dispatchers.IO) {
        val isWhitelisted = shortCodeWhitelistDao.isWhitelisted(address)
        val (isFinancial, isPackageDelivery) = detectFinancialOrPackage(body)
        val adjustment = when {
            isFinancial || isPackageDelivery -> {
                if (isWhitelisted) DOWNGRADE_WHITELISTED
                else BOOST_SEMANTIC_SUSPICION
            }
            else -> 0
        }
        Result(
            scoreAdjustment = adjustment,
            isFinancial = isFinancial,
            isPackageDelivery = isPackageDelivery,
            isWhitelisted = isWhitelisted
        )
    }

    private suspend fun detectFinancialOrPackage(body: String): Pair<Boolean, Boolean> {
        if (body.length < 10) return false to false
        val hasHebrew = body.any { it in '\u0590'..'\u05FF' }
        return if (hasHebrew) {
            detectFinancialOrPackageHebrew(body)
        } else {
            detectFinancialOrPackageMlKit(body)
        }
    }

    private fun detectFinancialOrPackageHebrew(body: String): Pair<Boolean, Boolean> {
        val financialKeywords = listOf(
            "החזר מס", "חשבון", "העברה", "תשלום", "מס", "שקלים", "בנק", "כרטיס",
            "פנסיה", "פיצויים"
        )
        val packageKeywords = listOf(
            "משלוח", "חבילה", "מעקב", "איסוף", "דואר", "ממתין"
        )
        val isFinancial = financialKeywords.any { containsAsWholeWord(body, it) }
        val isPackageDelivery = packageKeywords.any { containsAsWholeWord(body, it) }
        return isFinancial to isPackageDelivery
    }

    /**
     * Matches keyword as a whole word (or phrase), not as a substring.
     * E.g. "מס" matches "מס" but not "מסיבה" (party).
     */
    private fun containsAsWholeWord(text: String, keyword: String): Boolean {
        if (keyword.isBlank()) return false
        val escaped = Regex.escape(keyword)
        val pattern = "(?:^|[^\\p{L}\\p{N}])$escaped(?=[^\\p{L}\\p{N}]|$)"
        return Regex(pattern).containsMatchIn(text)
    }

    private suspend fun detectFinancialOrPackageMlKit(body: String): Pair<Boolean, Boolean> {
        return try {
            val extractor = getOrCreateExtractor() ?: return false to false
            val params = EntityExtractionParams.Builder(body).build()
            val task: Task<List<com.google.mlkit.nl.entityextraction.EntityAnnotation>> =
                extractor.annotate(params)
            val annotations = task.await()
            var hasFinancial = false
            var hasTracking = false
            var hasAddress = false
            var hasDateTime = false
            for (ann in annotations) {
                for (entity in ann.entities) {
                    when (entity.type) {
                        Entity.TYPE_MONEY, Entity.TYPE_PAYMENT_CARD, Entity.TYPE_IBAN -> hasFinancial = true
                        Entity.TYPE_TRACKING_NUMBER -> hasTracking = true
                        Entity.TYPE_ADDRESS -> hasAddress = true
                        Entity.TYPE_DATE_TIME -> hasDateTime = true
                    }
                }
            }
            val isFinancial = hasFinancial
            val isPackageDelivery = hasTracking || (hasAddress && hasDateTime)
            isFinancial to isPackageDelivery
        } catch (e: Exception) {
            Log.w(TAG, "ML Kit entity extraction failed, falling back to safe", e)
            false to false
        }
    }

    private suspend fun getOrCreateExtractor(): com.google.mlkit.nl.entityextraction.EntityExtractor? {
        if (extractorInitialized) return entityExtractor
        return withContext(Dispatchers.IO) {
            try {
                val options = EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
                val ext = EntityExtraction.getClient(options)
                ext.downloadModelIfNeeded().await()
                entityExtractor = ext
                extractorInitialized = true
                ext
            } catch (e: Exception) {
                Log.w(TAG, "Failed to init ML Kit Entity Extractor", e)
                null
            }
        }
    }

}
