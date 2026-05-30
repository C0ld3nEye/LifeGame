package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.resiliencesandbox.data.local.entity.NpcEntity

@Dao
interface NpcDao {
    @Query("SELECT * FROM npcs WHERE id = :id LIMIT 1")
    suspend fun getNpcById(id: String): NpcEntity?

    @Query("SELECT * FROM npcs")
    suspend fun getAllNpcs(): List<NpcEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpc(npc: NpcEntity)
}
