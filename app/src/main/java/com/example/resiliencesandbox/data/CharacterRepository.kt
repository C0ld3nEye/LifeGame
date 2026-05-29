package com.example.resiliencesandbox.data

import com.example.resiliencesandbox.data.local.dao.CharacterDao
import com.example.resiliencesandbox.data.local.dao.EventLogDao
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import com.example.resiliencesandbox.data.local.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

class CharacterRepository(
    private val characterDao: CharacterDao,
    private val eventLogDao: EventLogDao
) {
    val characterFlow: Flow<CharacterEntity?> = characterDao.getCharacterFlow()
    val eventLogsFlow: Flow<List<EventLogEntity>> = eventLogDao.getAllLogs()

    suspend fun getCharacterState(): CharacterEntity? {
        return characterDao.getCharacter()
    }

    suspend fun saveCharacterState(character: CharacterEntity) {
        characterDao.insertCharacter(character)
    }

    suspend fun updateCharacter(character: CharacterEntity) {
        characterDao.updateCharacter(character)
    }

    suspend fun updatePeur(newValue: Int) {
        characterDao.updatePeur(newValue)
    }

    suspend fun updatePhysicalSkill(newValue: Int) {
        characterDao.updatePhysicalSkill(newValue)
    }

    suspend fun addEventLog(timestamp: Long, description: String, isRoutineTick: Boolean) {
        val log = EventLogEntity(
            timestamp = timestamp,
            descriptionText = description,
            isRoutineTick = isRoutineTick
        )
        eventLogDao.insertLog(log)
    }

    suspend fun getRecentEventLogs(limit: Int): List<EventLogEntity> {
        return eventLogDao.getRecentLogs(limit)
    }
}
