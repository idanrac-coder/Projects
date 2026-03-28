package com.novachat.core.sms.ml

/**
 * Fuses scores from all spam detection layers into a single 0-100 score.
 *
 * Signal weights are adjusted dynamically: when TFLite model is available,
 * it gets higher weight; otherwise heuristic/deterministic layers dominate.
 */
object SpamScoreFusionEngine {

    data class FusionInput(
        val deterministicScore: Int,
        val heuristicScore: Int,
        val semanticAdjustment: Int,
        val mlProbability: Float,
        val personalScore: Float,
        val isModelAvailable: Boolean,
        val isPersonalModelReady: Boolean
    )

    data class FusionResult(
        val finalScore: Int,
        val mlContribution: Int,
        val personalContribution: Int,
        val breakdown: Map<String, Int>
    )

    fun fuse(input: FusionInput): FusionResult {
        val breakdown = mutableMapOf<String, Int>()

        val baseScore = input.heuristicScore + input.semanticAdjustment
        breakdown["heuristic"] = input.heuristicScore
        if (input.semanticAdjustment != 0) breakdown["semantic"] = input.semanticAdjustment

        var mlContribution = 0
        if (input.isModelAvailable && input.mlProbability >= 0f) {
            val mlScore = (input.mlProbability * 100).toInt()
            mlContribution = when {
                mlScore > 80 -> 25
                mlScore > 60 -> 15
                mlScore > 40 -> 5
                mlScore < 20 -> -10
                else -> 0
            }
            breakdown["ml_model"] = mlContribution
        }

        var personalContribution = 0
        if (input.isPersonalModelReady && input.personalScore >= 0f) {
            val personalInt = (input.personalScore * 100).toInt()
            personalContribution = when {
                personalInt > 80 -> 15
                personalInt > 60 -> 8
                personalInt < 20 -> -8
                else -> 0
            }
            breakdown["personal"] = personalContribution
        }

        val finalScore = (baseScore + mlContribution + personalContribution).coerceIn(0, 100)

        return FusionResult(
            finalScore = finalScore,
            mlContribution = mlContribution,
            personalContribution = personalContribution,
            breakdown = breakdown
        )
    }
}
