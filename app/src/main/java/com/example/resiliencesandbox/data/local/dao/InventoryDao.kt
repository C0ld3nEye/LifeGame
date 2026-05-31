package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.resiliencesandbox.data.local.entity.InventoryEntity

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE id = :id LIMIT 1")
    suspend fun getInventoryItem(id: String): InventoryEntity?

    @Query("SELECT * FROM inventory")
    suspend fun getAllInventory(): List<InventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryEntity)

    @Query("SELECT * FROM inventory WHERE name = :name LIMIT 1")
    suspend fun getInventoryItemByName(name: String): InventoryEntity?

    @androidx.room.Delete
    suspend fun deleteInventoryItem(item: InventoryEntity)
}
