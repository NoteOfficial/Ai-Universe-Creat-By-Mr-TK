package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.*
import com.example.data.model.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

import androidx.compose.runtime.Immutable

@Immutable
data class StorageStats(
  val vaultBytes: Long,
  val cacheBytes: Long,
  val databaseBytes: Long,
  val fileCount: Int,
)

class AiUniverseRepository(
  private val context: Context,
  private val db: AppDatabase,
  val settingsStore: SettingsDataStore,
  val secureKeyStore: SecureKeyStore,
) {
  private val vaultDir: File = File(context.filesDir, "vault").apply { mkdirs() }

  val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
  val workspacesFlow: Flow<List<WorkspaceEntity>> = db.workspaceDao().getAll()
  val projectsFlow: Flow<List<ProjectEntity>> = db.projectDao().getAll()
  val apiProvidersFlow: Flow<List<ApiProviderEntity>> = db.apiProviderDao().getAll()
  val apiChatSessionsFlow: Flow<List<ApiChatSessionEntity>> = db.apiChatDao().getAllSessions()
  val vaultFilesFlow: Flow<List<VaultFileEntity>> = db.vaultFileDao().getAll()
  val activityLogsFlow: Flow<List<ActivityLogEntity>> = db.activityLogDao().getAll()

  val allPlatformsFlow: Flow<List<AiPlatform>> =
    combine(db.customAiDao().getAll(), db.aiPreferenceDao().getAll()) { customList, prefs ->
      val prefMap = prefs.associateBy { it.aiId }
      val official =
        AiRegistry.officialPlatforms.map { platform ->
          val p = prefMap[platform.id]
          if (p != null) {
            platform.copy(
              isFavorite = p.isFavorite,
              isPinned = p.isPinned,
              customOrder = p.customOrder,
            )
          } else {
            platform
          }
        }
      val custom =
        customList.map { c ->
          val p = prefMap[c.id]
          val cat = runCatching { AiCategory.valueOf(c.category) }.getOrDefault(AiCategory.OTHER)
          val price =
            runCatching { PricingType.valueOf(c.pricingType) }.getOrDefault(PricingType.FREE_AND_PAID)
          val host = com.example.data.network.UrlValidator.extractHost(c.officialUrl)
          AiPlatform(
            id = c.id,
            name = c.name,
            officialUrl = c.officialUrl,
            approvedOrigins = listOfNotNull(host.takeIf { it.isNotBlank() }),
            monogram = c.monogram,
            brandColorHex = c.brandColorHex,
            secondaryColorHex = c.brandColorHex,
            category = cat,
            shortDescription = c.shortDescription,
            mainPurpose = c.shortDescription,
            bestUseCases = listOf("Custom AI workflow"),
            features = listOf("Custom Website AI", "Isolated Workspace"),
            capabilities = AiCapabilities(coding = true, fileUpload = true),
            pricingType = price,
            pricingDetails = c.pricingDetails,
            loginRequirement = "Managed by provider website",
            apiAvailability = "Website integration",
            compatibility = CompatibilityStatus.COMPATIBILITY_UNKNOWN,
            knownLimitations = "User-added custom website entry.",
            isCustom = true,
            isFavorite = p?.isFavorite ?: false,
            isPinned = p?.isPinned ?: false,
            customOrder = p?.customOrder ?: 500,
          )
        }
      (official + custom).sortedWith(compareByDescending<AiPlatform> { it.isPinned }.thenBy { it.customOrder })
    }

  fun messagesForSession(sessionId: String): Flow<List<ApiChatMessageEntity>> =
    db.apiChatDao().getMessagesForSession(sessionId)

  suspend fun toggleFavorite(aiId: String) {
    val existing = db.aiPreferenceDao().getById(aiId)
    val updated =
      existing?.copy(isFavorite = !existing.isFavorite)
        ?: AiPreferenceEntity(aiId = aiId, isFavorite = true)
    db.aiPreferenceDao().upsert(updated)
  }

  suspend fun togglePin(aiId: String) {
    val existing = db.aiPreferenceDao().getById(aiId)
    val updated =
      existing?.copy(isPinned = !existing.isPinned)
        ?: AiPreferenceEntity(aiId = aiId, isPinned = true)
    db.aiPreferenceDao().upsert(updated)
  }

  suspend fun updateAiOrder(aiId: String, newOrder: Int) {
    val existing = db.aiPreferenceDao().getById(aiId)
    val updated =
      existing?.copy(customOrder = newOrder)
        ?: AiPreferenceEntity(aiId = aiId, customOrder = newOrder)
    db.aiPreferenceDao().upsert(updated)
  }

  suspend fun recordAiOpened(aiId: String) {
    val existing = db.aiPreferenceDao().getById(aiId)
    val updated =
      existing?.copy(
        usageCount = existing.usageCount + 1,
        lastOpenedAt = System.currentTimeMillis(),
      )
        ?: AiPreferenceEntity(
          aiId = aiId,
          usageCount = 1,
          lastOpenedAt = System.currentTimeMillis(),
        )
    db.aiPreferenceDao().upsert(updated)
  }

  suspend fun saveCustomAi(entity: CustomAiEntity) {
    db.customAiDao().upsert(entity)
    logActivity("Custom AI", "Saved ${entity.name}", entity.officialUrl)
  }

  suspend fun deleteCustomAi(id: String) {
    db.customAiDao().deleteById(id)
  }

  suspend fun upsertWorkspace(workspace: WorkspaceEntity) {
    db.workspaceDao().upsert(workspace)
  }

  suspend fun deleteWorkspace(id: String) {
    db.workspaceDao().deleteById(id)
  }

  suspend fun clearAllWorkspaces() {
    db.workspaceDao().clearAll()
  }

  suspend fun saveProject(project: ProjectEntity) {
    db.projectDao().upsert(project)
    logActivity("Project", "Saved ${project.name}", project.url)
  }

  suspend fun deleteProject(id: String) {
    db.projectDao().deleteById(id)
  }

  suspend fun saveApiProvider(entity: ApiProviderEntity, apiKey: String) {
    if (apiKey.isNotBlank()) {
      secureKeyStore.saveApiKey(entity.id, apiKey.trim())
    }
    db.apiProviderDao().upsert(entity)
    logActivity("API", "Saved provider ${entity.displayName}", "Model: ${entity.model}")
  }

  suspend fun deleteApiProvider(id: String) {
    secureKeyStore.deleteApiKey(id)
    db.apiProviderDao().deleteById(id)
  }

  suspend fun upsertChatSession(session: ApiChatSessionEntity) {
    db.apiChatDao().upsertSession(session)
  }

  suspend fun deleteChatSession(sessionId: String) {
    db.apiChatDao().deleteMessagesForSession(sessionId)
    db.apiChatDao().deleteSession(sessionId)
  }

  suspend fun upsertChatMessage(message: ApiChatMessageEntity) {
    db.apiChatDao().upsertMessage(message)
  }

  suspend fun deleteChatMessage(messageId: String) {
    db.apiChatDao().deleteMessage(messageId)
  }

  suspend fun importFileFromUri(uri: Uri, folder: String = "Root"): Result<VaultFileEntity> =
    withContext(Dispatchers.IO) {
      try {
        val resolver = context.contentResolver
        var fileName = "file_${System.currentTimeMillis()}"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
          }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(vaultDir, "${UUID.randomUUID()}_$safeName")
        resolver.openInputStream(uri)?.use { input ->
          destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file"))

        val entity =
          VaultFileEntity(
            id = UUID.randomUUID().toString(),
            name = fileName,
            folder = folder,
            mimeType = mime,
            sizeBytes = if (size > 0L) size else destFile.length(),
            localPath = destFile.absolutePath,
            isFolder = false,
          )
        db.vaultFileDao().upsert(entity)
        logActivity("Upload", "Imported $fileName", "${entity.sizeBytes} bytes")
        Result.success(entity)
      } catch (e: Exception) {
        logActivity("Upload", "Import Failed", e.localizedMessage ?: "Unknown error", isError = true)
        Result.failure(e)
      }
    }

  suspend fun createVaultFolder(name: String, parentFolder: String = "Root") {
    val entity =
      VaultFileEntity(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        folder = parentFolder,
        mimeType = "inode/directory",
        sizeBytes = 0L,
        localPath = "",
        isFolder = true,
      )
    db.vaultFileDao().upsert(entity)
  }

  suspend fun renameVaultFile(file: VaultFileEntity, newName: String) {
    db.vaultFileDao().upsert(file.copy(name = newName.trim()))
  }

  suspend fun moveVaultFile(file: VaultFileEntity, targetFolder: String) {
    db.vaultFileDao().upsert(file.copy(folder = targetFolder))
  }

  suspend fun deleteVaultFile(file: VaultFileEntity) {
    withContext(Dispatchers.IO) {
      if (!file.isFolder && file.localPath.isNotBlank()) {
        runCatching { File(file.localPath).delete() }
      }
    }
    db.vaultFileDao().deleteById(file.id)
  }

  suspend fun logActivity(category: String, title: String, message: String, isError: Boolean = false) {
    db.activityLogDao()
      .insert(
        ActivityLogEntity(
          id = UUID.randomUUID().toString(),
          category = category,
          title = title,
          message = message,
          isError = isError,
        )
      )
  }

  suspend fun markAllLogsRead() = db.activityLogDao().markAllRead()

  suspend fun clearAllLogs() = db.activityLogDao().clearAll()

  suspend fun computeStorageStats(): StorageStats =
    withContext(Dispatchers.IO) {
      val vaultFiles = vaultDir.listFiles()?.toList().orEmpty()
      val vaultSize = vaultFiles.sumOf { it.length() }
      val cacheSize = folderSize(context.cacheDir)
      val dbFile = context.getDatabasePath("ai_universe_db")
      val dbSize = if (dbFile.exists()) dbFile.length() else 0L
      StorageStats(
        vaultBytes = vaultSize,
        cacheBytes = cacheSize,
        databaseBytes = dbSize,
        fileCount = vaultFiles.size,
      )
    }

  suspend fun clearAppCache() =
    withContext(Dispatchers.IO) {
      context.cacheDir.deleteRecursively()
      context.cacheDir.mkdirs()
    }

  suspend fun exportBackupJson(): String =
    withContext(Dispatchers.IO) {
      val customAis = db.customAiDao().getAll().first()
      val projects = db.projectDao().getAll().first()
      val workspaces = db.workspaceDao().getAll().first()
      val prefs = db.aiPreferenceDao().getAll().first()

      val root =
        JSONObject().apply {
          put("app", "AI UNIVERSE")
          put("version", 1)
          put("exportedAt", System.currentTimeMillis())
          put(
            "customAis",
            JSONArray().apply {
              customAis.forEach { c ->
                put(
                  JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("officialUrl", c.officialUrl)
                    put("monogram", c.monogram)
                    put("brandColorHex", c.brandColorHex)
                    put("category", c.category)
                    put("shortDescription", c.shortDescription)
                    put("pricingType", c.pricingType)
                    put("pricingDetails", c.pricingDetails)
                  }
                )
              }
            },
          )
          put(
            "projects",
            JSONArray().apply {
              projects.forEach { p ->
                put(
                  JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("url", p.url)
                    put("aiId", p.aiId)
                    put("aiName", p.aiName)
                    put("category", p.category)
                    put("notes", p.notes)
                    put("isFavorite", p.isFavorite)
                  }
                )
              }
            },
          )
          put(
            "workspaces",
            JSONArray().apply {
              workspaces.forEach { w ->
                put(
                  JSONObject().apply {
                    put("id", w.id)
                    put("aiId", w.aiId)
                    put("aiName", w.aiName)
                    put("title", w.title)
                    put("officialUrl", w.officialUrl)
                    put("currentUrl", w.currentUrl)
                    put("notes", w.notes)
                    put("isMinimized", w.isMinimized)
                    put("isLocked", w.isLocked)
                  }
                )
              }
            },
          )
          put(
            "preferences",
            JSONArray().apply {
              prefs.forEach { pr ->
                put(
                  JSONObject().apply {
                    put("aiId", pr.aiId)
                    put("isFavorite", pr.isFavorite)
                    put("isPinned", pr.isPinned)
                    put("customOrder", pr.customOrder)
                  }
                )
              }
            },
          )
        }
      settingsStore.updateLastBackupAt(System.currentTimeMillis())
      logActivity("Backup", "Backup Exported", "Exported custom AIs, projects, and workspaces")
      root.toString(2)
    }

  suspend fun importBackupJson(jsonText: String): Result<Int> =
    withContext(Dispatchers.IO) {
      try {
        val root = JSONObject(jsonText)
        if (root.optString("app") != "AI UNIVERSE") {
          return@withContext Result.failure(IllegalArgumentException("Not a valid AI UNIVERSE backup file"))
        }
        var count = 0
        root.optJSONArray("customAis")?.let { arr ->
          for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.customAiDao()
              .upsert(
                CustomAiEntity(
                  id = o.getString("id"),
                  name = o.getString("name"),
                  officialUrl = o.getString("officialUrl"),
                  monogram = o.optString("monogram", "AI"),
                  brandColorHex = o.optLong("brandColorHex", 0xFF536DFE),
                  category = o.optString("category", AiCategory.OTHER.name),
                  shortDescription = o.optString("shortDescription", ""),
                  pricingType = o.optString("pricingType", PricingType.FREE_AND_PAID.name),
                  pricingDetails = o.optString("pricingDetails", ""),
                )
              )
            count++
          }
        }
        root.optJSONArray("projects")?.let { arr ->
          for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.projectDao()
              .upsert(
                ProjectEntity(
                  id = o.getString("id"),
                  name = o.getString("name"),
                  url = o.getString("url"),
                  aiId = o.optString("aiId", ""),
                  aiName = o.optString("aiName", ""),
                  category = o.optString("category", "General"),
                  notes = o.optString("notes", ""),
                  isFavorite = o.optBoolean("isFavorite", false),
                )
              )
            count++
          }
        }
        root.optJSONArray("workspaces")?.let { arr ->
          for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.workspaceDao()
              .upsert(
                WorkspaceEntity(
                  id = o.getString("id"),
                  aiId = o.getString("aiId"),
                  aiName = o.getString("aiName"),
                  title = o.getString("title"),
                  officialUrl = o.getString("officialUrl"),
                  currentUrl = o.optString("currentUrl", o.getString("officialUrl")),
                  notes = o.optString("notes", ""),
                  isMinimized = o.optBoolean("isMinimized", true),
                  isLocked = o.optBoolean("isLocked", false),
                )
              )
            count++
          }
        }
        root.optJSONArray("preferences")?.let { arr ->
          for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.aiPreferenceDao()
              .upsert(
                AiPreferenceEntity(
                  aiId = o.getString("aiId"),
                  isFavorite = o.optBoolean("isFavorite", false),
                  isPinned = o.optBoolean("isPinned", false),
                  customOrder = o.optInt("customOrder", 999),
                )
              )
          }
        }
        logActivity("Backup", "Backup Restored", "Imported $count items successfully")
        Result.success(count)
      } catch (e: Exception) {
        logActivity("Backup", "Restore Failed", e.localizedMessage ?: "Invalid JSON", isError = true)
        Result.failure(e)
      }
    }

  private fun folderSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var total = 0L
    dir.listFiles()?.forEach { file ->
      total += if (file.isDirectory) folderSize(file) else file.length()
    }
    return total
  }
}
