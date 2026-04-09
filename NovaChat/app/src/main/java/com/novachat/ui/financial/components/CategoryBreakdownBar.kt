package com.novachat.ui.financial.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.core.sms.financial.FinancialCategory
import com.novachat.domain.model.CategoryBreakdown

val CATEGORY_COLORS = mapOf(
    FinancialCategory.BILL to Color(0xFF2196F3),
    FinancialCategory.SUBSCRIPTION to Color(0xFF9C27B0),
    FinancialCategory.PAYMENT to Color(0xFF4CAF50),
    FinancialCategory.EXPENSE to Color(0xFFFF9800)
)

@Composable
fun CategoryBreakdownBar(
    breakdown: List<CategoryBreakdown>,
    onSubscriptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) return

    val total = breakdown.sumOf { it.total }.coerceAtLeast(0.01)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            breakdown.forEach { item ->
                val fraction = (item.total / total).toFloat().coerceIn(0f, 1f)
                val color = CATEGORY_COLORS[item.category] ?: Color.Gray
                val clickModifier = if (item.category == FinancialCategory.SUBSCRIPTION) {
                    Modifier.clickable { onSubscriptionClick() }
                } else Modifier

                Box(
                    modifier = clickModifier
                        .weight(fraction.coerceAtLeast(0.01f))
                        .height(16.dp)
                        .background(color)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            breakdown.forEach { item ->
                val color = CATEGORY_COLORS[item.category] ?: Color.Gray
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = item.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${"%.0f".format(item.percentage)}% · ₪${"%.0f".format(item.total)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
