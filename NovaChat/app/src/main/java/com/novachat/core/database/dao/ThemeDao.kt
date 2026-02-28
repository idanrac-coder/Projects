package com.novachat.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novachat.core.database.entity.ThemeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {

    @Query("SELECT * FROM themes ORDER BY isBuiltIn DESC, name ASC")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE isBuiltIn = 1")
    fun getBuiltInThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE isBuiltIn = 0")
    fun getCustomThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getThemeById(id: Long): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity): Long

    @Update
    suspend fun updateTheme(theme: ThemeEntity)

    @Delete
    suspend fun deleteTheme(theme: ThemeEntity)

    @Query("DELETE FROM themes WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomThemeById(id: Long)
}
