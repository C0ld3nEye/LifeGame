package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "affection_table")
data class AffectionEntity(
    @PrimaryKey
    val id: String, // Le nom sera utilisé comme ID unique
    val nom: String,
    val type: String,
    val description: String
)
