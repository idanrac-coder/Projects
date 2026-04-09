package com.novachat.core.database.financial.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchants",
    indices = [Index(value = ["name"], unique = true)]
)
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val averageAmount: Double = 0.0,
    val transactionCount: Int = 0,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long,
    val category: String? = null
)
