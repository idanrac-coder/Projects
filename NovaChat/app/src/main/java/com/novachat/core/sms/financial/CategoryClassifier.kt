package com.novachat.core.sms.financial

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryClassifier @Inject constructor() {

    private val knownSubscriptionMerchants = listOf(
        "netflix", "disney", "spotify", "claude.ai", "cursor",
        "apple.com/bill", "google play", "youtube", "amazon prime",
        "hbo", "hulu", "apple tv", "paramount", "peacock",
        "microsoft", "office 365", "adobe", "dropbox", "icloud",
        "one drive", "linkedin", "canva", "figma", "notion",
        "slack", "zoom", "github", "chatgpt", "openai"
    )

    fun classifyByMerchantName(merchantName: String?): FinancialCategory? {
        if (merchantName == null) return null
        val lower = merchantName.lowercase()
        return if (knownSubscriptionMerchants.any { lower.contains(it) })
            FinancialCategory.SUBSCRIPTION
        else
            null
    }

    fun classify(
        merchantName: String? = null,
        isRecurring: Boolean,
        hasDueDate: Boolean,
        hasPaymentKeyword: Boolean,
        body: String
    ): FinancialCategory {
        classifyByMerchantName(merchantName)?.let { return it }
        if (isRecurring) return FinancialCategory.SUBSCRIPTION
        if (hasDueDate) return FinancialCategory.BILL
        if (containsBillKeywords(body)) return FinancialCategory.BILL
        if (hasPaymentKeyword) return FinancialCategory.PAYMENT
        return FinancialCategory.EXPENSE
    }

    private fun containsBillKeywords(body: String): Boolean {
        val keywords = listOf(
            "bill", "invoice", "חשבון", "חשבונית",
            "utility", "electric", "water", "gas", "חשמל", "מים", "גז",
            "internet", "phone", "טלפון", "אינטרנט"
        )
        val lower = body.lowercase()
        return keywords.any { lower.contains(it) }
    }
}
