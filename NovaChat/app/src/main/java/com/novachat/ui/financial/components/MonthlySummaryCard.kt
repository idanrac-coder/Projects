package com.novachat.ui.financial.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novachat.domain.model.MonthComparison
import com.novachat.domain.model.MonthlySummary
import com.novachat.ui.financial.FinancialAccentLight
import com.novachat.ui.financial.FinancialGreen
import com.novachat.ui.financial.FinancialRed
import com.novachat.ui.financial.FinancialTextPrimary
import com.novachat.ui.financial.FinancialTextSecondary
import java.text.DateFormatSymbols
import kotlin.math.abs

@Composable
fun MonthlySummaryCard(
    summary: MonthlySummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    monthComparison: MonthComparison? = null,
    modifier: Modifier = Modifier
) {
    val monthName = DateFormatSymbols().months[summary.month - 1].replaceFirstChar { it.uppercase() }

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2D1B69), Color(0xFF1A0E40))
    )

    // Count-up animation
    val animatedTotal = remember { Animatable(0f) }
    var prevTotal by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(summary.total) {
        val target = summary.total.toFloat()
        if (target != prevTotal) {
            animatedTotal.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
            prevTotal = target
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF3D2B8A), RoundedCornerShape(20.dp))
            .background(gradient)
    ) {
        // Decorative circle blob — top-right corner, like the mockup ::before element
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color(0xFF6C5CE7).copy(alpha = 0.15f))
        )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label — "TOTAL SPENDING · APRIL" like the mockup
        Text(
            text = "TOTAL SPENDING · ${monthName.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = FinancialAccentLight.copy(alpha = 0.75f),
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(10.dp))

        // Animated big amount
        Text(
            text = "${currencySymbol(summary.currency)} ${"%.2f".format(animatedTotal.value.toDouble())}",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "${summary.count} transactions  ·  avg ${currencySymbol(summary.currency)}${"%.2f".format(summary.average)}",
            style = MaterialTheme.typography.bodyMedium,
            color = FinancialAccentLight.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
        Spacer(Modifier.height(14.dp))

        // Comparison stats row (like mockup: THIS MONTH / LAST MONTH / CHANGE)
        if (monthComparison != null) {
            val isUp = monthComparison.percentageChange > 0
            val pctText = if (monthComparison.previousTotal == 0.0) "NEW"
            else "${if (isUp) "▲" else "▼"} ${"%.1f".format(abs(monthComparison.percentageChange))}%"
            val pctColor = if (isUp) FinancialRed else FinancialGreen

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ComparisonStatItem(
                    label = "THIS MONTH",
                    value = "${currencySymbol(summary.currency)} ${"%.0f".format(monthComparison.currentTotal)}",
                    valueColor = Color.White
                )
                ComparisonStatItem(
                    label = "LAST MONTH",
                    value = "${currencySymbol(summary.currency)} ${"%.0f".format(monthComparison.previousTotal)}",
                    valueColor = FinancialTextSecondary
                )
                ComparisonStatItem(
                    label = "CHANGE",
                    value = pctText,
                    valueColor = pctColor
                )
            }
        } else {
            // No comparison data yet — show simple stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ComparisonStatItem("TRANSACTIONS", "${summary.count}", Color.White)
                ComparisonStatItem("AVG / TXN", "${currencySymbol(summary.currency)}${"%.0f".format(summary.average)}", Color.White)
                ComparisonStatItem("CURRENCY", summary.currency, Color.White)
            }
        }
    } // end Column
    } // end Box
}

@Composable
private fun ComparisonStatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FinancialTextSecondary,
            letterSpacing = 0.5.sp,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

internal fun currencySymbol(code: String): String = when (code) {
    "ILS" -> "₪"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    else -> "$code "
}
