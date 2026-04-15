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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.novachat.R

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
                text = stringResource(R.string.spam_detail_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.spam_detail_from_prefix) + address,
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
                            text = stringResource(R.string.spam_detail_score_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$score " + stringResource(R.string.spam_detail_score_suffix),
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
                text = stringResource(R.string.spam_detail_reason_label),
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
                text = stringResource(R.string.spam_detail_preview_label),
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

@Composable
private fun buildReasons(ruleType: String): List<ReasonInfo> {
    val stripped = ruleType.replace(Regex("\\|SCORE_\\d+$"), "")

    return when {
        stripped == "NUMBER" -> listOf(
            ReasonInfo(
                Icons.Default.Block,
                stringResource(R.string.spam_detail_number_blocked_label),
                stringResource(R.string.spam_detail_number_blocked_description),
                Color(0xFFE53935)
            )
        )
        stripped == "SENDER_NAME" -> listOf(
            ReasonInfo(
                Icons.Default.Person,
                stringResource(R.string.spam_detail_sender_blocked_label),
                stringResource(R.string.spam_detail_sender_blocked_description),
                Color(0xFFE53935)
            )
        )
        stripped == "KEYWORD" -> listOf(
            ReasonInfo(
                Icons.Default.TextFields,
                stringResource(R.string.spam_detail_keyword_match_label),
                stringResource(R.string.spam_detail_keyword_match_description),
                Color(0xFFE53935)
            )
        )
        stripped == "LANGUAGE" -> listOf(
            ReasonInfo(
                Icons.Default.Language,
                stringResource(R.string.spam_detail_language_filtered_label),
                stringResource(R.string.spam_detail_language_filtered_description),
                Color(0xFFFF9800)
            )
        )
        stripped == "INTERNATIONAL_FILTER" -> listOf(
            ReasonInfo(
                Icons.Default.Public,
                stringResource(R.string.spam_detail_international_filter_label),
                stringResource(R.string.spam_detail_international_filter_description),
                Color(0xFFFF9800)
            )
        )
        stripped == "SCAM_REPORT" -> listOf(
            ReasonInfo(
                Icons.Default.Report,
                stringResource(R.string.spam_detail_scam_report_label),
                stringResource(R.string.spam_detail_scam_report_description),
                Color(0xFFE53935)
            )
        )
        stripped.startsWith("SCAM:") -> {
            val category = stripped.removePrefix("SCAM:")
            listOf(
                ReasonInfo(
                    Icons.Default.Shield,
                    stringResource(R.string.spam_detail_scam_detected_label),
                    formatScamCategory(category),
                    Color(0xFFE53935)
                )
            )
        }
        stripped.startsWith("SPAM_AGENT:") -> listOf(
            ReasonInfo(
                Icons.Default.Psychology,
                stringResource(R.string.spam_detail_ai_flagged_label),
                stripped.removePrefix("SPAM_AGENT:").replace('_', ' ')
                    .lowercase().replaceFirstChar { it.uppercase() } + ".",
                Color(0xFF1E88E5)
            )
        )
        stripped.startsWith("SPAM_FILTER:") -> buildSpamFilterReasons(stripped.removePrefix("SPAM_FILTER:"))
        stripped.startsWith("INBOX_SCAN:") -> buildSpamFilterReasons(stripped.removePrefix("INBOX_SCAN:"))
        else -> listOf(
            ReasonInfo(
                Icons.Default.Shield,
                stringResource(R.string.spam_filter_blocked_as_spam),
                stripped.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + ".",
                Color(0xFF757575)
            )
        )
    }
}

@Composable
private fun buildSpamFilterReasons(inner: String): List<ReasonInfo> {
    val cleaned = inner.replace(Regex("\\|SCORE_\\d+$"), "")

    return when {
        cleaned.startsWith("DET:") -> {
            val det = cleaned.removePrefix("DET:")
            listOf(
                ReasonInfo(
                    Icons.Default.Shield,
                    stringResource(R.string.spam_detail_suspicious_content_label),
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
                    Icons.Default.TextFields,
                    stringResource(R.string.spam_detail_suspicious_content_label),
                    formatHebrewReason(reason),
                    Color(0xFF1E88E5)
                )
            )
        }
        cleaned.startsWith("SCORE_") -> listOf(
            ReasonInfo(
                Icons.Default.Psychology,
                stringResource(R.string.spam_filter_spam_detected),
                stringResource(R.string.spam_detail_general_spam_description),
                Color(0xFF1E88E5)
            )
        )
        else -> listOf(
            ReasonInfo(
                Icons.Default.Psychology,
                stringResource(R.string.spam_filter_spam_detected),
                stringResource(R.string.spam_detail_general_spam_description),
                Color(0xFF1E88E5)
            )
        )
    }
}

@Composable
private fun heuristicToReason(key: String): ReasonInfo = when (key) {
    "sender_unknown" -> ReasonInfo(
        Icons.Default.Person,
        stringResource(R.string.spam_reason_unknown_sender_label),
        stringResource(R.string.spam_reason_unknown_sender_description),
        Color(0xFFFF9800)
    )
    "contains_url" -> ReasonInfo(
        Icons.Default.Link,
        stringResource(R.string.spam_reason_contains_url_label),
        stringResource(R.string.spam_reason_contains_url_description),
        Color(0xFFE53935)
    )
    "otp_verify" -> ReasonInfo(
        Icons.Default.TextFields,
        stringResource(R.string.spam_reason_otp_verify_label),
        stringResource(R.string.spam_reason_otp_verify_description),
        Color(0xFFE53935)
    )
    "high_special_chars" -> ReasonInfo(
        Icons.Default.TextFields,
        stringResource(R.string.spam_reason_high_special_chars_label),
        stringResource(R.string.spam_reason_high_special_chars_description),
        Color(0xFF7B1FA2)
    )
    "deterministic" -> ReasonInfo(
        Icons.Default.Shield,
        stringResource(R.string.spam_reason_deterministic_label),
        stringResource(R.string.spam_reason_deterministic_description),
        Color(0xFFFF9800)
    )
    else -> ReasonInfo(
        Icons.Default.Shield,
        key.replace('_', ' ').replaceFirstChar { it.uppercase() },
        stringResource(R.string.spam_reason_default_description),
        Color(0xFF757575)
    )
}

@Composable
private fun formatDetReason(det: String): String = when (det) {
    "SHORTENED_URL" -> stringResource(R.string.spam_detail_shortened_url_description)
    "SUSPICIOUS_TLD" -> stringResource(R.string.spam_detail_suspicious_tld_description)
    "IP_URL" -> stringResource(R.string.spam_detail_ip_url_description)
    "URGENT_KEYWORDS" -> stringResource(R.string.spam_detail_urgent_keywords_description)
    "OTP_VERIFY_EN", "OTP_VERIFY_HE" -> stringResource(R.string.spam_detail_otp_verify_description)
    else -> stringResource(R.string.spam_detail_general_spam_description)
}

@Composable
private fun formatScamCategory(category: String): String = when (category.uppercase()) {
    "SUSPICIOUS_LINK" -> stringResource(R.string.spam_detail_suspicious_link_description)
    "OTP_FRAUD" -> stringResource(R.string.spam_detail_otp_fraud_description)
    "FINANCIAL_SCAM" -> stringResource(R.string.spam_detail_financial_scam_description)
    "PHISHING" -> stringResource(R.string.spam_detail_phishing_description)
    "UNKNOWN" -> stringResource(R.string.spam_detail_unknown_scam_description)
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
