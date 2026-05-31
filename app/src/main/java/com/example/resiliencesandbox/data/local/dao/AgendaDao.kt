package com.example.resiliencesandbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.resiliencesandbox.data.local.entity.AgendaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaDao {
    @Query("SELECT * FROM agenda_table")
    fun getAllAgenda(): Flow<List<AgendaEntity>>

    @Query("SELECT * FROM agenda_table")
    suspend fun getAllAgendaList(): List<AgendaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgendaItem(agendaItem: AgendaEntity)

    @Query("DELETE FROM agenda_table")
    suspend fun clearAgenda()
}
