package com.novachat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Reaction(
    val id: Long = 0,
    val messageId: Long,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ReactionEmojis {
    val quickReactions = listOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE21")
    val allReactions = quickReactions + listOf(
        "\uD83D\uDD25", "\uD83C\uDF89", "\uD83D\uDE4F", "\uD83D\uDCAF", "\uD83E\uDD14", "\uD83D\uDC4E"
    )
}
