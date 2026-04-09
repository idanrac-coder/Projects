package com.novachat.core.database.financial.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["merchantName", "cardLast4"], unique = true)
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val amount: Double,
    val previousAmount: Double? = null,
    val currency: String,
    val frequency: String,
    val lastSeenTimestamp: Long,
    val firstSeenTimestamp: Long,
    val isActive: Boolean = true,
    val transactionCount: Int = 0,
    val cardLast4: String? = null
)
