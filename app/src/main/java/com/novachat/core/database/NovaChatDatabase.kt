package com.novachat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novachat.core.database.dao.BlockRuleDao
import com.novachat.core.database.dao.ConversationMetaDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.dao.ThemeDao
import com.novachat.core.database.entity.BlockRuleEntity
import com.novachat.core.database.entity.ConversationMetaEntity
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.database.entity.ThemeEntity

@Database(
    entities = [
        BlockRuleEntity::class,
        ThemeEntity::class,
        SpamMessageEntity::class,
        ConversationMetaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NovaChatDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun themeDao(): ThemeDao
    abstract fun spamMessageDao(): SpamMessageDao
    abstract fun conversationMetaDao(): ConversationMetaDao
}
