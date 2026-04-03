package com.novachat.ui.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamDetailSheet(
    address: String,
    body: String,
    score: Int?,
    matchedRuleType: String,
    timestamp: Long,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isRuleBasedBlock = matchedRuleType in setOf(
        "NUMBER", "SENDER_NAME", "KEYWORD", "LANGUAGE",
        "INTERNATIONAL_FILTER", "SCAM_REPORT"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Why was this flagged?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "From: $address",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRuleBasedBlock && score != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spam Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$score / 100",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                score > 75 -> MaterialTheme.colorScheme.error
                                score >= 55 -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        LinearProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                score > 75 -> MaterialTheme.colorScheme.error
                                score >= 55 -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = "Reason",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val reasons = buildReasons(matchedRuleType)
            reasons.forEach { reason ->
                ReasonRow(
                    icon = reason.icon,
                    label = reason.label,
                    detail = reason.detail,
                    iconTint = reason.color
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Message Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReasonRow(
    icon: ImageVector,
    label: String,
    detail: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconTint)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ReasonInfo(
    val icon: ImageVector,
    val label: String,
    val detail: String,
    val color: Color
)

private fun buildReasons(ruleType: String): List<ReasonInfo> {
    val stripped = ruleType.replace(Regex("\\|SCORE_\\d+$"), "")

    return when {
        stripped == "NUMBER" -> listOf(
            ReasonInfo(
                Icons.Default.Block, "Number blocked",
                "This number is on your block list.",
                Color(0xFFE53935)
            )
        )
        stripped == "SENDER_NAME" -> listOf(
            ReasonInfo(
                Icons.Default.Person, "Sender blocked",
                "This sender name is on your block list.",
                Color(0xFFE53935)
            )
        )
        stripped == "KEYWORD" -> listOf(
            ReasonInfo(
                Icons.Default.TextFields, "Keyword match",
                "The message contains a word or phrase you've blocked.",
                Color(0xFFE53935)
            )
        )
        stripped == "LANGUAGE" -> listOf(
            ReasonInfo(
                Icons.Default.Language, "Language filtered",
                "The message is in a language you've chosen to block.",
                Color(0xFFFF9800)
            )
        )
        stripped == "INTERNATIONAL_FILTER" -> listOf(
            ReasonInfo(
                Icons.Default.Public, "International number",
                "This message came from a foreign number and your international filter is on.",
                Color(0xFFFF9800)
            )
        )
        stripped == "SCAM_REPORT" -> listOf(
            ReasonInfo(
                Icons.Default.Report, "Reported as spam",
                "You or other users reported this number as spam.",
                Color(0xFFE53935)
            )
        )
        stripped.startsWith("SCAM:") -> {
            val category = stripped.removePrefix("SCAM:")
            listOf(
                ReasonInfo(
                    Icons.Default.Shield, "Scam detected",
                    formatScamCategory(category),
                    Color(0xFFE53935)
                )
            )
        }
        stripped.startsWith("SPAM_AGENT:") -> listOf(
            ReasonInfo(
                Icons.Default.Psychology, "AI flagged",
                stripped.removePrefix("SPAM_AGENT:").replace('_', ' ')
                    .lowercase().replaceFirstChar { it.uppercase() } + ".",
                Color(0xFF1E88E5)
            )
        )
        stripped.startsWith("SPAM_FILTER:") -> buildSpamFilterReasons(stripped.removePrefix("SPAM_FILTER:"))
        stripped.startsWith("INBOX_SCAN:") -> buildSpamFilterReasons(stripped.removePrefix("INBOX_SCAN:"))
        else -> listOf(
            ReasonInfo(
                Icons.Default.Shield, "Blocked",
                stripped.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + ".",
                Color(0xFF757575)
            )
        )
    }
}

private fun buildSpamFilterReasons(inner: String): List<ReasonInfo> {
    val cleaned = inner.replace(Regex("\\|SCORE_\\d+$"), "")

    return when {
        cleaned.startsWith("DET:") -> {
            val det = cleaned.removePrefix("DET:")
            listOf(
                ReasonInfo(
                    Icons.Default.Shield, "Suspicious content",
                    formatDetReason(det),
                    Color(0xFFFF9800)
                )
            )
        }
        cleaned.startsWith("HEUR:") -> {
            val parts = cleaned.removePrefix("HEUR:").split("+")
            parts.map { part ->
                val key = if ("=" in part) part.substringBefore("=") else part
                heuristicToReason(key)
            }
        }
        cleaned.startsWith("HEBREW:") -> {
            val reason = cleaned.removePrefix("HEBREW:")
            listOf(
                ReasonInfo(
                    Icons.Default.TextFields, "Content analysis",
                    formatHebrewReason(reason),
                    Color(0xFF1E88E5)
                )
            )
        }
        cleaned.startsWith("SCORE_") -> listOf(
            ReasonInfo(
                Icons.Default.Psychology, "Spam detected",
                "Our filters flagged this message as spam.",
                Color(0xFF1E88E5)
            )
        )
        else -> listOf(
            ReasonInfo(
                Icons.Default.Psychology, "Spam detected",
                "Our filters flagged this message as spam.",
                Color(0xFF1E88E5)
            )
        )
    }
}

private fun heuristicToReason(key: String): ReasonInfo = when (key) {
    "sender_unknown" -> ReasonInfo(
        Icons.Default.Person, "Unknown sender",
        "The sender is not in your contacts.",
        Color(0xFFFF9800)
    )
    "contains_url" -> ReasonInfo(
        Icons.Default.Link, "Contains a link",
        "The message includes a URL, which is common in spam.",
        Color(0xFFE53935)
    )
    "otp_verify" -> ReasonInfo(
        Icons.Default.TextFields, "Fake verification code",
        "The message looks like a fraudulent verification request.",
        Color(0xFFE53935)
    )
    "high_special_chars" -> ReasonInfo(
        Icons.Default.TextFields, "Unusual formatting",
        "The message has an unusual amount of special characters.",
        Color(0xFF7B1FA2)
    )
    "deterministic" -> ReasonInfo(
        Icons.Default.Shield, "Known spam pattern",
        "The message matches a known spam template.",
        Color(0xFFFF9800)
    )
    else -> ReasonInfo(
        Icons.Default.Shield, key.replace('_', ' ').replaceFirstChar { it.uppercase() },
        "Flagged by our spam filters.",
        Color(0xFF757575)
    )
}

private fun formatDetReason(det: String): String = when (det) {
    "SHORTENED_URL" -> "The message contains a shortened link that could hide a malicious destination."
    "SUSPICIOUS_TLD" -> "The message links to a website with a suspicious domain."
    "IP_URL" -> "The message contains a direct IP address link, often used in phishing."
    "URGENT_KEYWORDS" -> "The message uses urgency tactics commonly found in scam messages."
    "OTP_VERIFY_EN", "OTP_VERIFY_HE" -> "The message looks like a fraudulent verification request."
    else -> "The message contains content commonly found in spam."
}

private fun formatScamCategory(category: String): String = when (category.uppercase()) {
    "SUSPICIOUS_LINK" -> "The message contains a link that appears unsafe."
    "OTP_FRAUD" -> "The message looks like a fake verification or login code."
    "FINANCIAL_SCAM" -> "The message appears to be a financial scam."
    "PHISHING" -> "The message is trying to steal personal information."
    "UNKNOWN" -> "The message matches known scam patterns."
    else -> category.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + " detected."
}

private fun formatHebrewReason(reason: String): String {
    val readable = reason.replace('_', ' ').lowercase()
    return readable.replaceFirstChar { it.uppercase() } + "."
}

/**
 * Extracts a 0-100 spam score embedded in [matchedRuleType].
 * Looks for `|SCORE_nn` suffix first, then any `SCORE_nn` token.
 * Returns null when no score is stored (rule-based blocks).
 */
fun parseSpamScoreFromMatchedRuleType(matchedRuleType: String): Int? {
    Regex("\\|SCORE_(\\d+)$").find(matchedRuleType)?.groupValues?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return it.coerceIn(0, 100) }
    val matches = Regex("SCORE_(\\d+)").findAll(matchedRuleType).map { it.groupValues[1] }.toList()
    return matches.lastOrNull()?.toIntOrNull()?.coerceIn(0, 100)
}
