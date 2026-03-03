package com.novachat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novachat.core.theme.LocalChatColors
import com.novachat.core.theme.LocalChatShapes
import com.novachat.core.theme.sentBubbleGradient
import com.novachat.core.sms.ScamAnalysis
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val emojiRegex = Regex("^[\\p{So}\\p{Cn}\\p{Cs}\\s\\uFE0F\\u200D\\u20E3\\u2600-\\u27BF\\U0001F000-\\U0001FFFF]{1,10}$")

fun isEmojiOnly(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > 20) return false
    val codePoints = trimmed.codePoints().toArray()
    if (codePoints.size > 6) return false
    return codePoints.all { cp ->
        Character.getType(cp) == Character.OTHER_SYMBOL.toInt() ||
        Character.getType(cp) == Character.SURROGATE.toInt() ||
        Character.getType(cp) == Character.FORMAT.toInt() ||
        Character.getType(cp) == Character.NON_SPACING_MARK.toInt() ||
        Character.getType(cp) == Character.UNASSIGNED.toInt() ||
        cp == 0xFE0F || cp == 0x200D || cp == 0x20E3 ||
        cp in 0x2600..0x27BF ||
        cp in 0x1F000..0x1FFFF ||
        cp in 0x2300..0x23FF ||
        cp in 0x2700..0x27BF ||
        cp in 0xFE00..0xFE0F ||
        cp in 0xE0020..0xE007F
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
    isActiveMatch: Boolean = false,
    replyToBody: String? = null,
    scamAnalysis: ScamAnalysis? = null,
    onLongClick: () -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onCodeCopy: (String) -> Unit = {},
    onPhoneNumberClick: (String) -> Unit = {},
    onShortCodeCopy: (String) -> Unit = {},
    onSwipeToReply: () -> Unit = {},
    onDismissScamWarning: () -> Unit = {},
    onConfirmSpam: () -> Unit = {}
) {
    val chatColors = LocalChatColors.current
    val chatShapes = LocalChatShapes.current
    val isSent = message.type == MessageType.SENT
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.80f

    val bubbleColor = if (isSent) chatColors.sentBubble else chatColors.receivedBubble
    val textColor = if (isSent) chatColors.sentText else chatColors.receivedText
    val bubbleShape = if (isSent) chatShapes.sentBubbleShape else chatShapes.receivedBubbleShape

    val bubbleBackground = remember(isSent, bubbleColor) {
        if (isSent) sentBubbleGradient(bubbleColor) else null
    }
    val emojiOnly = remember(message.body) { isEmojiOnly(message.body) }
    val formattedTime = remember(message.timestamp) { formatTime(message.timestamp) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(bubbleShape)
                .then(
                    if (bubbleBackground != null) Modifier.background(bubbleBackground)
                    else Modifier.background(bubbleColor)
                )
                .combinedClickable(
                    onClick = {
                        extractFirstCodeSnippet(message.body)?.let(onCodeCopy)
                    },
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                val replyBody = replyToBody ?: message.replyToBody
                if (replyBody != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(textColor.copy(alpha = 0.06f))
                            .padding(start = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSent) Color.White.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            text = replyBody,
                            color = textColor.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.padding(bottom = 6.dp))
                }

                if (scamAnalysis != null && scamAnalysis.isScam) {
                    ScamWarningBanner(
                        scamAnalysis = scamAnalysis,
                        textColor = textColor,
                        onDismissScamWarning = onDismissScamWarning,
                        onConfirmSpam = onConfirmSpam
                    )
                }

                SelectionContainer {
                    if (emojiOnly) {
                        Text(
                            text = message.body.trim(),
                            fontSize = 42.sp,
                            lineHeight = 50.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    } else if (!highlightText.isNullOrBlank()) {
                        val highlightColor = if (isActiveMatch)
                            Color(0xFFFF9800)
                        else
                            Color(0xFFFFF176)
                        val highlighted = remember(message.body, highlightText, isActiveMatch) {
                            buildHighlightedText(message.body, highlightText, highlightColor)
                        }
                        Text(
                            text = highlighted,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp
                        )
                    } else {
                        val uriHandler = LocalUriHandler.current
                        val formatted = remember(message.body) { parseFormattedText(message.body) }
                        val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = textColor,
                                lineHeight = 22.sp
                            ),
                            onTextLayout = { layoutResult.value = it },
                            modifier = Modifier.pointerInput(formatted) {
                                detectTapGestures { pos ->
                                    layoutResult.value?.let { layout ->
                                        val offset = layout.getOffsetForPosition(pos)
                                        formatted.getStringAnnotations("SHORT_CODE", offset, offset)
                                            .firstOrNull()
                                            ?.let { onShortCodeCopy(it.item); return@detectTapGestures }
                                        formatted.getStringAnnotations("PHONE", offset, offset)
                                            .firstOrNull()
                                            ?.let { onPhoneNumberClick(it.item); return@detectTapGestures }
                                        formatted.getStringAnnotations("URL", offset, offset)
                                            .firstOrNull()
                                            ?.let { uriHandler.openUri(it.item) }
                                    }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(if (isSent) Alignment.End else Alignment.Start)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = formattedTime,
                        color = textColor.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isSent) {
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            modifier = Modifier.size(14.dp),
                            tint = if (message.isRead) Color(0xFF55EFC4)
                            else textColor.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            val grouped = remember(message.reactions) { message.reactions.groupBy { it.emoji } }
            FlowRow(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 2.dp)
                    .widthIn(max = maxBubbleWidth),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (emoji, reactions) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        shadowElevation = 1.dp,
                        onClick = { onReactionClick(emoji) },
                        modifier = Modifier.padding(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 14.sp)
                            if (reactions.size > 1) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${reactions.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScamWarningBanner(
    scamAnalysis: ScamAnalysis,
    textColor: Color,
    onDismissScamWarning: () -> Unit,
    onConfirmSpam: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFF6B6B).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\u26A0\uFE0F", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scamAnalysis.reason ?: "Suspicious message",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF6B6B)
                    )
                    Text(
                        text = "${(scamAnalysis.confidence * 100).toInt()}% confidence" +
                            (scamAnalysis.category?.let { " \u2022 ${it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }}" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF6B6B).copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismissScamWarning,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        "Not spam",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onConfirmSpam,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        "Report spam",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

fun parseFormattedText(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("***", i) || text.startsWith("___", i) -> {
                val delim = text.substring(i, i + 3)
                val end = text.indexOf(delim, i + 3)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    builder.append(text.substring(i + 3, end))
                    builder.pop()
                    i = end + 3
                } else { builder.append(text[i]); i++ }
            }
            text.startsWith("**", i) || text.startsWith("__", i) -> {
                val delim = text.substring(i, i + 2)
                val end = text.indexOf(delim, i + 2)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(text.substring(i + 2, end))
                    builder.pop()
                    i = end + 2
                } else { builder.append(text[i]); i++ }
            }
            text[i] == '*' || text[i] == '_' -> {
                val delim = text[i].toString()
                val end = text.indexOf(delim, i + 1)
                if (end != -1 && end > i + 1) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else { builder.append(text[i]); i++ }
            }
            text[i] == '~' && text.getOrNull(i + 1) == '~' -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    builder.append(text.substring(i + 2, end))
                    builder.pop()
                    i = end + 2
                } else { builder.append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x15808080),
                        fontSize = 13.sp
                    ))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else { builder.append(text[i]); i++ }
            }
            else -> { builder.append(text[i]); i++ }
        }
    }

    val resultText = builder.toAnnotatedString().text

    // Find links first so phone detection can skip URL ranges.
    val urlRanges = mutableListOf<IntRange>()
    val urlMatcher = android.util.Patterns.WEB_URL.matcher(resultText)
    while (urlMatcher.find()) {
        val start = urlMatcher.start()
        val end = urlMatcher.end()
        urlRanges += start until end
        val url = urlMatcher.group() ?: continue
        val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url

        builder.addStyle(
            style = SpanStyle(
                color = Color(0xFF64B5F6),
                textDecoration = TextDecoration.Underline
            ),
            start = start,
            end = end
        )
        builder.addStringAnnotation(
            tag = "URL",
            annotation = fullUrl,
            start = start,
            end = end
        )
    }

    // Find short codes (4-6 digits) — tapping copies to clipboard, not a phone action.
    val shortCodeRegex = Regex("\\b\\d{4,6}\\b")
    shortCodeRegex.findAll(resultText).forEach { matchResult ->
        val start = matchResult.range.first
        val end = matchResult.range.last + 1
        val code = matchResult.value
        val overlapUrl = urlRanges.any { start < it.last + 1 && end > it.first }
        if (overlapUrl) return@forEach

        builder.addStyle(
            style = SpanStyle(
                color = Color(0xFFFFB74D),
                fontWeight = FontWeight.Medium
            ),
            start = start,
            end = end
        )
        builder.addStringAnnotation(
            tag = "SHORT_CODE",
            annotation = code,
            start = start,
            end = end
        )
    }

    // Find likely phone numbers like +1 555-123-4567, (555) 123 4567, etc.
    val phoneRegex = Regex("(?<!\\w)(?:\\+?\\d[\\d()\\s.-]{7,}\\d)")
    phoneRegex.findAll(resultText).forEach { matchResult ->
        val start = matchResult.range.first
        val end = matchResult.range.last + 1
        val overlapUrl = urlRanges.any { start < it.last + 1 && end > it.first }
        if (overlapUrl) return@forEach

        val normalized = normalizePhoneNumber(matchResult.value)
        if (normalized.length < 7) return@forEach

        builder.addStyle(
            style = SpanStyle(
                color = Color(0xFF81C784),
                textDecoration = TextDecoration.Underline
            ),
            start = start,
            end = end
        )
        builder.addStringAnnotation(
            tag = "PHONE",
            annotation = normalized,
            start = start,
            end = end
        )
    }

    return builder.toAnnotatedString()
}

private fun buildHighlightedText(
    text: String,
    highlight: String,
    highlightColor: Color
) = buildAnnotatedString {
    val lowerText = text.lowercase()
    val lowerHighlight = highlight.lowercase()
    var start = 0
    while (start < text.length) {
        val index = lowerText.indexOf(lowerHighlight, start)
        if (index == -1) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, index))
        pushStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.SemiBold))
        append(text.substring(index, index + highlight.length))
        pop()
        start = index + highlight.length
    }
}

private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("h:mm a", Locale.getDefault())
}

private fun formatTime(timestamp: Long): String {
    return timeFormat.get()!!.format(Date(timestamp))
}

private fun extractFirstCodeSnippet(text: String): String? {
    val fenced = Regex("```(?:\\w+\\n)?([\\s\\S]*?)```").find(text)
    if (fenced != null) {
        return fenced.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }
    val inline = Regex("`([^`\\n]+)`").find(text)
    return inline?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
}

private fun normalizePhoneNumber(raw: String): String {
    val trimmed = raw.trim()
    val leadingPlus = trimmed.startsWith("+")
    val digitsOnly = trimmed.filter { it.isDigit() }
    return if (leadingPlus) "+$digitsOnly" else digitsOnly
}
