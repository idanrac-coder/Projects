package com.novachat.core.database.financial.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novachat.core.database.financial.entity.MerchantEntity

@Dao
interface MerchantDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(merchant: MerchantEntity): Long

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Query("SELECT * FROM merchants WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): MerchantEntity?

    @Query("DELETE FROM merchants")
    suspend fun deleteAll()
}
