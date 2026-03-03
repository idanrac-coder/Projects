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
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.dao.ThemeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9).build()
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
}
