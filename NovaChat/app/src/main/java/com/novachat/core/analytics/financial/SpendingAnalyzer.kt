package com.novachat.core.analytics.financial

import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.SubscriptionDao
import com.novachat.core.database.financial.dao.CategoryTotal
import com.novachat.core.database.financial.dao.DailyTotal
import com.novachat.core.sms.financial.FinancialCategory
import com.novachat.domain.model.CategoryBreakdown
import com.novachat.domain.model.DailySpending
import com.novachat.domain.model.MonthlySummary
import com.novachat.domain.model.TopMerchant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingAnalyzer @Inject constructor(
    private val transactionDao: FinancialTransactionDao,
    private val subscriptionDao: SubscriptionDao
) {
    fun getMonthlySummary(year: Int, month: Int, cardLast4: String?): Flow<MonthlySummary> {
        val (start, end) = getMonthRange(year, month)
        return combine(
            transactionDao.getTotalInRange(start, end, cardLast4),
            transactionDao.getCountInRange(start, end, cardLast4)
        ) { total, count ->
            MonthlySummary(
                total = total,
                count = count,
                average = if (count > 0) total / count else 0.0,
                currency = "ILS",
                month = month,
                year = year
            )
        }.flowOn(Dispatchers.IO)
    }

    fun getCategoryBreakdown(year: Int, month: Int, cardLast4: String?): Flow<List<CategoryBreakdown>> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.getCategoryBreakdown(start, end, cardLast4).map { totals ->
            val grandTotal = totals.sumOf { it.total }.coerceAtLeast(0.01)
            totals.map { ct ->
                CategoryBreakdown(
                    category = try { FinancialCategory.valueOf(ct.category) } catch (_: Exception) { FinancialCategory.EXPENSE },
                    total = ct.total,
                    percentage = (ct.total / grandTotal * 100).toFloat(),
                    count = ct.count
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getDailySpending(year: Int, month: Int, cardLast4: String?): Flow<List<DailySpending>> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.getDailySpending(start, end, cardLast4).map { dailies ->
            dailies.map { DailySpending(dayOfMonth = it.dayOfMonth, total = it.total) }
        }.flowOn(Dispatchers.IO)
    }

    fun getTopMerchants(year: Int, month: Int, cardLast4: String?, limit: Int = 5): Flow<List<TopMerchant>> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.getTopMerchants(start, end, cardLast4, limit).map { list ->
            list.map { TopMerchant(merchantName = it.merchantName, totalSpent = it.totalSpent, transactionCount = it.txCount) }
        }.flowOn(Dispatchers.IO)
    }

    fun getSubscriptionTotal(cardLast4: String?): Flow<Double> =
        subscriptionDao.getActiveTotal(cardLast4)

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }
}
