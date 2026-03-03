package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_learning")
data class SpamLearningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val body: String,
    val isSpam: Boolean,
    val detectedCategory: String?,
    val userFeedback: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "spam_sender_reputation")
data class SpamSenderReputationEntity(
    @PrimaryKey
    val address: String,
    val spamCount: Int = 0,
    val hamCount: Int = 0,
    val lastSpamTimestamp: Long = 0,
    val lastHamTimestamp: Long = 0
)

@Entity(tableName = "spam_keyword_weights")
data class SpamKeywordWeightEntity(
    @PrimaryKey
    val keyword: String,
    val spamOccurrences: Int = 0,
    val hamOccurrences: Int = 0,
    val weight: Float = 0f
)
