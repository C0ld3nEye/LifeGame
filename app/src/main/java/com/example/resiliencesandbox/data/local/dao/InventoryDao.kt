package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.resiliencesandbox.data.local.entity.InventoryEntity

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE itemId = :itemId LIMIT 1")
    suspend fun getInventoryItem(itemId: String): InventoryEntity?

    @Query("SELECT * FROM inventory")
    suspend fun getAllInventory(): List<InventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryEntity)
}
