package com.novachat.ui.financial.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.core.sms.financial.FinancialCategory
import com.novachat.domain.model.CategoryBreakdown
import com.novachat.domain.model.UserCategory
import com.novachat.ui.financial.FinancialCard
import com.novachat.ui.financial.FinancialTextPrimary
import com.novachat.ui.financial.FinancialTextSecondary

val CATEGORY_COLORS = mapOf(
    FinancialCategory.BILL to Color(0xFF42A5F5),
    FinancialCategory.SUBSCRIPTION to Color(0xFF6C5CE7),
    FinancialCategory.PAYMENT to Color(0xFF00B894),
    FinancialCategory.EXPENSE to Color(0xFFFDCB6E)
)

fun resolveCategory(id: String, userCategories: List<UserCategory>): Pair<String, Color> {
    val cat = userCategories.firstOrNull { it.id == id && !it.isDeleted }
    val color = cat?.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
    } ?: CATEGORY_COLORS[runCatching { FinancialCategory.valueOf(id) }.getOrNull()] ?: Color.Gray
    return (cat?.displayName ?: id.lowercase().replaceFirstChar { it.uppercase() }) to color
}

@Composable
fun CategoryBreakdownBar(
    breakdown: List<CategoryBreakdown>,
    userCategories: List<UserCategory> = emptyList(),
    onSubscriptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) return

    val total = breakdown.sumOf { it.total }.coerceAtLeast(0.01)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinancialCard)
            .padding(16.dp)
    ) {
        Text(
            text = "Category Breakdown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = FinancialTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Animated segmented bar — 2dp gaps between segments, each pill-shaped (matches mockup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            breakdown.forEach { item ->
                val targetFraction = (item.total / total).toFloat().coerceIn(0.01f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "catFraction_${item.category.name}"
                )
                val (_, color) = resolveCategory(item.category.name, userCategories)
                val clickModifier = if (item.category == FinancialCategory.SUBSCRIPTION)
                    Modifier.clickable { onSubscriptionClick() } else Modifier

                Box(
                    modifier = clickModifier
                        .weight(animatedFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                )
            }
        }

        // Inline labels: dot + "Category XX%" on one line (matches mockup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            breakdown.forEach { item ->
                val (displayName, color) = resolveCategory(item.category.name, userCategories)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = "$displayName ${"%.0f".format(item.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = FinancialTextSecondary
                    )
                }
            }
        }
    }
}
