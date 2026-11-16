package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities =
    [
      CustomAiEntity::class,
      AiPreferenceEntity::class,
      WorkspaceEntity::class,
      ProjectEntity::class,
      ApiProviderEntity::class,
      ApiChatSessionEntity::class,
      ApiChatMessageEntity::class,
      VaultFileEntity::class,
      ActivityLogEntity::class,
    ],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun customAiDao(): CustomAiDao

  abstract fun aiPreferenceDao(): AiPreferenceDao

  abstract fun workspaceDao(): WorkspaceDao

  abstract fun projectDao(): ProjectDao

  abstract fun apiProviderDao(): ApiProviderDao

  abstract fun apiChatDao(): ApiChatDao

  abstract fun vaultFileDao(): VaultFileDao

  abstract fun activityLogDao(): ActivityLogDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ai_universe_db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
