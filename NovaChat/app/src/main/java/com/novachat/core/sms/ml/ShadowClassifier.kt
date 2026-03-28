package com.novachat.core.sms.ml

import android.util.Log
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.entity.SpamLearningEntity
import com.novachat.core.sms.DeterministicSpamLayer
import com.novachat.core.sms.HeuristicSpamLayer
import com.novachat.core.sms.SpamFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shadow A/B testing: runs the heuristic-only pipeline alongside the full ML pipeline
 * and logs disagreements. This never affects the user-facing classification --
 * it only records data for offline analysis.
 *
 * When enabled, every classify() call also runs the old scoring path and compares results.
 * Disagreements are logged with detailed breakdown for model iteration.
 */
@Singleton
class ShadowClassifier @Inject constructor(
    private val mlClassifier: SpamMlClassifier,
    private val personalAdapter: PersonalSpamAdapter,
    private val spamLearningDao: SpamLearningDao
) {
    companion object {
        private const val TAG = "ShadowClassifier"
    }

    @Volatile
    var enabled: Boolean = true

    data class ShadowResult(
        val heuristicOnlyScore: Int,
        val fullPipelineScore: Int,
        val heuristicClassification: SpamFilter.SpamClassification,
        val fullClassification: SpamFilter.SpamClassification,
        val disagrees: Boolean,
        val mlContribution: Int,
        val personalContribution: Int
    )

    /**
     * Run the old heuristic-only path for comparison. Call this alongside the real classify().
     * Returns null if shadow testing is disabled.
     */
    suspend fun shadowClassify(
        body: String,
        isKnownContact: Boolean,
        actualResult: SpamFilter.ClassificationResult
    ): ShadowResult? {
        if (!enabled) return null

        return withContext(Dispatchers.Default) {
            try {
                val detResult = DeterministicSpamLayer.analyze(body)
                val detBonus = if (detResult.matched) detResult.contributesToScore else 0
                val heuristicResult = HeuristicSpamLayer.analyze(
                    body = body,
                    isKnownContact = isKnownContact,
                    deterministicBonus = detBonus
                )
                val heuristicScore = heuristicResult.score

                val mlProb = if (mlClassifier.isModelAvailable) mlClassifier.classify(body) else -1f
                val personalScore = if (personalAdapter.isReady) personalAdapter.score(body) else -1f

                val fusionInput = SpamScoreFusionEngine.FusionInput(
                    deterministicScore = detBonus,
                    heuristicScore = heuristicScore,
                    semanticAdjustment = 0,
                    mlProbability = mlProb,
                    personalScore = personalScore,
                    isModelAvailable = mlClassifier.isModelAvailable,
                    isPersonalModelReady = personalAdapter.isReady
                )
                val fusionResult = SpamScoreFusionEngine.fuse(fusionInput)

                val heuristicClass = classify(heuristicScore)
                val fullClass = classify(fusionResult.finalScore)

                val disagrees = heuristicClass != fullClass

                if (disagrees) {
                    Log.d(
                        TAG,
                        "SHADOW DISAGREE: heuristic=$heuristicClass($heuristicScore) vs full=$fullClass(${fusionResult.finalScore}) " +
                        "ml=${fusionResult.mlContribution} personal=${fusionResult.personalContribution}"
                    )
                    spamLearningDao.insertLearningEntry(
                        SpamLearningEntity(
                            address = "SHADOW_TEST",
                            body = "H:$heuristicScore F:${fusionResult.finalScore} ML:${fusionResult.mlContribution} P:${fusionResult.personalContribution}",
                            isSpam = actualResult.classification == SpamFilter.SpamClassification.SPAM,
                            detectedCategory = "SHADOW_DISAGREE",
                            userFeedback = "shadow_ab",
                            confidence = 0f
                        )
                    )
                }

                ShadowResult(
                    heuristicOnlyScore = heuristicScore,
                    fullPipelineScore = fusionResult.finalScore,
                    heuristicClassification = heuristicClass,
                    fullClassification = fullClass,
                    disagrees = disagrees,
                    mlContribution = fusionResult.mlContribution,
                    personalContribution = fusionResult.personalContribution
                )
            } catch (e: Exception) {
                Log.w(TAG, "Shadow classify failed", e)
                null
            }
        }
    }

    private fun classify(score: Int): SpamFilter.SpamClassification = when {
        score > 75 -> SpamFilter.SpamClassification.SPAM
        score >= 55 -> SpamFilter.SpamClassification.SUSPICIOUS
        else -> SpamFilter.SpamClassification.SAFE
    }
}
