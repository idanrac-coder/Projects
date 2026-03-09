package com.novachat.core.sms.hebrew

/**
 * Sender-based heuristics: international, brand mismatch, bulk, allowlist.
 * Used by HebrewSpamEngine to adjust score.
 */
object PhoneHeuristics {

    private val ISRAELI_DIAL = setOf("972")
    private val BRAND_SENDER_PATTERN = Regex("^[A-Za-z0-9]+$")

    fun analyze(
        address: String,
        normalizedBody: String,
        userCountryIso: String?,
        isKnownContact: Boolean
    ): Pair<Float, List<String>> {
        val reasons = mutableListOf<String>()
        var score = 0f
        if (isKnownContact) return 0f to emptyList()

        val normalized = address.replace(Regex("[^+0-9]"), "")
        val isInternational = when {
            normalized.startsWith("+") -> {
                val dial = when {
                    normalized.drop(1).startsWith("972") -> "972"
                    normalized.drop(1).startsWith("1") && normalized.length >= 11 -> "1"
                    else -> normalized.drop(1).take(3)
                }
                userCountryIso == "IL" && dial !in ISRAELI_DIAL
            }
            else -> false
        }
        if (isInternational) {
            score += 0.15f
            reasons.add("international_sender")
        }

        val bodyMentionsBank = listOf("בנק", "לאומי", "הפועלים", "דיסקונט", "מזרחי", "חשבון", "נחסם").any { it in normalizedBody }
        val isNumericSender = address.all { it.isDigit() || it in "+ -" } && address.replace(Regex("[^0-9]"), "").length >= 8
        if (bodyMentionsBank && isNumericSender) {
            score += 0.25f
            reasons.add("sender_brand_mismatch")
        }

        return score.coerceIn(0f, 0.5f) to reasons
    }
}
