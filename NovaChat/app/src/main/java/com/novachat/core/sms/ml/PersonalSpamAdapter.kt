package com.novachat.core.sms.ml

import android.util.Log
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.entity.SpamKeywordWeightEntity
import com.novachat.core.database.entity.SpamLearningEntity
import com.novachat.core.database.entity.SpamSenderReputationEntity
import com.novachat.core.sms.hebrew.HebrewTextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln

/**
 * On-device personal spam adapter using Naive Bayes over user feedback.
 * Learns from explicit feedback (report spam/not spam) and implicit signals
 * (reply=ham, delete within seconds=likely spam, block=spam, add contact=ham).
 * Keyword weights use exponential decay so old signals fade over time.
 *
 * Minimum 10 training samples required before producing scores.
 */
@Singleton
class PersonalSpamAdapter @Inject constructor(
    private val spamLearningDao: SpamLearningDao
) {
    companion object {
        private const val TAG = "PersonalSpamAdapter"
        private const val MIN_TRAINING_SAMPLES = 10
        private const val DECAY_HALF_LIFE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    private val mutex = Mutex()

    @Volatile
    var isReady: Boolean = false
        private set

    private var spamPrior = 0.5f
    private var hamPrior = 0.5f
    private var keywordWeights: Map<String, Float> = emptyMap()

    suspend fun refresh() = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val spamCount = spamLearningDao.getSpamTrainingCount()
                val hamCount = spamLearningDao.getHamTrainingCount()
                val total = spamCount + hamCount

                if (total < MIN_TRAINING_SAMPLES) {
                    isReady = false
                    return@withContext
                }

                spamPrior = spamCount.toFloat() / total
                hamPrior = hamCount.toFloat() / total

                val weights = spamLearningDao.getAllKeywordWeights()
                keywordWeights = weights.associate { it.keyword to it.weight }

                isReady = true
                Log.d(TAG, "Personal model refreshed: $spamCount spam, $hamCount ham, ${weights.size} keywords")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh personal model", e)
                isReady = false
            }
        }
    }

    /**
     * Scores a message body using the personal Naive Bayes model.
     * Returns spam probability [0, 1] or -1f if not ready.
     */
    fun score(body: String): Float {
        if (!isReady || keywordWeights.isEmpty()) return -1f

        val tokens = tokenize(body)
        if (tokens.isEmpty()) return -1f

        var logSpam = ln(spamPrior.coerceAtLeast(0.01f).toDouble())
        var logHam = ln(hamPrior.coerceAtLeast(0.01f).toDouble())

        for (token in tokens) {
            val weight = keywordWeights[token] ?: 0.5f
            val pSpam = weight.coerceIn(0.01f, 0.99f)
            val pHam = (1f - pSpam).coerceIn(0.01f, 0.99f)
            logSpam += ln(pSpam.toDouble())
            logHam += ln(pHam.toDouble())
        }

        val maxLog = maxOf(logSpam, logHam)
        val probSpam = exp(logSpam - maxLog)
        val probHam = exp(logHam - maxLog)
        return (probSpam / (probSpam + probHam)).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Records an implicit learning signal.
     */
    suspend fun recordImplicitSignal(
        address: String,
        body: String,
        signal: ImplicitSignal
    ) = withContext(Dispatchers.IO) {
        val isSpam = when (signal) {
            ImplicitSignal.REPLIED -> false
            ImplicitSignal.CONTACT_ADDED -> false
            ImplicitSignal.BLOCKED -> true
            ImplicitSignal.QUICK_DELETE -> true
            ImplicitSignal.REPORTED_SPAM -> true
            ImplicitSignal.REPORTED_NOT_SPAM -> false
        }

        val confidence = when (signal) {
            ImplicitSignal.REPORTED_SPAM, ImplicitSignal.REPORTED_NOT_SPAM -> 0.95f
            ImplicitSignal.BLOCKED -> 0.9f
            ImplicitSignal.CONTACT_ADDED -> 0.85f
            ImplicitSignal.REPLIED -> 0.7f
            ImplicitSignal.QUICK_DELETE -> 0.6f
        }

        spamLearningDao.insertLearningEntry(
            SpamLearningEntity(
                address = address,
                body = body,
                isSpam = isSpam,
                detectedCategory = null,
                userFeedback = signal.name,
                confidence = confidence
            )
        )

        val reputation = spamLearningDao.getSenderReputation(address)
        val updated = if (reputation != null) {
            if (isSpam) reputation.copy(
                spamCount = reputation.spamCount + 1,
                lastSpamTimestamp = System.currentTimeMillis()
            ) else reputation.copy(
                hamCount = reputation.hamCount + 1,
                lastHamTimestamp = System.currentTimeMillis()
            )
        } else {
            SpamSenderReputationEntity(
                address = address,
                spamCount = if (isSpam) 1 else 0,
                hamCount = if (isSpam) 0 else 1,
                lastSpamTimestamp = if (isSpam) System.currentTimeMillis() else 0,
                lastHamTimestamp = if (isSpam) 0 else System.currentTimeMillis()
            )
        }
        spamLearningDao.upsertSenderReputation(updated)

        updateKeywordWeights(body, isSpam)
    }

    private suspend fun updateKeywordWeights(body: String, isSpam: Boolean) {
        val tokens = tokenize(body).distinct()
        for (token in tokens) {
            val existing = spamLearningDao.getKeywordWeight(token)
            val updated = if (existing != null) {
                val newSpam = existing.spamOccurrences + if (isSpam) 1 else 0
                val newHam = existing.hamOccurrences + if (isSpam) 0 else 1
                val weight = newSpam.toFloat() / (newSpam + newHam).coerceAtLeast(1)
                existing.copy(
                    spamOccurrences = newSpam,
                    hamOccurrences = newHam,
                    weight = weight
                )
            } else {
                SpamKeywordWeightEntity(
                    keyword = token,
                    spamOccurrences = if (isSpam) 1 else 0,
                    hamOccurrences = if (isSpam) 0 else 1,
                    weight = if (isSpam) 1f else 0f
                )
            }
            spamLearningDao.upsertKeywordWeight(updated)
        }
    }

    /**
     * Applies exponential decay to keyword weights, reducing the influence of old data.
     * Should be called periodically (e.g., once per day via WorkManager).
     */
    suspend fun applyExponentialDecay() = withContext(Dispatchers.IO) {
        try {
            val allWeights = spamLearningDao.getAllKeywordWeights()
            val now = System.currentTimeMillis()
            for (kw in allWeights) {
                val total = kw.spamOccurrences + kw.hamOccurrences
                if (total <= 1) continue
                val decayFactor = 0.95f
                val decayedSpam = (kw.spamOccurrences * decayFactor).toInt().coerceAtLeast(0)
                val decayedHam = (kw.hamOccurrences * decayFactor).toInt().coerceAtLeast(0)
                if (decayedSpam + decayedHam == 0) continue
                val newWeight = decayedSpam.toFloat() / (decayedSpam + decayedHam).coerceAtLeast(1)
                spamLearningDao.upsertKeywordWeight(
                    kw.copy(spamOccurrences = decayedSpam, hamOccurrences = decayedHam, weight = newWeight)
                )
            }
            Log.d(TAG, "Exponential decay applied to ${allWeights.size} keywords")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply decay", e)
        }
    }

    private fun tokenize(body: String): List<String> {
        val normalized = if (body.any { it in '\u0590'..'\u05FF' }) {
            HebrewTextNormalizer.normalizeForMatching(body)
        } else {
            body
        }
        return normalized.lowercase()
            .split(Regex("[\\s\\p{P}]+"))
            .filter { it.length >= 2 }
    }

    enum class ImplicitSignal {
        REPLIED,
        CONTACT_ADDED,
        BLOCKED,
        QUICK_DELETE,
        REPORTED_SPAM,
        REPORTED_NOT_SPAM
    }
}
