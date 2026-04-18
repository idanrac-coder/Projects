package com.novachat.ui.financial.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.R
import com.novachat.domain.model.TopMerchant
import com.novachat.ui.financial.FinancialCard
import com.novachat.ui.financial.FinancialTextPrimary
import com.novachat.ui.financial.FinancialTextSecondary
import kotlinx.coroutines.delay

private val MERCHANT_AVATAR_COLORS = listOf(
    Color(0xFF6C5CE7), Color(0xFF00B894), Color(0xFFFF7043),
    Color(0xFF42A5F5), Color(0xFFAB47BC)
)

@Composable
fun TopMerchantsCard(
    merchants: List<TopMerchant>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinancialCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.top_merchants),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = FinancialTextPrimary
        )

        merchants.forEachIndexed { index, merchant ->
            var visible by remember(merchant.merchantName) { mutableStateOf(false) }
            LaunchedEffect(merchant.merchantName) {
                delay(index * 80L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 2 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinancialTextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    val firstLetter = merchant.merchantName.first().uppercase()
                    val avatarColor = MERCHANT_AVATAR_COLORS[index % MERCHANT_AVATAR_COLORS.size]
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstLetter,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = merchant.merchantName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = FinancialTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${merchant.transactionCount} ${if (merchant.transactionCount != 1) stringResource(R.string.transactions_plural) else stringResource(R.string.transaction_singular)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinancialTextSecondary
                        )
                    }

                    Text(
                        text = "${currencySymbol(currency)}${"%.2f".format(merchant.totalSpent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = FinancialTextPrimary
                    )
                }
            }
        }
    }
}
