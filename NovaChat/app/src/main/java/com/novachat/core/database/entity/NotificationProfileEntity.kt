package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_profiles")
data class NotificationProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val ledColor: Int? = null,
    val priority: String = "HIGH",
    val popupEnabled: Boolean = true,
    val isActive: Boolean = false,
    val scheduleStartHour: Int? = null,
    val scheduleStartMinute: Int? = null,
    val scheduleEndHour: Int? = null,
    val scheduleEndMinute: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
