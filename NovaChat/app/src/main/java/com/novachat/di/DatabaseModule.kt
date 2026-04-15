package com.novachat.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.novachat.core.database.NovaChatDatabase
import com.novachat.core.database.dao.BlockRuleDao
import com.novachat.core.database.dao.ConversationMetaDao
import com.novachat.core.database.dao.CustomCategoryDao
import com.novachat.core.database.dao.MessageReactionDao
import com.novachat.core.database.dao.MessageReminderDao
import com.novachat.core.database.dao.NotificationProfileDao
import com.novachat.core.database.dao.PinnedMessageDao
import com.novachat.core.database.dao.ScheduledMessageDao
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.dao.ShortCodeWhitelistDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.dao.MessageEditDao
import com.novachat.core.database.dao.ThemeDao
import com.novachat.core.database.dao.VoiceTranscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Per-sender mute with duration (replaces boolean isMuted)
        db.execSQL("ALTER TABLE conversation_meta ADD COLUMN muteUntil INTEGER")
        // Migrate existing muted conversations to muted-forever
        db.execSQL("UPDATE conversation_meta SET muteUntil = 9223372036854775807 WHERE isMuted = 1")
        // Inbox filter: Favorites
        db.execSQL("ALTER TABLE conversation_meta ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        // Inbox filter: Locked chats
        db.execSQL("ALTER TABLE conversation_meta ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scan_excluded_messages (
                smsId INTEGER PRIMARY KEY NOT NULL,
                address TEXT NOT NULL,
                bodyHash INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scan_excluded_messages_address ON scan_excluded_messages(address)")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS voice_transcriptions (
                messageId INTEGER PRIMARY KEY NOT NULL,
                transcription TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_spam_messages_smsId ON spam_messages(smsId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_spam_messages_address ON spam_messages(address)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_spam_messages_matchedRuleId ON spam_messages(matchedRuleId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_message_reactions_messageId ON message_reactions(messageId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pinned_messages_threadId ON pinned_messages(threadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_messages_isSent_isFailed_scheduledTime ON scheduled_messages(isSent, isFailed, scheduledTime)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_block_rules_type ON block_rules(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_block_rules_type_value ON block_rules(type, value)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_meta_isPinned ON conversation_meta(isPinned)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_meta_isArchived ON conversation_meta(isArchived)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_meta_isMuted ON conversation_meta(isMuted)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_message_reminders_isTriggered_reminderTime ON message_reminders(isTriggered, reminderTime)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_short_code_whitelist_address ON short_code_whitelist(address)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_spam_learning_isSpam ON spam_learning(isSpam)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_spam_learning_timestamp ON spam_learning(timestamp)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scheduled_messages ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE scheduled_messages ADD COLUMN isFailed INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS message_edits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                messageId INTEGER NOT NULL,
                previousBody TEXT NOT NULL,
                newBody TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_message_edits_messageId ON message_edits(messageId)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE _dedup_keep AS
            SELECT type, value, MIN(id) as keep_id FROM block_rules GROUP BY type, value
        """.trimIndent())
        db.execSQL("""
            UPDATE spam_messages SET matchedRuleId = (
                SELECT k.keep_id FROM _dedup_keep k
                INNER JOIN block_rules r ON r.id = spam_messages.matchedRuleId
                    AND r.type = k.type AND r.value = k.value
            )
            WHERE matchedRuleId IN (
                SELECT r.id FROM block_rules r
                INNER JOIN _dedup_keep k ON r.type = k.type AND r.value = k.value AND r.id != k.keep_id
            )
        """.trimIndent())
        db.execSQL("DELETE FROM block_rules WHERE id NOT IN (SELECT keep_id FROM _dedup_keep)")
        db.execSQL("DROP TABLE _dedup_keep")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS short_code_whitelist (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                address TEXT NOT NULL,
                label TEXT,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sender_allowlist (
                address TEXT PRIMARY KEY NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS spam_learning (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                address TEXT NOT NULL,
                body TEXT NOT NULL,
                isSpam INTEGER NOT NULL,
                detectedCategory TEXT,
                userFeedback TEXT NOT NULL,
                confidence REAL NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS spam_sender_reputation (
                address TEXT PRIMARY KEY NOT NULL,
                spamCount INTEGER NOT NULL DEFAULT 0,
                hamCount INTEGER NOT NULL DEFAULT 0,
                lastSpamTimestamp INTEGER NOT NULL DEFAULT 0,
                lastHamTimestamp INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS spam_keyword_weights (
                keyword TEXT PRIMARY KEY NOT NULL,
                spamOccurrences INTEGER NOT NULL DEFAULT 0,
                hamOccurrences INTEGER NOT NULL DEFAULT 0,
                weight REAL NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS notification_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                soundUri TEXT,
                vibrationEnabled INTEGER NOT NULL DEFAULT 1,
                ledColor INTEGER,
                priority TEXT NOT NULL DEFAULT 'HIGH',
                popupEnabled INTEGER NOT NULL DEFAULT 1,
                isActive INTEGER NOT NULL DEFAULT 0,
                scheduleStartHour INTEGER,
                scheduleStartMinute INTEGER,
                scheduleEndHour INTEGER,
                scheduleEndMinute INTEGER,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversation_meta ADD COLUMN lastReadMessageCount INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversation_meta ADD COLUMN lastReadTimestamp INTEGER")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversation_meta ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scheduled_messages ADD COLUMN sendViaWhatsApp INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        runCatching {
            db.execSQL("ALTER TABLE conversation_meta ADD COLUMN customCategory TEXT")
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scheduled_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                address TEXT NOT NULL,
                body TEXT NOT NULL,
                scheduledTime INTEGER NOT NULL,
                threadId INTEGER NOT NULL DEFAULT -1,
                contactName TEXT,
                isSent INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS message_reactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                messageId INTEGER NOT NULL,
                emoji TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS message_reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                messageId INTEGER NOT NULL,
                threadId INTEGER NOT NULL,
                address TEXT NOT NULL,
                messageBody TEXT NOT NULL,
                contactName TEXT,
                reminderTime INTEGER NOT NULL,
                isTriggered INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS pinned_messages (
                messageId INTEGER PRIMARY KEY NOT NULL,
                threadId INTEGER NOT NULL,
                pinnedAt INTEGER NOT NULL
            )
        """.trimIndent())

        runCatching {
            db.execSQL("ALTER TABLE conversation_meta ADD COLUMN customThemeId INTEGER")
        }
        runCatching {
            db.execSQL("ALTER TABLE conversation_meta ADD COLUMN autoDeleteAfterMs INTEGER")
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovaChatDatabase {
        return Room.databaseBuilder(
            context,
            NovaChatDatabase::class.java,
            "novachat.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18).build()
    }

    @Provides
    fun provideBlockRuleDao(db: NovaChatDatabase): BlockRuleDao = db.blockRuleDao()

    @Provides
    fun provideThemeDao(db: NovaChatDatabase): ThemeDao = db.themeDao()

    @Provides
    fun provideSpamMessageDao(db: NovaChatDatabase): SpamMessageDao = db.spamMessageDao()

    @Provides
    fun provideConversationMetaDao(db: NovaChatDatabase): ConversationMetaDao = db.conversationMetaDao()

    @Provides
    fun provideScheduledMessageDao(db: NovaChatDatabase): ScheduledMessageDao = db.scheduledMessageDao()

    @Provides
    fun provideMessageReactionDao(db: NovaChatDatabase): MessageReactionDao = db.messageReactionDao()

    @Provides
    fun provideMessageReminderDao(db: NovaChatDatabase): MessageReminderDao = db.messageReminderDao()

    @Provides
    fun providePinnedMessageDao(db: NovaChatDatabase): PinnedMessageDao = db.pinnedMessageDao()

    @Provides
    fun provideCustomCategoryDao(db: NovaChatDatabase): CustomCategoryDao = db.customCategoryDao()

    @Provides
    fun provideNotificationProfileDao(db: NovaChatDatabase): NotificationProfileDao = db.notificationProfileDao()

    @Provides
    fun provideSpamLearningDao(db: NovaChatDatabase): SpamLearningDao = db.spamLearningDao()

    @Provides
    fun provideShortCodeWhitelistDao(db: NovaChatDatabase): ShortCodeWhitelistDao = db.shortCodeWhitelistDao()

    @Provides
    fun provideMessageEditDao(db: NovaChatDatabase): MessageEditDao = db.messageEditDao()

    @Provides
    fun provideVoiceTranscriptionDao(db: NovaChatDatabase): VoiceTranscriptionDao = db.voiceTranscriptionDao()
}
