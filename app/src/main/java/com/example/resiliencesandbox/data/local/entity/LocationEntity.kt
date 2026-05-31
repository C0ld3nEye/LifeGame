package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isDiscovered: Boolean = false,
    val sentiment: String = "Inconnu",
    val passif: String = "Aucun souvenir particulier."
)
