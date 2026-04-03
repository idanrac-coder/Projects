package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_transcriptions")
data class VoiceTranscriptionEntity(
    @PrimaryKey
    val messageId: Long,
    val transcription: String,
    val createdAt: Long = System.currentTimeMillis()
)
