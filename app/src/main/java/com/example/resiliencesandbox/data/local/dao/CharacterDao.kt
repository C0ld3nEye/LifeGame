package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character_table WHERE id = 1 LIMIT 1")
    fun getCharacterFlow(): Flow<CharacterEntity?>

    @Query("SELECT * FROM character_table WHERE id = 1 LIMIT 1")
    suspend fun getCharacter(): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    // Exemple de requête pour mettre à jour une émotion spécifique
    @Query("UPDATE character_table SET peur = :newValue WHERE id = 1")
    suspend fun updatePeur(newValue: Int)
    
    // Exemple de requête pour mettre à jour une compétence spécifique
    @Query("UPDATE character_table SET physical = :newValue WHERE id = 1")
    suspend fun updatePhysicalSkill(newValue: Int)
}
