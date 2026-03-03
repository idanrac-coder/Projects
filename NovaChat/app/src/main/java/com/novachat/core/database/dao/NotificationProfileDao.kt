package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.core.database.entity.NotificationProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationProfileDao {

    @Query("SELECT * FROM notification_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<NotificationProfileEntity>>

    @Query("SELECT * FROM notification_profiles ORDER BY createdAt DESC")
    suspend fun getAllProfilesOnce(): List<NotificationProfileEntity>

    @Query("SELECT * FROM notification_profiles WHERE id = :id")
    suspend fun getProfile(id: Long): NotificationProfileEntity?

    @Query("SELECT * FROM notification_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): NotificationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: NotificationProfileEntity): Long

    @Query("UPDATE notification_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE notification_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("UPDATE notification_profiles SET name = :name, vibrationEnabled = :vibration, priority = :priority, popupEnabled = :popup, scheduleStartHour = :startH, scheduleStartMinute = :startM, scheduleEndHour = :endH, scheduleEndMinute = :endM WHERE id = :id")
    suspend fun update(id: Long, name: String, vibration: Boolean, priority: String, popup: Boolean, startH: Int?, startM: Int?, endH: Int?, endM: Int?)

    @Query("DELETE FROM notification_profiles WHERE id = :id")
    suspend fun delete(id: Long)
}
