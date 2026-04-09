package com.novachat.ui.financial.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.domain.model.SubscriptionInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AVATAR_COLORS = listOf(
    Color(0xFF6C63FF), Color(0xFF26A69A), Color(0xFFFF7043),
    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFEF5350)
)

@Composable
fun SubscriptionItem(
    subscription: SubscriptionInfo,
    modifier: Modifier = Modifier
) {
    val firstLetter = subscription.merchantName.first().uppercase()
    val avatarColor = AVATAR_COLORS[firstLetter.hashCode().mod(AVATAR_COLORS.size).let { if (it < 0) it + AVATAR_COLORS.size else it }]
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(subscription.lastCharged))
    val hasPriceIncrease = subscription.previousAmount != null && subscription.amount > subscription.previousAmount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstLetter,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subscription.merchantName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildString {
                    append(subscription.frequency.lowercase().replaceFirstChar { it.uppercase() })
                    if (subscription.cardLast4 != null) {
                        append(" · *${subscription.cardLast4}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Last charged: $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${currencySymbol(subscription.currency)}${"%.2f".format(subscription.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (hasPriceIncrease) {
                val delta = subscription.amount - subscription.previousAmount!!
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "↑ +${currencySymbol(subscription.currency)}${"%.2f".format(delta)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else if (subscription.previousAmount != null && subscription.amount < subscription.previousAmount) {
                val delta = subscription.previousAmount - subscription.amount
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "↓ -${currencySymbol(subscription.currency)}${"%.2f".format(delta)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
