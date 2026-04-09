package com.novachat.core.sms.financial

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryClassifier @Inject constructor() {

    fun classify(
        isRecurring: Boolean,
        hasDueDate: Boolean,
        hasPaymentKeyword: Boolean,
        body: String
    ): FinancialCategory {
        if (isRecurring) return FinancialCategory.SUBSCRIPTION
        if (hasDueDate) return FinancialCategory.BILL
        if (hasPaymentKeyword) return FinancialCategory.PAYMENT
        if (containsBillKeywords(body)) return FinancialCategory.BILL
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
