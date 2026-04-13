package com.novachat.core.sms.financial

import javax.inject.Inject
import javax.inject.Singleton

data class ParsedFinancialData(
    val amount: Double?,
    val currency: String,
    val merchantName: String?,
    val cardLast4: String?,
    val dueDate: String?,
    val hasPaymentKeyword: Boolean,
    val hasDueDate: Boolean,
    val installments: Int? = null
)

enum class BankFormat { LEUMI, HAPOALIM, DISCOUNT, MIZRAHI, CREDIT_CARD, GENERIC }

@Singleton
class RegexParsingEngine @Inject constructor() {

    companion object {

        // ── Provider-specific transaction patterns ────────────────────────────

        // Isracard / AMEX format:
        //   "בכרטיסך 1408 אושרה עסקה ב-12/04 בסך 20.00 USD בCLAUDE.AI SUBSCRIPTION - UNITED STATES."
        //   "בכרטיסך המסתיים ב- 1408, אושרה עסקה ב-06/04 בסך 568.10 ש"ח ביוחננוף - אור יהודה."
        //   "בכרטיסך 0412 אושרה עסקה ב-31/03 בסך 2990.00 ש"ח ב- 5 תשלומים בעידו ספורט."
        // Groups: 1=card4, 2=date, 3=amount, 4=currency, 5=installments (optional), 6=merchant
        private val ISRACARD_TX = Regex(
            """בכרטיסך\s*(?:המסתיים\s*ב[-\s]*)?\s*(\d{4})[,]?\s*אושרה\s+עסקה\s+ב[-]?\d{1,2}/\d{2}\s+בסך\s+([\d,.]+)\s+(ש["״]?ח|USD|EUR|ILS|GBP)\s+ב[-]?\s*(?:(\d+)\s+תשלומים\s+ב[-]?\s*)?(.+?)(?=\.\s*\n|\.\s*לשירותך|\.\s*למידע|$)"""
        )
        // Groups: 1=card4, 2=amount, 3=currency, 4=installments (optional), 5=merchant

        // Cal format:
        //   "בכרטיסך המסתיים ב-9565 אושרה עסקה לחיוב ב-אל על נתיבי אויר לישראל על סך 22501.29 שח."
        //   "בכרטיסך המסתיים ב-9565 אושרה עסקה לחיוב ב-2C2P*BANGKOKAIRWAYS על סך 20360.00 TH."
        // Groups: 1=card4, 2=merchant, 3=amount, 4=currency
        private val CAL_TX = Regex(
            """בכרטיסך\s*(?:המסתיים\s*ב[-\s]*)?\s*(\d{4})\s*אושרה\s+עסקה\s+לחיוב\s+ב[-]?(.+?)\s+על\s+סך\s+([\d,.]+)\s*(שח|ש["״]?ח|USD|EUR|ILS|GBP|THB?)"""
        )

        // ── Non-transaction rejection keywords ───────────────────────────────
        private val NON_TX_KEYWORDS = listOf(
            "הטבה", "קופון", "חסכת", "צירפנו", "צירוף",
            "בקשתך", "קוד אימות", "דף הפירוט", "מסגרת האשראי",
            "שאלות ותשובות", "להורדה", "הורדה", "נסיונות הונאה",
            "שיוך כרטיס", "ארנק הדיגיטלי", "יתרת הנקודות",
            "הגדלת מסגרת", "הגדלה", "נתוני אשראי", "לשכת האשראי",
            "Google Pay", "Apple Pay"
        )

        // ── Fallback generic patterns ─────────────────────────────────────────
        val AMOUNT_PATTERN = Regex(
            """(?:₪|ILS|NIS|\$|USD|€|EUR|£|GBP)\s*[\d,]+\.?\d{0,2}|[\d,]+\.?\d{0,2}\s*(?:₪|ILS|NIS|\$|USD|€|EUR|£|GBP)|[\d,]+\s*ש["״]?ח"""
        )

        val PAYMENT_KEYWORDS = Regex(
            """(?i)\b(paid|debited|credited|charged|withdrawn|transferred|payment|תשלום|חיוב|זיכוי|renew|debit)\b"""
        )

        val DUE_DATE_PATTERN = Regex(
            """(?i)(?:due|by|before|עד|תאריך)\s*[:\-]?\s*(\d{1,2}[/\-.]\d{1,2}(?:[/\-.]\d{2,4})?)"""
        )

        val MERCHANT_PATTERN = Regex(
            """(?:at|from|to|מ|ב|ל)\s+([A-Za-z\u0590-\u05FF][\w\s\u0590-\u05FF]{1,30})"""
        )

        // Fixed: כרטיסך? handles both כרטיס and כרטיסך (possessive ך suffix)
        val CARD_LAST4_PATTERN = Regex(
            """(?i)(?:כרטיסך?\s*(?:המסתיים\s*ב[-\s]?)?|card\s*(?:ending\s*(?:in\s*)?|\.{2,3}\s*)?|[xX*]{3,4}[-\s]?|כ\.\s*|ending\s+in\s+)(\d{4})\b|\b\d{4}[-\s]\d{4}[-\s]\d{4}[-\s](\d{4})\b"""
        )

        val BANK_SENDERS = mapOf(
            Regex(".*leumi.*|.*לאומי.*", RegexOption.IGNORE_CASE) to BankFormat.LEUMI,
            Regex(".*hapoalim.*|.*הפועלים.*", RegexOption.IGNORE_CASE) to BankFormat.HAPOALIM,
            Regex(".*discount.*|.*דיסקונט.*", RegexOption.IGNORE_CASE) to BankFormat.DISCOUNT,
            Regex(".*mizrahi.*|.*מזרחי.*", RegexOption.IGNORE_CASE) to BankFormat.MIZRAHI,
            Regex(".*cal|.*כאל.*|.*visa.*", RegexOption.IGNORE_CASE) to BankFormat.CREDIT_CARD,
            Regex(".*max|.*מקס.*|.*isracard.*|.*ישראכרט.*", RegexOption.IGNORE_CASE) to BankFormat.CREDIT_CARD,
        )

        private val CURRENCY_SYMBOL_MAP = mapOf(
            "₪" to "ILS", "ILS" to "ILS", "NIS" to "ILS",
            "$" to "USD", "USD" to "USD",
            "€" to "EUR", "EUR" to "EUR",
            "£" to "GBP", "GBP" to "GBP",
            "ש\"ח" to "ILS", "ש״ח" to "ILS", "שח" to "ILS",
            "THB" to "THB", "TH" to "THB"
        )
    }

    fun isFinancialSms(body: String): Boolean {
        // Reject known non-transaction message types first
        if (NON_TX_KEYWORDS.any { body.contains(it) }) return false
        // Match provider-specific transaction patterns, then fall back to generic amount detection
        return ISRACARD_TX.containsMatchIn(body)
                || CAL_TX.containsMatchIn(body)
                || AMOUNT_PATTERN.containsMatchIn(body)
    }

    fun detectBankFormat(sender: String): BankFormat {
        for ((pattern, format) in BANK_SENDERS) {
            if (pattern.matches(sender)) return format
        }
        return BankFormat.GENERIC
    }

    fun detectIssuer(sender: String): String? {
        return when {
            sender.contains("isracard", ignoreCase = true) || sender.contains("ישראכרט", ignoreCase = true) -> "Isracard"
            sender.contains("max", ignoreCase = true) || sender.contains("מקס", ignoreCase = true) -> "MAX"
            sender.contains("cal", ignoreCase = true) || sender.contains("כאל", ignoreCase = true) -> "Cal"
            sender.contains("leumi", ignoreCase = true) || sender.contains("לאומי", ignoreCase = true) -> "Leumi"
            sender.contains("hapoalim", ignoreCase = true) || sender.contains("הפועלים", ignoreCase = true) -> "Hapoalim"
            sender.contains("discount", ignoreCase = true) || sender.contains("דיסקונט", ignoreCase = true) -> "Discount"
            sender.contains("chase", ignoreCase = true) -> "Chase"
            sender.contains("amex", ignoreCase = true) || sender.contains("american express", ignoreCase = true) -> "Amex"
            else -> null
        }
    }

    fun parse(body: String, sender: String): ParsedFinancialData {
        // 1. Try Isracard / AMEX structured pattern
        ISRACARD_TX.find(body)?.let { m ->
            val cardLast4 = m.groupValues[1].ifEmpty { null }
            val amount = parseAmount(m.groupValues[2])
            val currency = normalizeCurrency(m.groupValues[3])
            val installments = m.groupValues[4].toIntOrNull()
            val merchant = m.groupValues[5].trim().trimEnd('.')
            return ParsedFinancialData(
                amount = amount,
                currency = currency,
                merchantName = merchant.ifEmpty { null },
                cardLast4 = cardLast4,
                dueDate = null,
                hasPaymentKeyword = true,
                hasDueDate = false,
                installments = installments
            )
        }

        // 2. Try Cal structured pattern
        CAL_TX.find(body)?.let { m ->
            val cardLast4 = m.groupValues[1].ifEmpty { null }
            val merchant = m.groupValues[2].trim().trimEnd('.')
            val amount = parseAmount(m.groupValues[3])
            val currency = normalizeCurrency(m.groupValues[4])
            return ParsedFinancialData(
                amount = amount,
                currency = currency,
                merchantName = merchant.ifEmpty { null },
                cardLast4 = cardLast4,
                dueDate = null,
                hasPaymentKeyword = true,
                hasDueDate = false
            )
        }

        // 3. Generic fallback for unknown provider formats
        return genericParse(body)
    }

    private fun genericParse(body: String): ParsedFinancialData {
        val amountMatch = AMOUNT_PATTERN.find(body)
        val amountAndCurrency = amountMatch?.let { extractAmountAndCurrency(it.value) }

        val merchantMatch = MERCHANT_PATTERN.find(body)
        val merchantName = merchantMatch?.groupValues?.get(1)?.trim()

        val cardMatch = CARD_LAST4_PATTERN.find(body)
        val cardLast4 = cardMatch?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }?.ifEmpty { null }

        val dueDateMatch = DUE_DATE_PATTERN.find(body)
        val hasPayment = PAYMENT_KEYWORDS.containsMatchIn(body)

        return ParsedFinancialData(
            amount = amountAndCurrency?.first,
            currency = amountAndCurrency?.second ?: "ILS",
            merchantName = merchantName,
            cardLast4 = cardLast4,
            dueDate = dueDateMatch?.groupValues?.get(1),
            hasPaymentKeyword = hasPayment,
            hasDueDate = dueDateMatch != null
        )
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.replace(",", "").trim()
        return cleaned.toDoubleOrNull()
    }

    private fun normalizeCurrency(raw: String): String {
        return CURRENCY_SYMBOL_MAP[raw] ?: CURRENCY_SYMBOL_MAP[raw.uppercase()] ?: raw.uppercase().ifEmpty { "ILS" }
    }

    private fun extractAmountAndCurrency(raw: String): Pair<Double, String> {
        var currency = "ILS"
        for ((symbol, code) in CURRENCY_SYMBOL_MAP) {
            if (raw.contains(symbol, ignoreCase = true)) {
                currency = code
                break
            }
        }
        val numStr = raw.replace(Regex("""[^\d.,]"""), "").replace(",", "")
        val amount = numStr.toDoubleOrNull() ?: 0.0
        return amount to currency
    }
}
