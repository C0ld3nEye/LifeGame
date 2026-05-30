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

@Database(
    entities = [
        CharacterEntity::class,
        EventLogEntity::class,
        LocationEntity::class,
        NpcEntity::class,
        InventoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun locationDao(): LocationDao
    abstract fun npcDao(): NpcDao
    abstract fun inventoryDao(): InventoryDao

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
