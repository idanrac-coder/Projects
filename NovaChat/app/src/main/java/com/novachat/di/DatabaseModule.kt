package com.novachat.di

import android.content.Context
import androidx.room.Room
import com.novachat.core.database.NovaChatDatabase
import com.novachat.core.database.dao.BlockRuleDao
import com.novachat.core.database.dao.ConversationMetaDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.dao.ThemeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        ).build()
    }

    @Provides
    fun provideBlockRuleDao(db: NovaChatDatabase): BlockRuleDao = db.blockRuleDao()

    @Provides
    fun provideThemeDao(db: NovaChatDatabase): ThemeDao = db.themeDao()

    @Provides
    fun provideSpamMessageDao(db: NovaChatDatabase): SpamMessageDao = db.spamMessageDao()

    @Provides
    fun provideConversationMetaDao(db: NovaChatDatabase): ConversationMetaDao = db.conversationMetaDao()
}
