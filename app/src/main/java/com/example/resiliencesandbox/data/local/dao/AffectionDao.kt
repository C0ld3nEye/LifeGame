package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.resiliencesandbox.data.local.entity.AffectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AffectionDao {
    @Query("SELECT * FROM affection_table")
    fun getAllAffectionsFlow(): Flow<List<AffectionEntity>>

    @Query("SELECT * FROM affection_table")
    suspend fun getAllAffections(): List<AffectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffection(affection: AffectionEntity)

    @Query("DELETE FROM affection_table WHERE nom = :nom")
    suspend fun deleteAffectionByName(nom: String)
    
    @Query("DELETE FROM affection_table")
    suspend fun clearAffections()
}
