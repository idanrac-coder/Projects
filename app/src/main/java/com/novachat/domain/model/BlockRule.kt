package com.novachat.domain.model

data class BlockRule(
    val id: Long = 0,
    val type: BlockType,
    val value: String,
    val isRegex: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BlockType {
    NUMBER,
    KEYWORD,
    SENDER_NAME
}
