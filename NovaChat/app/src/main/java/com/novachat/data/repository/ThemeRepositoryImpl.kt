package com.novachat.data.repository

import com.novachat.core.database.dao.ThemeDao
import com.novachat.core.database.entity.ThemeEntity
import com.novachat.core.theme.BuiltInThemes
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themeDao: ThemeDao
) : ThemeRepository {

    override fun getAllThemes(): Flow<List<NovaChatTheme>> {
        return themeDao.getAllThemes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getBuiltInThemes(): Flow<List<NovaChatTheme>> {
        return themeDao.getBuiltInThemes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getCustomThemes(): Flow<List<NovaChatTheme>> {
        return themeDao.getCustomThemes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getThemeById(id: Long): NovaChatTheme? {
        return themeDao.getThemeById(id)?.toDomainModel()
    }

    override suspend fun saveTheme(theme: NovaChatTheme): Long {
        return themeDao.insertTheme(ThemeEntity.fromDomainModel(theme))
    }

    override suspend fun deleteCustomTheme(id: Long) {
        themeDao.deleteCustomThemeById(id)
    }

    override suspend fun seedBuiltInThemes() {
        val existing = themeDao.getBuiltInThemes().first()
        if (existing.isEmpty()) {
            BuiltInThemes.all.forEach { theme ->
                themeDao.insertTheme(ThemeEntity.fromDomainModel(theme))
            }
        }
    }
}
