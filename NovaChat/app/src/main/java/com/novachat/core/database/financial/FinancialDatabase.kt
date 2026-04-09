package com.novachat.core.database.financial

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novachat.core.database.financial.dao.CardDao
import com.novachat.core.database.financial.dao.FinancialAlertDao
import com.novachat.core.database.financial.dao.FinancialSenderDao
import com.novachat.core.database.financial.dao.FinancialTransactionDao
import com.novachat.core.database.financial.dao.MerchantDao
import com.novachat.core.database.financial.dao.SubscriptionDao
import com.novachat.core.database.financial.entity.CardEntity
import com.novachat.core.database.financial.entity.FinancialAlertEntity
import com.novachat.core.database.financial.entity.FinancialSenderEntity
import com.novachat.core.database.financial.entity.FinancialTransactionEntity
import com.novachat.core.database.financial.entity.MerchantEntity
import com.novachat.core.database.financial.entity.SubscriptionEntity

@Database(
    entities = [
        FinancialTransactionEntity::class,
        SubscriptionEntity::class,
        MerchantEntity::class,
        CardEntity::class,
        FinancialSenderEntity::class,
        FinancialAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinancialDatabase : RoomDatabase() {
    abstract fun transactionDao(): FinancialTransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun merchantDao(): MerchantDao
    abstract fun cardDao(): CardDao
    abstract fun senderDao(): FinancialSenderDao
    abstract fun alertDao(): FinancialAlertDao
}
