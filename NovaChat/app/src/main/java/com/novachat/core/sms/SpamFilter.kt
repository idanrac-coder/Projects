package com.novachat.core.sms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zero-Trust spam filter orchestrator. Runs three-layer pipeline:
 * Layer 1: Deterministic regex
 * Layer 2: Weighted heuristic scoring
 * Layer 3: Semantic (ML Kit + short-code whitelist)
 * Integrates with ScamDetector for user feedback (reportSpam / reportNotSpam).
 */
@Singleton
class SpamFilter @Inject constructor(
    private val scamDetector: ScamDetector,
    private val semanticSpamLayer: SemanticSpamLayer
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

        val deterministicResult = DeterministicSpamLayer.analyze(body)
        val deterministicBonus = if (deterministicResult.matched) deterministicResult.contributesToScore else 0

        val heuristicResult = HeuristicSpamLayer.analyze(
            body = body,
            isKnownContact = isKnownContact,
            deterministicBonus = deterministicBonus
        )

        var finalScore = heuristicResult.score
        val semanticResult = semanticSpamLayer.analyze(body, address, finalScore)
        finalScore = (finalScore + semanticResult.scoreAdjustment).coerceAtLeast(0)

        val classification = when {
            finalScore > 75 -> SpamClassification.SPAM
            finalScore >= 40 -> SpamClassification.SUSPICIOUS
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
