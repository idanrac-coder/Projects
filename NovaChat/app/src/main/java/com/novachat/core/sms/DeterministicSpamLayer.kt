package com.novachat.core.sms

/**
 * Layer 1: High-performance deterministic regex engine for common phishing patterns.
 * Catches shortened URLs, suspicious TLDs, urgent keywords, OTP/Verify patterns,
 * Hebrew tax refund scams (החזרי/החזר מס), survey/political spam (להסרה: השיבו),
 * and political polls (סקר, מנדטים).
 * Runs first before heuristic/semantic layers.
 */
object DeterministicSpamLayer {

    private val shortenedUrlRegex = Regex(
        "(?i)(bit\\.ly|tinyurl|t\\.co|goo\\.gl|rb\\.gy|shorturl|cutt\\.ly|is\\.gd|v\\.gd|qr\\.ae|clck\\.ru)/\\S+"
    )
    private val suspiciousTldRegex = Regex(
        "(?i)https?://[a-z0-9-]+\\.(tk|ml|ga|cf|gq|xyz|top|buzz|club|icu|work|info|rest|click|link|online|site|fun)/\\S*"
    )
    private val urgentKeywordsRegex = Regex(
        "(?i)(act\\s+now|respond\\s+immediately|urgent|expires?\\s+(today|soon|in\\s+\\d+\\s*(hour|min|second))|verify\\s+now|confirm\\s+immediately|דחוף|מיד|בהקדם|פעולה מיידית)"
    )
    private val otpVerifyEnglishRegex = Regex(
        "(?i)(OTP|verify|verification code|verification code)"
    )
    private val otpVerifyHebrewRegex = Regex(
        "קוד\\s*אימות|הזן\\s*את\\s*הקוד|קוד\\s*זמני|שלח.*הקוד|קוד\\s*בן\\s*6\\s*ספרות"
    )
    private val ipUrlRegex = Regex(
        "(?i)https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}[/\\S]*"
    )
    // Hebrew tax refund scam (unsolicited SMS from unknown senders)
    private val taxRefundScamRegex = Regex("החזרי?\\s*מס")
    // Survey/political spam with unsubscribe reply (common in bulk SMS)
    private val surveyUnsubscribeRegex = Regex("להסרה:\\s*השיבו")
    // Political polls: survey or Knesset mandates (Israeli political context)
    private val politicalPollRegex = Regex("סקר|מנדטים")
    // Israeli bank phishing (normalized: ם→מ, ן→נ)
    private val israeliBankRegex = Regex("בנק.*(נחסמ|ננעל)|כרטיס.*נחסמ|חשבנ.*נחסמ|לאומי|הפועלים|דיסקונט|מזרחי")
    // Israeli delivery scam
    private val israeliDeliveryRegex = Regex("חבילה.*ממתינה|דואר.*ישראל|משלוח.*מחכה|חבילתך")
    // Israeli tax/gov
    private val israeliGovRegex = Regex("רשות המיסים|משטרה|פיקוד העורף|מבקר המדינה")
    // Panic / war
    private val israeliPanicRegex = Regex("מתקפת טילים|חפש מקלט|אזהרה.*טילים|אזעקה")
    // Crypto
    private val israeliCryptoRegex = Regex("קריפטו|השקעה מובטחת|ביטקוין")

    data class MatchResult(
        val matched: Boolean,
        val ruleType: String?,
        val contributesToScore: Int
    )

    /**
     * Runs deterministic pattern check on message body.
     * @return MatchResult with matched flag, rule type, and score contribution (e.g. +25 for strong match)
     */
    fun analyze(body: String): MatchResult {
        if (body.length < 10) return MatchResult(false, null, 0)

        when {
            shortenedUrlRegex.containsMatchIn(body) ->
                return MatchResult(true, "SHORTENED_URL", 25)
            suspiciousTldRegex.containsMatchIn(body) ->
                return MatchResult(true, "SUSPICIOUS_TLD", 25)
            ipUrlRegex.containsMatchIn(body) ->
                return MatchResult(true, "IP_URL", 25)
            urgentKeywordsRegex.containsMatchIn(body) ->
                return MatchResult(true, "URGENT_KEYWORDS", 20)
            otpVerifyEnglishRegex.containsMatchIn(body) ->
                return MatchResult(true, "OTP_VERIFY_EN", 25)
            otpVerifyHebrewRegex.containsMatchIn(body) ->
                return MatchResult(true, "OTP_VERIFY_HE", 25)
            taxRefundScamRegex.containsMatchIn(body) ->
                return MatchResult(true, "TAX_REFUND_SCAM", 25)
            surveyUnsubscribeRegex.containsMatchIn(body) ->
                return MatchResult(true, "SURVEY_UNSUBSCRIBE", 35)
            politicalPollRegex.containsMatchIn(body) ->
                return MatchResult(true, "POLITICAL_POLL", 35)
            israeliBankRegex.containsMatchIn(body) ->
                return MatchResult(true, "ISRAELI_BANK", 25)
            israeliDeliveryRegex.containsMatchIn(body) ->
                return MatchResult(true, "ISRAELI_DELIVERY", 25)
            israeliGovRegex.containsMatchIn(body) ->
                return MatchResult(true, "ISRAELI_GOV", 25)
            israeliPanicRegex.containsMatchIn(body) ->
                return MatchResult(true, "ISRAELI_PANIC", 30)
            israeliCryptoRegex.containsMatchIn(body) ->
                return MatchResult(true, "ISRAELI_CRYPTO", 25)
        }
        return MatchResult(false, null, 0)
    }
}
