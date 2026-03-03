package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
