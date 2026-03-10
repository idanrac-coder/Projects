package com.novachat.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.novachat.core.database.dao.BlockRuleDao
import com.novachat.core.database.dao.ConversationMetaDao
import com.novachat.core.database.dao.CustomCategoryDao
import com.novachat.core.database.dao.MessageReactionDao
import com.novachat.core.database.dao.MessageReminderDao
import com.novachat.core.database.dao.NotificationProfileDao
import com.novachat.core.database.dao.PinnedMessageDao
import com.novachat.core.database.dao.ScheduledMessageDao
import com.novachat.core.database.dao.ShortCodeWhitelistDao
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.dao.ThemeDao
import com.novachat.core.database.entity.BlockRuleEntity
import com.novachat.core.database.entity.ConversationMetaEntity
import com.novachat.core.database.entity.CustomCategoryEntity
import com.novachat.core.database.entity.MessageReactionEntity
import com.novachat.core.database.entity.MessageReminderEntity
import com.novachat.core.database.entity.NotificationProfileEntity
import com.novachat.core.database.entity.PinnedMessageEntity
import com.novachat.core.database.entity.ScheduledMessageEntity
import com.novachat.core.database.entity.SenderAllowlistEntity
import com.novachat.core.database.entity.ShortCodeWhitelistEntity
import com.novachat.core.database.entity.SpamKeywordWeightEntity
import com.novachat.core.database.entity.SpamLearningEntity
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.database.entity.SpamSenderReputationEntity
import com.novachat.core.database.entity.ThemeEntity

@Database(
    entities = [
        BlockRuleEntity::class,
        ThemeEntity::class,
        SpamMessageEntity::class,
        ConversationMetaEntity::class,
        ScheduledMessageEntity::class,
        MessageReactionEntity::class,
        MessageReminderEntity::class,
        PinnedMessageEntity::class,
        CustomCategoryEntity::class,
        NotificationProfileEntity::class,
        SpamLearningEntity::class,
        SpamSenderReputationEntity::class,
        SpamKeywordWeightEntity::class,
        SenderAllowlistEntity::class,
        ShortCodeWhitelistEntity::class
    ],
    version = 12,
    exportSchema = true
)
abstract class NovaChatDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun themeDao(): ThemeDao
    abstract fun spamMessageDao(): SpamMessageDao
    abstract fun spamLearningDao(): SpamLearningDao
    abstract fun conversationMetaDao(): ConversationMetaDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun messageReminderDao(): MessageReminderDao
    abstract fun pinnedMessageDao(): PinnedMessageDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun notificationProfileDao(): NotificationProfileDao
    abstract fun shortCodeWhitelistDao(): ShortCodeWhitelistDao
}
