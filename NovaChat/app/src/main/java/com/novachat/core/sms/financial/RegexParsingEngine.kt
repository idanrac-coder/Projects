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
    val hasDueDate: Boolean
)

enum class BankFormat { LEUMI, HAPOALIM, DISCOUNT, MIZRAHI, CREDIT_CARD, GENERIC }

@Singleton
class RegexParsingEngine @Inject constructor() {

    companion object {
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

        val CARD_LAST4_PATTERN = Regex(
            """(?i)(?:כרטיס\s*(?:המסתיים\s*ב[-\s]?)?|card\s*(?:ending\s*(?:in\s*)?|\.{2,3}\s*)?|[xX*]{3,4}[-\s]?|כ\.\s*|ending\s+in\s+)(\d{4})\b|\b\d{4}[-\s]\d{4}[-\s]\d{4}[-\s](\d{4})\b"""
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
            "ש\"ח" to "ILS", "ש״ח" to "ILS", "שח" to "ILS"
        )
    }

    fun isFinancialSms(body: String): Boolean = AMOUNT_PATTERN.containsMatchIn(body)

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
