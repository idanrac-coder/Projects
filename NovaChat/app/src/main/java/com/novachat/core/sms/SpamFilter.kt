package com.novachat.core.sms

import com.novachat.core.sms.hebrew.HebrewSpamEngine
import com.novachat.core.sms.hebrew.SpamCategory
import com.novachat.core.sms.ml.PersonalSpamAdapter
import com.novachat.core.sms.ml.SpamMlClassifier
import com.novachat.core.sms.ml.SpamScoreFusionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zero-Trust spam filter orchestrator. Runs four-layer pipeline:
 * Layer 1: Deterministic regex
 * Layer 2: Weighted heuristic scoring
 * Layer 3: Semantic (ML Kit + short-code whitelist)
 * Layer 4: TFLite ML classifier (when model available)
 * Score fusion engine combines all signals. Personal adaptive model blended when ready.
 * When body contains Hebrew, runs HebrewSpamEngine first; maps result to SpamClassification.
 */
@Singleton
class SpamFilter @Inject constructor(
    private val scamDetector: ScamDetector,
    private val semanticSpamLayer: SemanticSpamLayer,
    private val hebrewSpamEngine: HebrewSpamEngine,
    private val mlClassifier: SpamMlClassifier,
    private val personalAdapter: PersonalSpamAdapter
) {

    data class ClassificationResult(
        val classification: SpamClassification,
        val score: Int,
        val matchedRuleType: String?,
        val isLowConfidence: Boolean = false
    )

    enum class SpamClassification {
        SPAM,      // score > 75 → Shadow Inbox, no notification
        SUSPICIOUS,// score 40-74 → show notification with warning
        SAFE
    }

    /**
     * Classifies an incoming message. Run from a coroutine (suspend).
     * Contact and allowlisted senders bypass the pipeline and return SAFE.
     */
    suspend fun classify(
        address: String,
        body: String,
        isKnownContact: Boolean
    ): ClassificationResult = withContext(Dispatchers.Default) {
        if (body.length < 10) {
            return@withContext ClassificationResult(
                SpamClassification.SAFE,
                0,
                null
            )
        }
        if (isKnownContact || scamDetector.isAllowlisted(address)) {
            return@withContext ClassificationResult(
                SpamClassification.SAFE,
                0,
                null
            )
        }

        val scamAnalysis = scamDetector.analyzeWithReputation(body, address, isKnownContact)
        if (scamAnalysis.isScam) {
            val scoreInt = (scamAnalysis.confidence * 100).toInt().coerceIn(0, 100)
            val classification = if (scamAnalysis.confidence >= 0.72f) {
                SpamClassification.SPAM
            } else {
                SpamClassification.SUSPICIOUS
            }
            return@withContext ClassificationResult(
                classification = classification,
                score = scoreInt,
                matchedRuleType = "SCAM:${scamAnalysis.category?.name ?: "UNKNOWN"}",
                isLowConfidence = classification == SpamClassification.SUSPICIOUS
            )
        }

        val shortCircuitRules = setOf("TAX_REFUND_SCAM", "ISRAELI_PANIC", "SURVEY_UNSUBSCRIBE", "POLITICAL_POLL")

        val hasHebrew = body.any { it in '\u0590'..'\u05FF' }
        if (hasHebrew) {
            val normalizedBody = com.novachat.core.sms.hebrew.HebrewTextNormalizer.normalizeForMatching(body)
            val detRaw = DeterministicSpamLayer.analyze(normalizedBody)
            val shortCircuitMatch = detRaw.matchedRuleTypes.firstOrNull { it in shortCircuitRules }
            if (detRaw.matched && shortCircuitMatch != null) {
                return@withContext ClassificationResult(
                    SpamClassification.SPAM,
                    (detRaw.contributesToScore + 50).coerceIn(0, 100),
                    "DET_RAW:$shortCircuitMatch",
                    false
                )
            }
            val hebrewResult = withTimeoutOrNull(50L) {
                hebrewSpamEngine.analyze(address, body, isKnownContact, null)
            }
            if (hebrewResult != null) {
                val classification = when (hebrewResult.category) {
                    SpamCategory.DEFINITE_SPAM -> SpamClassification.SPAM
                    SpamCategory.SUSPECTED_SPAM -> SpamClassification.SUSPICIOUS
                    SpamCategory.SAFE -> SpamClassification.SAFE
                }
                val score = (hebrewResult.score * 100).toInt().coerceIn(0, 100)
                val ruleType = hebrewResult.reasons.firstOrNull() ?: "HEBREW:${hebrewResult.score}"
                return@withContext ClassificationResult(
                    classification = classification,
                    score = score,
                    matchedRuleType = "HEBREW:$ruleType",
                    isLowConfidence = classification == SpamClassification.SUSPICIOUS
                )
            }
        }

        val deterministicResult = DeterministicSpamLayer.analyze(body)
        val deterministicBonus = if (deterministicResult.matched) deterministicResult.contributesToScore else 0

        val heuristicResult = HeuristicSpamLayer.analyze(
            body = body,
            isKnownContact = isKnownContact,
            deterministicBonus = deterministicBonus
        )

        val semanticResult = semanticSpamLayer.analyze(body, address, heuristicResult.score)

        val mlProb = withTimeoutOrNull(100L) { mlClassifier.classify(body) } ?: -1f
        val personalScore = if (personalAdapter.isReady) personalAdapter.score(body) else -1f

        val fusionInput = SpamScoreFusionEngine.FusionInput(
            deterministicScore = deterministicBonus,
            heuristicScore = heuristicResult.score,
            semanticAdjustment = semanticResult.scoreAdjustment,
            mlProbability = mlProb,
            personalScore = personalScore,
            isModelAvailable = mlClassifier.isModelAvailable,
            isPersonalModelReady = personalAdapter.isReady
        )
        val fusionResult = SpamScoreFusionEngine.fuse(fusionInput)
        val finalScore = fusionResult.finalScore

        val classification = when {
            finalScore > 75 -> SpamClassification.SPAM
            finalScore >= 55 -> SpamClassification.SUSPICIOUS
            else -> SpamClassification.SAFE
        }

        val matchedRuleType = deterministicResult.ruleType?.let { "DET:$it" }
            ?: heuristicResult.breakdown.entries.joinToString("+") { "${it.key}=${it.value}" }
                .takeIf { it.isNotBlank() }
                ?.let { "HEUR:$it" }

        ClassificationResult(
            classification = classification,
            score = finalScore,
            matchedRuleType = matchedRuleType,
            isLowConfidence = classification == SpamClassification.SUSPICIOUS
        )
    }

    suspend fun reportSpam(address: String, body: String, category: ScamCategory?) {
        scamDetector.reportSpam(address, body, category)
    }

    suspend fun reportNotSpam(address: String, body: String) {
        scamDetector.reportNotSpam(address, body)
    }

    suspend fun isAllowlisted(address: String): Boolean = scamDetector.isAllowlisted(address)
}
