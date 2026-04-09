package com.novachat.core.database.financial.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_alerts")
data class FinancialAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val transactionId: Long,
    val message: String,
    val timestamp: Long,
    val isDismissed: Boolean = false
)
