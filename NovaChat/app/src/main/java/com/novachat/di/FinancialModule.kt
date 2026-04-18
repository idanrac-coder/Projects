package com.novachat.di

import android.content.Context
import androidx.room.Room
import com.novachat.core.database.financial.FinancialDatabase
import com.novachat.core.database.financial.dao.CardDao
import com.novachat.core.database.financial.dao.FinancialAlertDao
import com.novachat.core.database.financial.dao.FinancialSenderDao
import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.MerchantDao
import com.novachat.core.database.financial.dao.SubscriptionDao
import com.novachat.core.security.FinancialKeyManager
import com.novachat.data.repository.FinancialRepositoryImpl
import com.novachat.domain.repository.FinancialRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FinancialDatabaseModule {

    @Provides
    @Singleton
    fun provideFinancialDatabase(
        @ApplicationContext context: Context,
        keyManager: FinancialKeyManager
    ): FinancialDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = runBlocking(Dispatchers.IO) { keyManager.getPassphrase() }
        val dbFile = context.getDatabasePath("financial.db")
        val hook: SQLiteDatabaseHook? = if (dbFile.exists()) object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection) {}
            override fun postKey(connection: SQLiteConnection) {
                connection.execute("PRAGMA cipher_page_size = 4096;", arrayOf(), null)
            }
        } else null
        val factory = SupportOpenHelperFactory(passphrase, hook, false)
        return Room.databaseBuilder(
            context,
            FinancialDatabase::class.java,
            "financial.db"
        )
            .openHelperFactory(factory)
            .addMigrations(FinancialDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideTransactionDao(db: FinancialDatabase): FinancialTransactionDao = db.transactionDao()

    @Provides
    fun provideSubscriptionDao(db: FinancialDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    fun provideMerchantDao(db: FinancialDatabase): MerchantDao = db.merchantDao()

    @Provides
    fun provideCardDao(db: FinancialDatabase): CardDao = db.cardDao()

    @Provides
    fun provideSenderDao(db: FinancialDatabase): FinancialSenderDao = db.senderDao()

    @Provides
    fun provideAlertDao(db: FinancialDatabase): FinancialAlertDao = db.alertDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FinancialBindsModule {

    @Binds
    @Singleton
    abstract fun bindFinancialRepository(impl: FinancialRepositoryImpl): FinancialRepository
}
