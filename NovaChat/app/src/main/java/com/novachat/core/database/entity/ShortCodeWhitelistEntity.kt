package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "short_code_whitelist")
data class ShortCodeWhitelistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
