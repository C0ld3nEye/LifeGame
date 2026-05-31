package com.example.resiliencesandbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.resiliencesandbox.data.local.dao.CharacterDao
import com.example.resiliencesandbox.data.local.dao.EventLogDao
import com.example.resiliencesandbox.data.local.dao.LocationDao
import com.example.resiliencesandbox.data.local.dao.NpcDao
import com.example.resiliencesandbox.data.local.dao.InventoryDao
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import com.example.resiliencesandbox.data.local.entity.EventLogEntity
import com.example.resiliencesandbox.data.local.entity.LocationEntity
import com.example.resiliencesandbox.data.local.entity.NpcEntity
import com.example.resiliencesandbox.data.local.entity.InventoryEntity
import com.example.resiliencesandbox.data.local.dao.AgendaDao
import com.example.resiliencesandbox.data.local.entity.AgendaEntity
import com.example.resiliencesandbox.data.local.dao.AffectionDao
import com.example.resiliencesandbox.data.local.entity.AffectionEntity
import com.example.resiliencesandbox.data.local.dao.SkillDao
import com.example.resiliencesandbox.data.local.entity.SkillEntity

@Database(
    entities = [
        CharacterEntity::class,
        LocationEntity::class,
        NpcEntity::class,
        InventoryEntity::class,
        EventLogEntity::class,
        AgendaEntity::class,
        AffectionEntity::class,
        SkillEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun locationDao(): LocationDao
    abstract fun npcDao(): NpcDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun agendaDao(): AgendaDao
    abstract fun affectionDao(): AffectionDao
    abstract fun skillDao(): SkillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resilience_sandbox_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
