package com.novachat.domain.repository

import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    suspend fun getThemeByBubbleShape(shape: BubbleShape): NovaChatTheme?

    fun getAllThemes(): Flow<List<NovaChatTheme>>
    fun getBuiltInThemes(): Flow<List<NovaChatTheme>>
    fun getCustomThemes(): Flow<List<NovaChatTheme>>
    suspend fun getThemeById(id: Long): NovaChatTheme?
    suspend fun saveTheme(theme: NovaChatTheme): Long
    suspend fun deleteCustomTheme(id: Long)
    suspend fun seedBuiltInThemes()
}
