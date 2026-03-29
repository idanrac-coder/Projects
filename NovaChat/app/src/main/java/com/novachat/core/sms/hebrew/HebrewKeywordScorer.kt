package com.novachat.core.sms.hebrew

/**
 * Weighted keyword/phrase rules for Israeli spam. Run on normalized body.
 */
object HebrewKeywordScorer {

    data class Rule(
        val name: String,
        val pattern: Regex,
        val score: Float,
        val description: String
    )

    // Patterns use normalized forms: ם→מ, ן→נ, etc. (run on normalized body)
    private val RULES = listOf(
        Rule("bank_phishing", Regex("חשבנ.*נחסמ|כרטיס.*נחסמ|בנק.*נחסמ|בנק.*(לאומי|הפועלים|דיסקונט|מזרחי).*נחסמ"), 0.75f, "Bank account blocked"),
        Rule("delivery_scam", Regex("חבילה.*ממתינה|דואר.*ישראל.*חבילה|חבילתכ|(DHL|UPS).*חבילה"), 0.65f, "Delivery package scam"),
        Rule("tax_refund_scam", Regex("החזר.*מס|רשות.*המיסים"), 0.7f, "Tax refund / gov"),
        Rule("crypto_scam", Regex("קריפטו|השקעה.*מובטחת|ביטקוין|מטבעות.*דיגיטליים"), 0.65f, "Crypto investment"),
        Rule("panic_disinformation", Regex("מתקפת.*טילים|חפש.*מקלט|אזהרה.*טילים|Red.?Alert|אזעקה"), 0.8f, "Panic / war misinformation"),
        Rule("government_impersonation", Regex("(משטרה|רשות.*המיסים|פיקוד.*העורף).*(הודעה.*חשובה|תגובה.*נדרשת)"), 0.65f, "Government impersonation"),
        Rule("otp_phishing", Regex("שלח.*הקוד|קוד.*אימות.*לחץ|הזן.*הקוד"), 0.85f, "OTP phishing"),
        Rule("political_campaign", Regex("בבחירות.*בוחרים|בוחרים ב|הצביעו ל|בחירות.*הקרובות|קלפי"), 0.7f, "Political campaign spam"),
        Rule("soft_urgency", Regex("חשוב.*מאוד|תוקף.*פג|פעולה.*נדרשת|הודעה.*קריטית"), 0.55f, "Soft urgency"),
        Rule("לחץ_כאן", Regex("לחץ.*כאן|עדכן.*פרטים"), 0.7f, "Click here / update details")
    )

    data class ScoreResult(
        val score: Float,
        val reasons: List<String>,
        val triggeredRules: List<TriggeredRule>
    )

    fun score(normalizedBody: String, hasUrl: Boolean): ScoreResult {
        if (normalizedBody.length < 5) return ScoreResult(0f, emptyList(), emptyList())
        val reasons = mutableListOf<String>()
        val triggered = mutableListOf<TriggeredRule>()
        var totalScore = 0f
        for (rule in RULES) {
            val match = rule.pattern.find(normalizedBody) ?: continue
            totalScore += rule.score
            reasons.add(rule.description)
            triggered.add(TriggeredRule(rule.name, "KEYWORD", match.value, rule.score))
        }
        if (totalScore > 0f && hasUrl && normalizedBody.contains("קוד")) {
            val otpUrlBonus = 0.1f
            totalScore += otpUrlBonus
            reasons.add("OTP with URL")
        }
        return ScoreResult(totalScore.coerceAtMost(1f), reasons, triggered)
    }
}
