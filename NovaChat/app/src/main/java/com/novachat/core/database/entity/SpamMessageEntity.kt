package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_messages")
data class SpamMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val smsId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val matchedRuleId: Long,
    val matchedRuleType: String
)
