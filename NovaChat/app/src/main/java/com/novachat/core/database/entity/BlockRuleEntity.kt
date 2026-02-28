package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType

@Entity(tableName = "block_rules")
data class BlockRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val value: String,
    val isRegex: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel() = BlockRule(
        id = id,
        type = BlockType.valueOf(type),
        value = value,
        isRegex = isRegex,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(rule: BlockRule) = BlockRuleEntity(
            id = rule.id,
            type = rule.type.name,
            value = rule.value,
            isRegex = rule.isRegex,
            createdAt = rule.createdAt
        )
    }
}
