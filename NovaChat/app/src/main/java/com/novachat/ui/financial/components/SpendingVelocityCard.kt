package com.novachat.ui.financial.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.R
import com.novachat.domain.model.SpendingVelocity
import com.novachat.ui.financial.FinancialAccent
import com.novachat.ui.financial.FinancialAccentLight
import com.novachat.ui.financial.FinancialAmber
import com.novachat.ui.financial.FinancialCard
import com.novachat.ui.financial.FinancialDivider
import com.novachat.ui.financial.FinancialTextPrimary
import com.novachat.ui.financial.FinancialTextSecondary

@Composable
fun SpendingVelocityCard(
    velocity: SpendingVelocity,
    modifier: Modifier = Modifier
) {
    val symbol = currencySymbol(velocity.currency)
    val targetProgress = if (velocity.projectedMonthTotal > 0)
        (velocity.dailyRate * velocity.daysElapsed / velocity.projectedMonthTotal).toFloat().coerceIn(0f, 1f)
    else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "velocityProgress"
    )

    val daysLeft = velocity.daysInMonth - velocity.daysElapsed

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinancialCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.spending_velocity),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = FinancialTextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.daily_rate_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancialTextSecondary
                )
                Text(
                    text = "~$symbol${"%.2f".format(velocity.dailyRate)}/day",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FinancialTextPrimary
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = stringResource(R.string.projected_total),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancialTextSecondary
                )
                Text(
                    text = "$symbol${"%.2f".format(velocity.projectedMonthTotal)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FinancialAmber
                )
            }
        }

        // Animated progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(FinancialDivider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(FinancialAccent, FinancialAccentLight)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Day 1", style = MaterialTheme.typography.labelSmall, color = FinancialTextSecondary)
            Text(text = "Today (day ${velocity.daysElapsed})", style = MaterialTheme.typography.labelSmall, color = FinancialTextSecondary)
            Text(text = "Day ${velocity.daysInMonth}", style = MaterialTheme.typography.labelSmall, color = FinancialTextSecondary)
        }
    }
}
