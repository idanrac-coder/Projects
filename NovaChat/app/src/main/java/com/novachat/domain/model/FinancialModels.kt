package com.novachat.domain.model

import com.novachat.core.sms.financial.FinancialCategory

data class MonthlySummary(
    val total: Double,
    val count: Int,
    val average: Double,
    val currency: String,
    val month: Int,
    val year: Int
)

data class CategoryBreakdown(
    val category: FinancialCategory,
    val total: Double,
    val percentage: Float,
    val count: Int
)

data class DailySpending(
    val dayOfMonth: Int,
    val total: Double
)

data class SubscriptionInfo(
    val id: Long,
    val merchantName: String,
    val amount: Double,
    val previousAmount: Double?,
    val currency: String,
    val frequency: String,
    val lastCharged: Long,
    val cardLast4: String?,
    val cardNickname: String?,
    val isActive: Boolean
)

data class CardInfo(
    val last4: String,
    val nickname: String?,
    val issuer: String?,
    val transactionCount: Int,
    val totalSpent: Double,
    val isHidden: Boolean
)

data class SenderInfo(
    val id: Long,
    val address: String,
    val displayName: String?,
    val isEnabled: Boolean,
    val alertsEnabled: Boolean,
    val source: String,
    val transactionCount: Int
)

data class AlertInfo(
    val id: Long,
    val type: String,
    val message: String,
    val timestamp: Long,
    val isDismissed: Boolean,
    val transactionId: Long
)

data class TransactionInfo(
    val id: Long,
    val merchantName: String?,
    val amount: Double,
    val currency: String,
    val category: String,
    val timestamp: Long,
    val cardLast4: String?,
    val cardNickname: String?,
    val isRecurring: Boolean,
    val senderAddress: String? = null
)

data class TopMerchant(
    val merchantName: String,
    val totalSpent: Double,
    val transactionCount: Int
)

data class MonthComparison(
    val currentTotal: Double,
    val previousTotal: Double,
    val percentageChange: Double,
    val currency: String,
    val currentMonth: Int,
    val currentYear: Int
)

data class SpendingVelocity(
    val dailyRate: Double,
    val projectedMonthTotal: Double,
    val daysElapsed: Int,
    val daysInMonth: Int,
    val currency: String
)
