package com.novachat.core.sms

/**
 * Layer 1: High-performance deterministic regex engine for common phishing patterns.
 * Catches shortened URLs, suspicious TLDs, urgent keywords, OTP/Verify patterns,
 * Hebrew tax refund scams (החזרי/החזר מס), survey/political spam (להסרה: השיבו),
 * and political/election campaign spam (סקר, מנדטים, בחירות, הצביעו, קלפי).
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
    // Political polls & election campaign spam (Israeli political context)
    private val politicalPollRegex = Regex("סקר|מנדטים|בבחירות.*בוחרים|בוחרים ב\\S+!|הצביעו ל|להצביע ל|יום הבחירות|קלפי")
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

    private data class Rule(val regex: Regex, val type: String, val score: Int, val priority: Int)

    private val rules = listOf(
        Rule(taxRefundScamRegex, "TAX_REFUND_SCAM", 25, 100),
        Rule(israeliPanicRegex, "ISRAELI_PANIC", 30, 95),
        Rule(surveyUnsubscribeRegex, "SURVEY_UNSUBSCRIBE", 35, 90),
        Rule(politicalPollRegex, "POLITICAL_POLL", 35, 90),
        Rule(israeliBankRegex, "ISRAELI_BANK", 25, 85),
        Rule(israeliGovRegex, "ISRAELI_GOV", 25, 85),
        Rule(israeliDeliveryRegex, "ISRAELI_DELIVERY", 25, 80),
        Rule(israeliCryptoRegex, "ISRAELI_CRYPTO", 25, 80),
        Rule(ipUrlRegex, "IP_URL", 25, 70),
        Rule(suspiciousTldRegex, "SUSPICIOUS_TLD", 25, 65),
        Rule(shortenedUrlRegex, "SHORTENED_URL", 25, 60),
        Rule(urgentKeywordsRegex, "URGENT_KEYWORDS", 20, 50),
        Rule(otpVerifyEnglishRegex, "OTP_VERIFY_EN", 25, 40),
        Rule(otpVerifyHebrewRegex, "OTP_VERIFY_HE", 25, 40)
    )

    data class MatchResult(
        val matched: Boolean,
        val ruleType: String?,
        val contributesToScore: Int,
        val matchedRuleTypes: List<String> = emptyList()
    )

    /**
     * Runs all deterministic pattern checks on message body.
     * Returns the highest-priority match as the primary ruleType,
     * with contributesToScore accumulated from all matched patterns (capped at 60).
     */
    fun analyze(body: String): MatchResult {
        if (body.length < 10) return MatchResult(false, null, 0)

        var bestRule: Rule? = null
        var totalScore = 0
        val matchedTypes = mutableListOf<String>()

        for (rule in rules) {
            if (rule.regex.containsMatchIn(body)) {
                matchedTypes.add(rule.type)
                totalScore += rule.score
                if (bestRule == null || rule.priority > bestRule.priority) {
                    bestRule = rule
                }
            }
        }

        if (bestRule == null) return MatchResult(false, null, 0)

        return MatchResult(
            matched = true,
            ruleType = bestRule.type,
            contributesToScore = totalScore.coerceAtMost(60),
            matchedRuleTypes = matchedTypes
        )
    }
}
