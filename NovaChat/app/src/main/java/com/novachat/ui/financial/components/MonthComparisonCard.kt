package com.novachat.ui.financial.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.R
import com.novachat.domain.model.MonthComparison
import com.novachat.ui.financial.FinancialCard
import com.novachat.ui.financial.FinancialGreen
import com.novachat.ui.financial.FinancialRed
import com.novachat.ui.financial.FinancialTextPrimary
import com.novachat.ui.financial.FinancialTextSecondary
import java.text.DateFormatSymbols
import kotlin.math.abs

@Composable
fun MonthComparisonCard(
    comparison: MonthComparison,
    modifier: Modifier = Modifier
) {
    val months = DateFormatSymbols().months
    val currentMonthName = months[comparison.currentMonth - 1].take(3)
    val prevMonth = if (comparison.currentMonth == 1) 12 else comparison.currentMonth - 1
    val prevYear = if (comparison.currentMonth == 1) comparison.currentYear - 1 else comparison.currentYear
    val prevMonthName = months[prevMonth - 1].take(3)
    val symbol = currencySymbol(comparison.currency)

    val newLabel = stringResource(R.string.comparison_new_label)
    val isUp = comparison.percentageChange > 0
    val pctText = if (comparison.previousTotal == 0.0) newLabel
    else "${if (isUp) "▲" else "▼"}${"%.1f".format(abs(comparison.percentageChange))}%"
    val pctColor = if (isUp) FinancialRed else FinancialGreen

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinancialCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.month_vs_last_month),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = FinancialTextPrimary
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = pctColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = pctText,
                    style = MaterialTheme.typography.labelMedium,
                    color = pctColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = prevMonthName + if (prevYear != comparison.currentYear) " $prevYear" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancialTextSecondary
                )
                Text(
                    text = "$symbol${"%.2f".format(comparison.previousTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = FinancialTextSecondary
                )
            }

            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = FinancialTextSecondary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentMonthName,
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancialTextPrimary
                )
                Text(
                    text = "$symbol${"%.2f".format(comparison.currentTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinancialTextPrimary
                )
            }
        }
    }
}
