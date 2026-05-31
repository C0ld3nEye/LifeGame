package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_table")
data class CharacterEntity(
    @PrimaryKey val id: Int = 1, // Utilisateur unique
    val argent: Int,
    val energie: Int,
    val postureActuelle: String,
    val gameTimeMinutes: Long = 0L,
    
    // Compétences (0 à 100)
    val physical: Int,
    val social: Int,
    val intellect: Int,
    val survival: Int,
    val finance: Int,
    val willpower: Int,
    val creativity: Int,

    // Émotions (0 à 100)
    val peur: Int,
    val colere: Int,
    val tristesse: Int,
    val joie: Int,
    val calme: Int,
    val fatigue: Int,
    val toxicite: Int = 0,
    val obsession: String = "Trouver un endroit sûr pour se reposer."
)
