package com.example.resiliencesandbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_log_table")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val descriptionText: String,
    val isRoutineTick: Boolean
)
