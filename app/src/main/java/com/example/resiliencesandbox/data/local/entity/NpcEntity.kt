package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "npcs")
data class NpcEntity(
    @PrimaryKey val id: String,
    val name: String,
    val relationScore: Int = 0,
    val memoryNotes: String = ""
)
