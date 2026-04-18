package com.novachat.core.database.financial.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val last4: String,
    val nickname: String? = null,
    val issuer: String? = null,
    val createdAt: Long,
    val lastSeenTimestamp: Long,
    val transactionCount: Int = 0,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)
