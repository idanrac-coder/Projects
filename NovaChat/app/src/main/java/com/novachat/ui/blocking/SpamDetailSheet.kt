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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Rule
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * "Why was this flagged?" bottom sheet for spam messages.
 * Parses the matchedRuleType string into a human-readable breakdown.
 */
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
                    if (score != null) {
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
                    } else {
                        Text(
                            text = "Not available",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No model score for this entry (blocked by number, keyword, or manual action).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (score != null) {
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Detection Signals",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val signals = parseMatchedRuleType(matchedRuleType)
            signals.forEach { signal ->
                SignalRow(
                    icon = signal.icon,
                    label = signal.label,
                    detail = signal.detail,
                    iconTint = signal.color
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
private fun SignalRow(
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
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class DetectionSignal(
    val icon: ImageVector,
    val label: String,
    val detail: String,
    val color: Color
)

private fun parseMatchedRuleType(ruleType: String): List<DetectionSignal> {
    val signals = mutableListOf<DetectionSignal>()

    when {
        ruleType.startsWith("SCAM:") -> {
            val category = ruleType.removePrefix("SCAM:")
            signals.add(
                DetectionSignal(
                    Icons.Default.BugReport, "Scam Pattern",
                    formatCategory(category), Color(0xFFE53935)
                )
            )
        }
        ruleType.startsWith("DET_RAW:") || ruleType.startsWith("DET:") -> {
            val det = ruleType.removePrefix("DET_RAW:").removePrefix("DET:")
            signals.add(
                DetectionSignal(
                    Icons.Default.Rule, "Pattern Match",
                    formatCategory(det), Color(0xFFFF9800)
                )
            )
        }
        ruleType.startsWith("HEBREW:") -> {
            val reason = ruleType.removePrefix("HEBREW:")
            signals.add(
                DetectionSignal(
                    Icons.Default.TextFields, "Hebrew Analysis",
                    reason.replace('_', ' '), Color(0xFF1E88E5)
                )
            )
        }
        ruleType.startsWith("HEUR:") -> {
            val parts = ruleType.removePrefix("HEUR:").split("+")
            parts.forEach { part ->
                val (key, value) = if ("=" in part) {
                    val s = part.split("=")
                    s[0] to s.getOrElse(1) { "" }
                } else part to ""
                val (icon, label, color) = when (key) {
                    "sender_unknown" -> Triple(Icons.Default.Person, "Unknown Sender", Color(0xFFFF9800))
                    "contains_url" -> Triple(Icons.Default.Link, "Contains URL", Color(0xFFE53935))
                    "otp_verify" -> Triple(Icons.Default.TextFields, "Verification Code", Color(0xFF1E88E5))
                    "high_special_chars" -> Triple(Icons.Default.TextFields, "Suspicious Characters", Color(0xFF7B1FA2))
                    "deterministic" -> Triple(Icons.Default.Rule, "Pattern Match", Color(0xFFFF9800))
                    else -> Triple(Icons.Default.Analytics, key.replace('_', ' '), Color(0xFF757575))
                }
                signals.add(DetectionSignal(icon, label, "+$value points", color))
            }
        }
        ruleType.startsWith("SPAM_FILTER:") -> {
            val inner = ruleType.removePrefix("SPAM_FILTER:").replace(Regex("\\|SCORE_\\d+$"), "")
            signals.add(
                DetectionSignal(
                    Icons.Default.Psychology, "AI Classification",
                    inner, Color(0xFF1E88E5)
                )
            )
        }
        ruleType.startsWith("INBOX_SCAN:") -> {
            val inner = ruleType.removePrefix("INBOX_SCAN:").replace(Regex("\\|SCORE_\\d+$"), "")
            signals.add(
                DetectionSignal(
                    Icons.Default.Psychology, "Inbox scan",
                    inner, Color(0xFF1E88E5)
                )
            )
        }
    }

    if (signals.isEmpty()) {
        signals.add(
            DetectionSignal(
                Icons.Default.Analytics, "Classification",
                ruleType.replace('_', ' '), Color(0xFF757575)
            )
        )
    }

    return signals
}

private fun formatCategory(category: String): String = category
    .replace('_', ' ')
    .lowercase()
    .replaceFirstChar { it.uppercase() }

/**
 * Extracts a 0–100 spam score stored in [matchedRuleType], e.g. `SPAM_FILTER:SCORE_72`,
 * `SPAM_FILTER:SHORTENED_URL|SCORE_72`, or `INBOX_SCAN:HEUR:…|SCORE_50`.
 */
fun parseSpamScoreFromMatchedRuleType(matchedRuleType: String): Int? {
    Regex("\\|SCORE_(\\d+)$").find(matchedRuleType)?.groupValues?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return it.coerceIn(0, 100) }
    val matches = Regex("SCORE_(\\d+)").findAll(matchedRuleType).map { it.groupValues[1] }.toList()
    return matches.lastOrNull()?.toIntOrNull()?.coerceIn(0, 100)
}
