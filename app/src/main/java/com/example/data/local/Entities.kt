package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_ais")
data class CustomAiEntity(
  @PrimaryKey val id: String,
  val name: String,
  val officialUrl: String,
  val monogram: String,
  val brandColorHex: Long,
  val category: String,
  val shortDescription: String,
  val pricingType: String,
  val pricingDetails: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "ai_preferences")
data class AiPreferenceEntity(
  @PrimaryKey val aiId: String,
  val isFavorite: Boolean = false,
  val isPinned: Boolean = false,
  val customOrder: Int = 999,
  val usageCount: Int = 0,
  val lastOpenedAt: Long = 0L,
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
  @PrimaryKey val id: String,
  val aiId: String,
  val aiName: String,
  val title: String,
  val officialUrl: String,
  val currentUrl: String,
  val notes: String = "",
  val isMinimized: Boolean = false,
  val isLocked: Boolean = false,
  val compatibilityMode: String = "IN_APP_WEBVIEW",
  val sortOrder: Int = 0,
  val lastActiveAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey val id: String,
  val name: String,
  val url: String,
  val aiId: String,
  val aiName: String,
  val category: String,
  val notes: String = "",
  val isFavorite: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "api_providers")
data class ApiProviderEntity(
  @PrimaryKey val id: String,
  val displayName: String,
  val providerType: String,
  val endpoint: String,
  val model: String,
  val systemPrompt: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "api_chat_sessions")
data class ApiChatSessionEntity(
  @PrimaryKey val id: String,
  val title: String,
  val providerId: String,
  val model: String,
  val systemPrompt: String,
  val temperature: Float = 0.7f,
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "api_chat_messages")
data class ApiChatMessageEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val role: String, // "user", "assistant", "error"
  val content: String,
  val attachmentName: String? = null,
  val attachmentType: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "vault_files")
data class VaultFileEntity(
  @PrimaryKey val id: String,
  val name: String,
  val folder: String = "Root",
  val mimeType: String,
  val sizeBytes: Long,
  val localPath: String,
  val isFolder: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
  @PrimaryKey val id: String,
  val category: String, // "Workspace", "API", "Download", "Upload", "Network", "Backup"
  val title: String,
  val message: String,
  val isError: Boolean = false,
  val isRead: Boolean = false,
  val timestamp: Long = System.currentTimeMillis(),
)
