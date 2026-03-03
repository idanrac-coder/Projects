package com.novachat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AiTone(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiComposeSheet(
    sheetState: SheetState,
    currentText: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val tones = remember {
        listOf(
            AiTone("Formal", Icons.Default.WorkOutline, "Professional and polished"),
            AiTone("Casual", Icons.Default.EmojiPeople, "Relaxed and friendly"),
            AiTone("Funny", Icons.Default.SentimentSatisfied, "Humorous and witty"),
            AiTone("Translate", Icons.Default.Translate, "Translate to another language"),
        )
    }

    var result by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "AI Compose Assistant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Transform your message with AI",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (currentText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Your message",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                "Change Tone",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tones.forEach { tone ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        onClick = {
                            result = transformTone(currentText, tone.name)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    tone.icon,
                                    contentDescription = tone.name,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = tone.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = tone.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Or describe what you want to say",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            var prompt by remember { mutableStateOf("") }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Write a birthday wish for my friend") },
                shape = RoundedCornerShape(14.dp),
                maxLines = 3
            )

            if (result.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onApply(result) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Suggestion (tap to use)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "AI features require internet connection. Suggestions are generated locally when possible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun transformTone(text: String, tone: String): String {
    if (text.isBlank()) return "Please enter a message first"
    return when (tone) {
        "Formal" -> "I would like to convey that: $text"
        "Casual" -> "Hey! ${text.lowercase().removeSuffix(".")}, ya know?"
        "Funny" -> "$text \uD83D\uDE04 (but seriously though!)"
        "Translate" -> "[Translation would appear here with ML Kit integration]"
        else -> text
    }
}
