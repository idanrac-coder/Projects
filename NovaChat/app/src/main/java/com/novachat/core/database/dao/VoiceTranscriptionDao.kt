package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.VoiceTranscriptionEntity

@Dao
interface VoiceTranscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(entity: VoiceTranscriptionEntity)

    @Query("SELECT * FROM voice_transcriptions WHERE messageId = :messageId")
    suspend fun getTranscription(messageId: Long): VoiceTranscriptionEntity?
}
