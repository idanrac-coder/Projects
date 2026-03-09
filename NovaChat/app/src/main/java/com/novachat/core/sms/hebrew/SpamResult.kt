package com.novachat.core.sms.hebrew

/**
 * Result of Hebrew/Israeli spam detection.
 * SAFE: score < 0.4; SUSPECTED_SPAM: 0.4–0.7; DEFINITE_SPAM: >= 0.7.
 */
data class SpamResult(
    val score: Float,
    val category: SpamCategory,
    val reasons: List<String>,
    val triggeredRules: List<TriggeredRule> = emptyList(),
    val confidence: Float = 0f
)

data class TriggeredRule(
    val ruleId: String,
    val ruleType: String,
    val matchedValue: String?,
    val scoreContribution: Float
)

enum class SpamCategory {
    SAFE,
    SUSPECTED_SPAM,
    DEFINITE_SPAM
}
