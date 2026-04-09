package com.novachat.core.database.financial.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_senders",
    indices = [Index(value = ["address"], unique = true)]
)
data class FinancialSenderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val displayName: String? = null,
    val isEnabled: Boolean = true,
    val alertsEnabled: Boolean = true,
    val source: String,
    val createdAt: Long,
    val lastSeenTimestamp: Long,
    val transactionCount: Int = 0
)
