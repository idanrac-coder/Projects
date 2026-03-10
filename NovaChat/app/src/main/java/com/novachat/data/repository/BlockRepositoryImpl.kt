package com.novachat.data.repository

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.novachat.core.database.dao.BlockRuleDao
import com.novachat.core.database.entity.BlockRuleEntity
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.BlockRuleLimitException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepositoryImpl @Inject constructor(
    private val blockRuleDao: BlockRuleDao,
    private val preferencesRepository: UserPreferencesRepository
) : BlockRepository {

    private val languageIdentifier = LanguageIdentification.getClient()

    override fun getAllRules(): Flow<List<BlockRule>> {
        return blockRuleDao.getAllRules().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRulesByType(type: BlockType): Flow<List<BlockRule>> {
        return blockRuleDao.getRulesByType(type.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRuleCount(): Flow<Int> {
        return blockRuleDao.getRuleCount()
    }

    override suspend fun addRule(rule: BlockRule): Long {
        val trimmedValue = rule.value.trim()
        val existing = blockRuleDao.getRuleByTypeAndValue(rule.type.name, trimmedValue)
        if (existing != null) return existing.id

        val count = blockRuleDao.getRuleCount().first()
        val isPremium = preferencesRepository.isPremium.first()
        if (!isPremium && count >= BlockRepository.FREE_RULE_LIMIT) {
            throw BlockRuleLimitException()
        }
        return blockRuleDao.insertRule(BlockRuleEntity.fromDomainModel(rule.copy(value = trimmedValue)))
    }

    override suspend fun deleteRule(id: Long) {
        blockRuleDao.deleteRuleById(id)
    }

    override suspend fun isBlocked(address: String, senderName: String?, body: String): BlockRule? {
        val rules = blockRuleDao.getAllRules().first()
        if (rules.isEmpty()) return null
        
        var detectedLanguage: String? = null
        val languageRules = rules.filter { it.type == BlockType.LANGUAGE.name }
        if (languageRules.isNotEmpty() && body.isNotBlank()) {
            try {
                val languageCode = languageIdentifier.identifyLanguage(body).await()
                if (languageCode != "und") {
                    detectedLanguage = languageCode
                }
            } catch (e: Exception) {
                // Ignore language detection errors
            }
        }

        for (entity in rules) {
            val rule = entity.toDomainModel()
            when (rule.type) {
                BlockType.NUMBER -> {
                    if (matchesPattern(address, rule.value, rule.isRegex, exactMatch = true)) return rule
                }
                BlockType.KEYWORD -> {
                    if (matchesPattern(body, rule.value, rule.isRegex)) return rule
                }
                BlockType.SENDER_NAME -> {
                    val nameToMatch = senderName ?: address
                    if (matchesPattern(nameToMatch, rule.value, rule.isRegex, exactMatch = true)) return rule
                }
                BlockType.LANGUAGE -> {
                    if (detectedLanguage != null && rule.value.equals(detectedLanguage, ignoreCase = true)) return rule
                }
            }
        }
        return null
    }

    private fun matchesPattern(input: String, pattern: String, isRegex: Boolean, exactMatch: Boolean = false): Boolean {
        return if (isRegex) {
            try {
                if (exactMatch) {
                    Regex(pattern, RegexOption.IGNORE_CASE).matches(input)
                } else {
                    Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(input)
                }
            } catch (e: Exception) {
                false
            }
        } else {
            if (exactMatch) {
                if (pattern.contains("*")) {
                    val regexPattern = Regex.escape(pattern).replace("\\*", ".*")
                    try {
                        Regex("^$regexPattern\$", RegexOption.IGNORE_CASE).matches(input)
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    input.equals(pattern, ignoreCase = true)
                }
            } else {
                if (pattern.contains("*")) {
                    val regexPattern = Regex.escape(pattern).replace("\\*", ".*")
                    try {
                        Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(input)
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    input.contains(pattern, ignoreCase = true)
                }
            }
        }
    }
}
