package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAiDao {
  @Query("SELECT * FROM custom_ais ORDER BY createdAt DESC")
  fun getAll(): Flow<List<CustomAiEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: CustomAiEntity)

  @Query("DELETE FROM custom_ais WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface AiPreferenceDao {
  @Query("SELECT * FROM ai_preferences")
  fun getAll(): Flow<List<AiPreferenceEntity>>

  @Query("SELECT * FROM ai_preferences WHERE aiId = :aiId LIMIT 1")
  suspend fun getById(aiId: String): AiPreferenceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: AiPreferenceEntity)

  @Query("DELETE FROM ai_preferences")
  suspend fun clearAll()
}

@Dao
interface WorkspaceDao {
  @Query("SELECT * FROM workspaces ORDER BY sortOrder ASC, lastActiveAt DESC")
  fun getAll(): Flow<List<WorkspaceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: WorkspaceEntity)

  @Query("DELETE FROM workspaces WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM workspaces")
  suspend fun clearAll()
}

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects ORDER BY createdAt DESC")
  fun getAll(): Flow<List<ProjectEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface ApiProviderDao {
  @Query("SELECT * FROM api_providers ORDER BY createdAt DESC")
  fun getAll(): Flow<List<ApiProviderEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: ApiProviderEntity)

  @Query("DELETE FROM api_providers WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface ApiChatDao {
  @Query("SELECT * FROM api_chat_sessions ORDER BY updatedAt DESC")
  fun getAllSessions(): Flow<List<ApiChatSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSession(session: ApiChatSessionEntity)

  @Query("DELETE FROM api_chat_sessions WHERE id = :sessionId")
  suspend fun deleteSession(sessionId: String)

  @Query("SELECT * FROM api_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
  fun getMessagesForSession(sessionId: String): Flow<List<ApiChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMessage(message: ApiChatMessageEntity)

  @Query("DELETE FROM api_chat_messages WHERE id = :messageId")
  suspend fun deleteMessage(messageId: String)

  @Query("DELETE FROM api_chat_messages WHERE sessionId = :sessionId")
  suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface VaultFileDao {
  @Query("SELECT * FROM vault_files ORDER BY isFolder DESC, createdAt DESC")
  fun getAll(): Flow<List<VaultFileEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(file: VaultFileEntity)

  @Query("DELETE FROM vault_files WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface ActivityLogDao {
  @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
  fun getAll(): Flow<List<ActivityLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(log: ActivityLogEntity)

  @Query("UPDATE activity_logs SET isRead = 1")
  suspend fun markAllRead()

  @Query("DELETE FROM activity_logs")
  suspend fun clearAll()
}
