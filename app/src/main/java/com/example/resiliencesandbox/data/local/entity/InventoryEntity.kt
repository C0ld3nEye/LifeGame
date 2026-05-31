package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val condition: String,
    val description: String,
    val quantity: Int
)
