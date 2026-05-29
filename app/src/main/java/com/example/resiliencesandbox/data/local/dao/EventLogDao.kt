package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.resiliencesandbox.data.local.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EventLogEntity)

    @Query("SELECT * FROM event_log_table ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_log_table ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<EventLogEntity>
}
