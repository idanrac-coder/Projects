package com.novachat.ui.financial.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.novachat.core.sms.financial.FinancialCategory
import com.novachat.domain.model.TransactionInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AVATAR_COLORS = listOf(
    Color(0xFF6C63FF), Color(0xFF26A69A), Color(0xFFFF7043),
    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFEF5350)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionInfo,
    onCategoryChange: ((String, FinancialCategory) -> Unit)? = null,
    onViewInConversation: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstLetter = (transaction.merchantName ?: "?").first().uppercase()
    val avatarColor = AVATAR_COLORS[firstLetter.hashCode().mod(AVATAR_COLORS.size).let { if (it < 0) it + AVATAR_COLORS.size else it }]
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(transaction.timestamp))

    val categoryColor = try {
        CATEGORY_COLORS[FinancialCategory.valueOf(transaction.category)] ?: Color.Gray
    } catch (_: Exception) { Color.Gray }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    if (showCategoryPicker && onCategoryChange != null) {
        CategoryPickerDialog(
            currentCategory = transaction.category,
            onCategorySelected = { newCat ->
                transaction.merchantName?.let { onCategoryChange(it, newCat) }
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { showContextMenu = true },
                    onLongClick = { showContextMenu = true }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    text = transaction.merchantName ?: stringResource(R.string.unknown_merchant),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = transaction.category.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.cardLast4 != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "*${transaction.cardLast4}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "${currencySymbol(transaction.currency)}${"%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            if (onCategoryChange != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.change_category)) },
                    onClick = {
                        showContextMenu = false
                        showCategoryPicker = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
            if (transaction.senderAddress != null && onViewInConversation != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.view_in_conversation)) },
                    onClick = {
                        showContextMenu = false
                        onViewInConversation(transaction.senderAddress)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}
