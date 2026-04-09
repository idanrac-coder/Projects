package com.novachat.core.database.financial.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_transactions",
    indices = [
        Index("sender"),
        Index("category"),
        Index("timestamp"),
        Index("merchantName"),
        Index(value = ["smsId"], unique = true),
        Index("cardLast4")
    ]
)
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "smsId") val smsId: Long,
    val sender: String,
    val merchantName: String? = null,
    val amount: Double,
    val currency: String = "ILS",
    val category: String,
    val timestamp: Long,
    val smsTimestamp: Long,
    val isRecurring: Boolean = false,
    val confidenceScore: Float = 0f,
    val rawBody: String,
    val cardLast4: String? = null,
    val isConversion: Boolean = false
)
