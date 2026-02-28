package com.novachat.domain.repository

import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import kotlinx.coroutines.flow.Flow

interface BlockRepository {
    fun getAllRules(): Flow<List<BlockRule>>
    fun getRulesByType(type: BlockType): Flow<List<BlockRule>>
    fun getRuleCount(): Flow<Int>
    suspend fun addRule(rule: BlockRule): Long
    suspend fun deleteRule(id: Long)
    suspend fun isBlocked(address: String, senderName: String?, body: String): BlockRule?
}
