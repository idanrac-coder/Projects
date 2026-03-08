package com.novachat.core.sms

/**
 * Layer 2: Weighted heuristic scoring system.
 * Point system:
 * - Sender not in Contacts: +40
 * - Contains URL: +30
 * - Contains OTP/Verify (English or Hebrew): +20
 * - High non-alphanumeric ratio (>15%): +15
 * Thresholds: >75 = Spam, 40-74 = Suspicious, <40 = Safe
 */
object HeuristicSpamLayer {

    private val urlRegex = Regex("(?i)https?://\\S+")
    private val otpVerifyEnglishRegex = Regex(
        "(?i)(OTP|verify|verification\\s+code|verification\\s+code)"
    )
    private val otpVerifyHebrewRegex = Regex(
        "קוד\\s*אימות|הזן\\s*את\\s*הקוד|קוד\\s*זמני|שלח.*הקוד|אימות"
    )

    private const val SCORE_SENDER_UNKNOWN = 40
    private const val SCORE_CONTAINS_URL = 30
    private const val SCORE_OTP_VERIFY = 20
    private const val SCORE_HIGH_SPECIAL_CHARS = 15
    private const val THRESHOLD_SPAM = 75
    private const val THRESHOLD_SUSPICIOUS = 40
    private const val NON_ALPHA_THRESHOLD_RATIO = 0.15f

    enum class Classification { SPAM, SUSPICIOUS, SAFE }

    data class Result(
        val score: Int,
        val classification: Classification,
        val breakdown: Map<String, Int>
    )

    /**
     * Computes weighted heuristic score. Use Character.isLetterOrDigit() for letters
     * so Hebrew (and other Unicode letters) are not counted as non-alphanumeric.
     */
    fun analyze(
        body: String,
        isKnownContact: Boolean,
        deterministicBonus: Int = 0
    ): Result {
        val breakdown = mutableMapOf<String, Int>()
        var score = deterministicBonus

        if (!isKnownContact) {
            score += SCORE_SENDER_UNKNOWN
            breakdown["sender_unknown"] = SCORE_SENDER_UNKNOWN
        }
        if (urlRegex.containsMatchIn(body)) {
            score += SCORE_CONTAINS_URL
            breakdown["contains_url"] = SCORE_CONTAINS_URL
        }
        if (otpVerifyEnglishRegex.containsMatchIn(body) || otpVerifyHebrewRegex.containsMatchIn(body)) {
            score += SCORE_OTP_VERIFY
            breakdown["otp_verify"] = SCORE_OTP_VERIFY
        }
        val nonAlphaRatio = computeNonAlphanumericRatio(body)
        if (nonAlphaRatio > NON_ALPHA_THRESHOLD_RATIO && body.length >= 20) {
            score += SCORE_HIGH_SPECIAL_CHARS
            breakdown["high_special_chars"] = SCORE_HIGH_SPECIAL_CHARS
        }
        if (deterministicBonus > 0) {
            breakdown["deterministic"] = deterministicBonus
        }

        val classification = when {
            score > THRESHOLD_SPAM -> Classification.SPAM
            score >= THRESHOLD_SUSPICIOUS -> Classification.SUSPICIOUS
            else -> Classification.SAFE
        }
        return Result(score, classification, breakdown)
    }

    /**
     * Counts chars that are neither letter nor digit (using Unicode categories).
     * Hebrew letters are letters (isLetter=true), so they are not penalized.
     */
    private fun computeNonAlphanumericRatio(text: String): Float {
        if (text.isEmpty()) return 0f
        var nonAlphaCount = 0
        for (c in text) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                nonAlphaCount++
            }
        }
        return nonAlphaCount.toFloat() / text.length
    }
}
