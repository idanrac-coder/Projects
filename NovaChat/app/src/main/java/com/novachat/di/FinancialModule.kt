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
import net.sqlcipher.database.SupportFactory
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
        val passphrase = keyManager.getPassphrase()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context,
            FinancialDatabase::class.java,
            "financial.db"
        )
            .openHelperFactory(factory)
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
