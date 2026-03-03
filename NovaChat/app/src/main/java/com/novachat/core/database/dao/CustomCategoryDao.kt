package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.novachat.core.database.entity.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAll(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    suspend fun getAllOnce(): List<CustomCategoryEntity>

    @Insert
    suspend fun insert(category: CustomCategoryEntity): Long

    @Query("UPDATE custom_categories SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
