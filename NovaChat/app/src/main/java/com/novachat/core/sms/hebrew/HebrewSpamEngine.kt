package com.novachat.core.sms.hebrew

import com.novachat.core.sms.AllowlistChecker
import com.novachat.core.sms.DeterministicSpamLayer

/**
 * Orchestrates Hebrew/Israeli spam detection: normalize -> rules -> link -> phone -> ML -> campaign -> fuse.
 * All regex/keyword runs on normalized body. Target <10ms; input capped at 2000 chars.
 */
class HebrewSpamEngine(
    private val mlLayer: HebrewSpamMlLayer,
    private val campaignDetector: CampaignDetector,
    private val allowlistChecker: AllowlistChecker
) {

    suspend fun analyze(
        address: String,
        body: String,
        isKnownContact: Boolean,
        userCountryIso: String?
    ): SpamResult {
        if (body.length < 10) return SpamResult(0f, SpamCategory.SAFE, emptyList())
        if (isKnownContact || this.allowlistChecker.isAllowlisted(address)) return SpamResult(0f, SpamCategory.SAFE, emptyList())
        val normalized = HebrewTextNormalizer.normalizeForMatching(body)
        val hasUrl = LinkRiskAnalyzer.extractUrls(body).isNotEmpty()

        val detResult = DeterministicSpamLayer.analyze(normalized)
        val detScore = if (detResult.matched) (detResult.contributesToScore / 100f) else 0f

        val kwResult = HebrewKeywordScorer.score(normalized, hasUrl)
        val linkResult = LinkRiskAnalyzer.analyze(body, normalized)
        val (phoneScore, phoneReasons) = PhoneHeuristics.analyze(address, normalized, userCountryIso, isKnownContact)
        val mlScore = mlLayer.score(normalized)
        val campaignBonus = campaignDetector.getCampaignBonus(normalized)

        val heuristicComponent = (detScore + kwResult.score.coerceAtMost(1f)).coerceIn(0f, 1f)
        var score = (0.7f * heuristicComponent +
            0.2f * linkResult.score +
            0.1f * phoneScore +
            0.1f * mlScore +
            campaignBonus).coerceIn(0f, 1f)

        val reasons = mutableListOf<String>()
        if (detResult.matched) reasons.add("DET:${detResult.ruleType}")
        reasons.addAll(kwResult.reasons)
        reasons.addAll(linkResult.risks)
        reasons.addAll(phoneReasons)
        if (campaignBonus > 0f) reasons.add("campaign")

        val category = when {
            score >= 0.7f -> SpamCategory.DEFINITE_SPAM
            score >= 0.4f -> SpamCategory.SUSPECTED_SPAM
            else -> SpamCategory.SAFE
        }

        val confidence = (0.3f + 0.1f * reasons.size).coerceAtMost(1f)
        campaignDetector.addMessage(normalized)

        return SpamResult(
            score = score,
            category = category,
            reasons = reasons.distinct(),
            triggeredRules = kwResult.triggeredRules,
            confidence = confidence
        )
    }
}
