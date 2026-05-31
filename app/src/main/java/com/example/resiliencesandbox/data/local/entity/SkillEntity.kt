package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey
    val name: String,
    val level: Int,
    val lastUsedMinute: Long
)
