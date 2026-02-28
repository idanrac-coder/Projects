package com.novachat.data.repository

import com.novachat.core.database.dao.BlockRuleDao
import com.novachat.core.database.entity.BlockRuleEntity
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.repository.BlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepositoryImpl @Inject constructor(
    private val blockRuleDao: BlockRuleDao
) : BlockRepository {

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
        return blockRuleDao.insertRule(BlockRuleEntity.fromDomainModel(rule))
    }

    override suspend fun deleteRule(id: Long) {
        blockRuleDao.deleteRuleById(id)
    }

    override suspend fun isBlocked(address: String, senderName: String?, body: String): BlockRule? {
        val rules = blockRuleDao.getAllRules().first()
        for (entity in rules) {
            val rule = entity.toDomainModel()
            when (rule.type) {
                BlockType.NUMBER -> {
                    if (matchesPattern(address, rule.value, rule.isRegex)) return rule
                }
                BlockType.KEYWORD -> {
                    if (matchesPattern(body, rule.value, rule.isRegex)) return rule
                }
                BlockType.SENDER_NAME -> {
                    if (senderName != null && matchesPattern(senderName, rule.value, rule.isRegex)) return rule
                }
            }
        }
        return null
    }

    private fun matchesPattern(input: String, pattern: String, isRegex: Boolean): Boolean {
        return if (isRegex) {
            try {
                Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(input)
            } catch (e: Exception) {
                false
            }
        } else {
            val wildcardPattern = pattern.replace("*", ".*")
            input.contains(wildcardPattern, ignoreCase = true) ||
                Regex(wildcardPattern, RegexOption.IGNORE_CASE).matches(input)
        }
    }
}
