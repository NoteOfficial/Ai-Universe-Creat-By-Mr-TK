package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AiUniverseApp
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.network.ApiAttachmentPayload
import com.example.data.network.ApiChatMessagePayload
import com.example.data.network.ApiResponseResult
import com.example.data.network.AvailabilityResult
import com.example.data.network.UrlValidator
import com.example.data.repository.StorageStats
import com.example.ui.localization.AppLanguage
import com.example.ui.theme.ColorPaletteTheme
import com.example.ui.theme.FontScaleOption
import com.example.ui.theme.ThemeMode
import java.util.UUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
  DEFAULT("Default Order"),
  NAME_ASC("Name (A-Z)"),
  FAVORITES_FIRST("Favorites First"),
  CATEGORY("Category"),
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as AiUniverseApp
  val repository = app.repository
  val webViewManager = app.webViewManager
  val apiChatClient = app.apiChatClient
  val urlChecker = app.urlAvailabilityChecker

  val settings: StateFlow<AppSettings> =
    repository.settingsFlow.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      repository.settingsStore.readSyncInitialSettings(),
    )

  val platforms: StateFlow<List<AiPlatform>> =
    repository.allPlatformsFlow.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      AiRegistry.officialPlatforms,
    )

  private val _optimisticWorkspaces = MutableStateFlow<Map<String, WorkspaceEntity>>(emptyMap())
  private val _closedWorkspaceIds = MutableStateFlow<Set<String>>(emptySet())

  val workspaces: StateFlow<List<WorkspaceEntity>> =
    combine(
        repository.workspacesFlow,
        _optimisticWorkspaces,
        _closedWorkspaceIds,
      ) { dbList, optimistic, closed ->
        val merged = LinkedHashMap<String, WorkspaceEntity>()
        optimistic.values
          .filterNot { it.id in closed }
          .forEach { merged[it.id] = it }
        dbList
          .filterNot { it.id in closed }
          .forEach { ws ->
            if (!merged.containsKey(ws.id)) {
              merged[ws.id] = ws
            }
          }
        merged.values.sortedByDescending { it.lastActiveAt }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val projects: StateFlow<List<ProjectEntity>> =
    repository.projectsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val apiProviders: StateFlow<List<ApiProviderEntity>> =
    repository.apiProvidersFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val chatSessions: StateFlow<List<ApiChatSessionEntity>> =
    repository.apiChatSessionsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val vaultFiles: StateFlow<List<VaultFileEntity>> =
    repository.vaultFilesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val activityLogs: StateFlow<List<ActivityLogEntity>> =
    repository.activityLogsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _networkStatus = MutableStateFlow(NetworkStatus())
  val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

  private val _clipboardHistory = MutableStateFlow<List<ClipboardItem>>(emptyList())
  val clipboardHistory: StateFlow<List<ClipboardItem>> = _clipboardHistory.asStateFlow()

  private val _activeWorkspaceId = MutableStateFlow<String?>(null)
  val activeWorkspaceId: StateFlow<String?> = _activeWorkspaceId.asStateFlow()

  private val _splitSecondaryWorkspaceId = MutableStateFlow<String?>(null)
  val splitSecondaryWorkspaceId: StateFlow<String?> = _splitSecondaryWorkspaceId.asStateFlow()

  private val _isApiGenerating = MutableStateFlow(false)
  val isApiGenerating: StateFlow<Boolean> = _isApiGenerating.asStateFlow()

  private val _availabilityCache = MutableStateFlow<Map<String, AvailabilityResult>>(emptyMap())
  val availabilityCache: StateFlow<Map<String, AvailabilityResult>> =
    _availabilityCache.asStateFlow()

  private val _storageStats = MutableStateFlow(StorageStats(0L, 0L, 0L, 0))
  val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

  private val _isAppLocked = MutableStateFlow(false)
  val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow<AiCategory?>(null)
  val selectedCategory: StateFlow<AiCategory?> = _selectedCategory.asStateFlow()

  private val _selectedPricing = MutableStateFlow<PricingType?>(null)
  val selectedPricing: StateFlow<PricingType?> = _selectedPricing.asStateFlow()

  private val _sortOption = MutableStateFlow(SortOption.DEFAULT)
  val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

  private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
  val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

  init {
    observeNetwork()
    refreshStorageStats()
    viewModelScope.launch {
      settings.first().let { s ->
        if (s.appLockEnabled && repository.secureKeyStore.hasPin()) {
          _isAppLocked.value = true
        }
      }
    }
    webViewManager.onUrlUpdated = { wsId, newUrl ->
      viewModelScope.launch {
        val current = workspaces.value.find { it.id == wsId }
        if (current != null && current.currentUrl != newUrl) {
          repository.upsertWorkspace(
            current.copy(currentUrl = newUrl, lastActiveAt = System.currentTimeMillis())
          )
        }
      }
    }
  }

  fun recordDownloadActivity(filename: String, mimeType: String, url: String) {
    if (!settings.value.notifyDownloads) return
    viewModelScope.launch {
      repository.logActivity("Download", "Download Started: $filename", "$mimeType • $url")
    }
  }

  private fun observeNetwork() {
    val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
    updateNetworkSnapshot(cm)
    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateNetworkSnapshot(cm)

        override fun onLost(network: Network) = updateNetworkSnapshot(cm)

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
          updateNetworkSnapshot(cm)
      }
    runCatching { cm.registerDefaultNetworkCallback(callback) }
  }

  private fun updateNetworkSnapshot(cm: ConnectivityManager) {
    val active = cm.activeNetwork
    val caps = active?.let { cm.getNetworkCapabilities(it) }
    val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val transport =
      when {
        caps == null -> "Offline"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Online"
      }
    _networkStatus.value = NetworkStatus(isConnected = connected, transportLabel = transport)
  }

  fun setSearchQuery(q: String) {
    _searchQuery.value = q
  }

  fun commitSearchHistory(q: String) {
    val clean = q.trim()
    if (clean.length >= 2) {
      _recentSearches.update { (listOf(clean) + it.filterNot { item -> item == clean }).take(8) }
    }
  }

  fun setSelectedCategory(category: AiCategory?) {
    _selectedCategory.value = category
  }

  fun setSelectedPricing(pricing: PricingType?) {
    _selectedPricing.value = pricing
  }

  fun setSortOption(option: SortOption) {
    _sortOption.value = option
  }

  fun toggleFavorite(aiId: String) = viewModelScope.launch { repository.toggleFavorite(aiId) }

  fun togglePin(aiId: String) = viewModelScope.launch { repository.togglePin(aiId) }

  fun movePlatformOrder(aiId: String, delta: Int) =
    viewModelScope.launch {
      val list = platforms.value
      val idx = list.indexOfFirst { it.id == aiId }
      if (idx == -1) return@launch
      val targetIdx = (idx + delta).coerceIn(0, list.lastIndex)
      if (idx == targetIdx) return@launch
      val current = list[idx]
      val swap = list[targetIdx]
      repository.updateAiOrder(current.id, targetIdx + 1)
      repository.updateAiOrder(swap.id, idx + 1)
    }

  fun openOrSwitchWorkspace(platform: AiPlatform): String {
    val existing = workspaces.value.find { it.aiId == platform.id }
    val wsId = existing?.id ?: "ws_${platform.id}_${UUID.randomUUID().toString().take(6)}"
    val ws =
      WorkspaceEntity(
        id = wsId,
        aiId = platform.id,
        aiName = platform.name,
        title = existing?.title ?: platform.name,
        officialUrl = platform.officialUrl,
        currentUrl = existing?.currentUrl ?: platform.officialUrl,
        notes = existing?.notes ?: "",
        isMinimized = false,
        isLocked = existing?.isLocked ?: false,
        compatibilityMode =
          if (settings.value.preferExternalCustomTab) "EXTERNAL_TAB" else platform.compatibility.name,
        lastActiveAt = System.currentTimeMillis(),
      )
    _closedWorkspaceIds.update { it - wsId }
    _optimisticWorkspaces.update { it + (wsId to ws) }
    viewModelScope.launch {
      repository.recordAiOpened(platform.id)
      repository.upsertWorkspace(ws)
      repository.logActivity("Workspace", "Opened ${platform.name}", platform.officialUrl)
      _optimisticWorkspaces.update { it - wsId }
    }
    _activeWorkspaceId.value = wsId
    webViewManager.pauseInactiveWorkspaces(setOfNotNull(wsId, _splitSecondaryWorkspaceId.value))
    return wsId
  }

  fun switchToWorkspace(workspaceId: String) {
    _activeWorkspaceId.value = workspaceId
    _closedWorkspaceIds.update { it - workspaceId }
    workspaces.value.find { it.id == workspaceId }?.let { ws ->
      val updated = ws.copy(isMinimized = false, lastActiveAt = System.currentTimeMillis())
      _optimisticWorkspaces.update { it + (workspaceId to updated) }
      viewModelScope.launch {
        repository.upsertWorkspace(updated)
        _optimisticWorkspaces.update { it - workspaceId }
      }
    }
    webViewManager.pauseInactiveWorkspaces(setOfNotNull(workspaceId, _splitSecondaryWorkspaceId.value))
  }

  fun minimizeWorkspace(workspaceId: String) {
    webViewManager.pauseWorkspace(workspaceId)
    workspaces.value.find { it.id == workspaceId }?.let { ws ->
      val updated = ws.copy(isMinimized = true, lastActiveAt = System.currentTimeMillis())
      _optimisticWorkspaces.update { it + (workspaceId to updated) }
      viewModelScope.launch {
        repository.upsertWorkspace(updated)
        _optimisticWorkspaces.update { it - workspaceId }
      }
    }
    if (_activeWorkspaceId.value == workspaceId) _activeWorkspaceId.value = null
    if (_splitSecondaryWorkspaceId.value == workspaceId) _splitSecondaryWorkspaceId.value = null
  }

  fun closeWorkspace(workspaceId: String) {
    webViewManager.destroyWorkspace(workspaceId)
    _optimisticWorkspaces.update { it - workspaceId }
    _closedWorkspaceIds.update { it + workspaceId }
    viewModelScope.launch {
      repository.deleteWorkspace(workspaceId)
      _closedWorkspaceIds.update { it - workspaceId }
    }
    if (_activeWorkspaceId.value == workspaceId) _activeWorkspaceId.value = null
    if (_splitSecondaryWorkspaceId.value == workspaceId) _splitSecondaryWorkspaceId.value = null
  }

  fun renameWorkspace(workspaceId: String, newTitle: String) =
    viewModelScope.launch {
      workspaces.value.find { it.id == workspaceId }?.let { ws ->
        val updated = ws.copy(title = newTitle.trim().ifBlank { ws.aiName })
        _optimisticWorkspaces.update { it + (workspaceId to updated) }
        repository.upsertWorkspace(updated)
        _optimisticWorkspaces.update { it - workspaceId }
      }
    }

  fun updateWorkspaceNotes(workspaceId: String, notes: String) =
    viewModelScope.launch {
      workspaces.value.find { it.id == workspaceId }?.let { ws ->
        val updated = ws.copy(notes = notes)
        _optimisticWorkspaces.update { it + (workspaceId to updated) }
        repository.upsertWorkspace(updated)
        _optimisticWorkspaces.update { it - workspaceId }
      }
    }

  fun toggleWorkspaceLock(workspaceId: String) =
    viewModelScope.launch {
      workspaces.value.find { it.id == workspaceId }?.let { ws ->
        val updated = ws.copy(isLocked = !ws.isLocked)
        _optimisticWorkspaces.update { it + (workspaceId to updated) }
        repository.upsertWorkspace(updated)
        _optimisticWorkspaces.update { it - workspaceId }
      }
    }

  fun setSplitSecondaryWorkspace(workspaceId: String?) {
    _splitSecondaryWorkspaceId.value = workspaceId
  }

  fun swapSplitWorkspaces() {
    val left = _activeWorkspaceId.value
    val right = _splitSecondaryWorkspaceId.value
    if (left != null && right != null) {
      _activeWorkspaceId.value = right
      _splitSecondaryWorkspaceId.value = left
    }
  }

  fun copyToClipboard(label: String, text: String) {
    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    _clipboardHistory.update { list ->
      (listOf(ClipboardItem(UUID.randomUUID().toString(), label, text)) + list).take(30)
    }
  }

  fun removeClipboardItem(id: String) {
    _clipboardHistory.update { list -> list.filterNot { it.id == id } }
  }

  fun checkUrlAvailability(aiId: String, url: String) =
    viewModelScope.launch {
      val result = urlChecker.check(url)
      _availabilityCache.update { it + (aiId to result) }
    }

  fun saveCustomAi(
    existingId: String?,
    name: String,
    url: String,
    description: String,
    category: AiCategory,
    pricingType: PricingType,
    pricingDetails: String,
    colorHex: Long,
  ) =
    viewModelScope.launch {
      val normalizedUrl = UrlValidator.normalizeUrl(url)
      val id = existingId ?: "custom_${UUID.randomUUID().toString().take(8)}"
      val monogram =
        name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") {
          it.take(1).uppercase()
        }.ifBlank { "AI" }
      repository.saveCustomAi(
        CustomAiEntity(
          id = id,
          name = name.trim(),
          officialUrl = normalizedUrl,
          monogram = monogram,
          brandColorHex = colorHex,
          category = category.name,
          shortDescription = description.trim(),
          pricingType = pricingType.name,
          pricingDetails = pricingDetails.trim().ifBlank { pricingType.badge },
        )
      )
    }

  fun duplicateCustomAi(platform: AiPlatform) {
    saveCustomAi(
      existingId = null,
      name = "${platform.name} Copy",
      url = platform.officialUrl,
      description = platform.shortDescription,
      category = platform.category,
      pricingType = platform.pricingType,
      pricingDetails = platform.pricingDetails,
      colorHex = platform.brandColorHex,
    )
  }

  fun deleteCustomAi(id: String) = viewModelScope.launch { repository.deleteCustomAi(id) }

  fun saveProject(
    existingId: String?,
    name: String,
    url: String,
    aiId: String,
    aiName: String,
    category: String,
    notes: String,
    isFavorite: Boolean = false,
  ) =
    viewModelScope.launch {
      repository.saveProject(
        ProjectEntity(
          id = existingId ?: UUID.randomUUID().toString(),
          name = name.trim(),
          url = UrlValidator.normalizeUrl(url),
          aiId = aiId,
          aiName = aiName,
          category = category,
          notes = notes,
          isFavorite = isFavorite,
        )
      )
    }

  fun toggleProjectFavorite(project: ProjectEntity) =
    viewModelScope.launch { repository.saveProject(project.copy(isFavorite = !project.isFavorite)) }

  fun deleteProject(id: String) = viewModelScope.launch { repository.deleteProject(id) }

  // API Provider & Chat
  fun saveApiProvider(
    existingId: String?,
    displayName: String,
    providerType: ApiProviderType,
    endpoint: String,
    model: String,
    systemPrompt: String,
    apiKey: String,
  ) =
    viewModelScope.launch {
      val id = existingId ?: "prov_${UUID.randomUUID().toString().take(8)}"
      repository.saveApiProvider(
        ApiProviderEntity(
          id = id,
          displayName = displayName.trim().ifBlank { providerType.displayName },
          providerType = providerType.name,
          endpoint = endpoint.trim().ifBlank { providerType.defaultEndpoint },
          model = model.trim().ifBlank { providerType.defaultModel },
          systemPrompt = systemPrompt.trim(),
        ),
        apiKey = apiKey,
      )
    }

  fun deleteApiProvider(id: String) = viewModelScope.launch { repository.deleteApiProvider(id) }

  fun getApiKey(providerId: String): String = repository.secureKeyStore.getApiKey(providerId)

  suspend fun testApiProvider(
    providerType: ApiProviderType,
    endpoint: String,
    model: String,
    apiKey: String,
  ): ApiResponseResult = apiChatClient.testConnection(providerType, endpoint, model, apiKey)

  fun createChatSession(provider: ApiProviderEntity): String {
    val id = "chat_${UUID.randomUUID().toString().take(8)}"
    viewModelScope.launch {
      repository.upsertChatSession(
        ApiChatSessionEntity(
          id = id,
          title = "New ${provider.displayName} Chat",
          providerId = provider.id,
          model = provider.model,
          systemPrompt = provider.systemPrompt,
        )
      )
    }
    return id
  }

  fun updateChatSession(session: ApiChatSessionEntity) =
    viewModelScope.launch { repository.upsertChatSession(session) }

  fun deleteChatSession(sessionId: String) =
    viewModelScope.launch { repository.deleteChatSession(sessionId) }

  fun sendApiMessage(
    session: ApiChatSessionEntity,
    provider: ApiProviderEntity,
    userText: String,
    attachment: ApiAttachmentPayload?,
  ) {
    if (_isApiGenerating.value) return
    val apiKey = repository.secureKeyStore.getApiKey(provider.id)
    val providerType =
      runCatching { ApiProviderType.valueOf(provider.providerType) }.getOrDefault(ApiProviderType.OPENAI)

    viewModelScope.launch {
      val newTitle =
        if (session.title.startsWith("New ") && userText.isNotBlank()) {
          userText.trim().take(36)
        } else {
          session.title
        }
      val persistedSession = session.copy(title = newTitle, updatedAt = System.currentTimeMillis())
      repository.upsertChatSession(persistedSession)

      val userMsg =
        ApiChatMessageEntity(
          id = UUID.randomUUID().toString(),
          sessionId = persistedSession.id,
          role = "user",
          content = userText.trim(),
          attachmentName = attachment?.name,
          attachmentType = attachment?.mimeType,
        )
      repository.upsertChatMessage(userMsg)

      _isApiGenerating.value = true
      val historyEntities = repository.messagesForSession(session.id).first()
      val payloads =
        historyEntities
          .filter { it.role == "user" || it.role == "assistant" }
          .map { msg ->
            ApiChatMessagePayload(
              role = msg.role,
              content = msg.content,
              attachment = if (msg.id == userMsg.id) attachment else null,
            )
          }

      val result =
        apiChatClient.sendMessage(
          providerType = providerType,
          endpoint = provider.endpoint,
          model = session.model.ifBlank { provider.model },
          apiKey = apiKey,
          systemPrompt = session.systemPrompt,
          temperature = session.temperature,
          history = payloads,
        )
      _isApiGenerating.value = false

      when (result) {
        is ApiResponseResult.Success -> {
          repository.upsertChatMessage(
            ApiChatMessageEntity(
              id = UUID.randomUUID().toString(),
              sessionId = session.id,
              role = "assistant",
              content = result.text,
            )
          )
        }
        is ApiResponseResult.Error -> {
          repository.upsertChatMessage(
            ApiChatMessageEntity(
              id = UUID.randomUUID().toString(),
              sessionId = session.id,
              role = "error",
              content = "${result.title}: ${result.detail}",
            )
          )
          if (settings.value.notifyApiErrors) {
            repository.logActivity("API", result.title, result.detail, isError = true)
          }
        }
      }
    }
  }

  fun stopApiGeneration() {
    apiChatClient.cancelActiveCall()
    _isApiGenerating.value = false
  }

  fun regenerateLastResponse(
    session: ApiChatSessionEntity,
    provider: ApiProviderEntity,
    messages: List<ApiChatMessageEntity>,
  ) {
    if (_isApiGenerating.value) return
    val lastUser = messages.lastOrNull { it.role == "user" } ?: return
    val lastMsg = messages.lastOrNull()
    viewModelScope.launch {
      if (lastMsg != null && (lastMsg.role == "assistant" || lastMsg.role == "error")) {
        repository.deleteChatMessage(lastMsg.id)
      }
      _isApiGenerating.value = true
      val apiKey = repository.secureKeyStore.getApiKey(provider.id)
      val providerType =
        runCatching { ApiProviderType.valueOf(provider.providerType) }
          .getOrDefault(ApiProviderType.OPENAI)
      val history =
        repository
          .messagesForSession(session.id)
          .first()
          .filter { it.role == "user" || it.role == "assistant" }
          .map { ApiChatMessagePayload(it.role, it.content) }
      val result =
        apiChatClient.sendMessage(
          providerType = providerType,
          endpoint = provider.endpoint,
          model = session.model.ifBlank { provider.model },
          apiKey = apiKey,
          systemPrompt = session.systemPrompt,
          temperature = session.temperature,
          history = history,
        )
      _isApiGenerating.value = false
      when (result) {
        is ApiResponseResult.Success ->
          repository.upsertChatMessage(
            ApiChatMessageEntity(
              id = UUID.randomUUID().toString(),
              sessionId = session.id,
              role = "assistant",
              content = result.text,
            )
          )
        is ApiResponseResult.Error ->
          repository.upsertChatMessage(
            ApiChatMessageEntity(
              id = UUID.randomUUID().toString(),
              sessionId = session.id,
              role = "error",
              content = "${result.title}: ${result.detail}",
            )
          )
      }
    }
  }

  fun editAndResendUserMessage(
    session: ApiChatSessionEntity,
    provider: ApiProviderEntity,
    targetMessage: ApiChatMessageEntity,
    newText: String,
    allMessages: List<ApiChatMessageEntity>,
  ) {
    viewModelScope.launch {
      val idx = allMessages.indexOfFirst { it.id == targetMessage.id }
      if (idx >= 0) {
        allMessages.drop(idx).forEach { repository.deleteChatMessage(it.id) }
      }
      sendApiMessage(session, provider, newText, null)
    }
  }

  fun deleteChatMessage(messageId: String) =
    viewModelScope.launch { repository.deleteChatMessage(messageId) }

  // Vault Files
  fun importVaultFile(uri: Uri, folder: String = "Root", onResult: (String) -> Unit) =
    viewModelScope.launch {
      val res = repository.importFileFromUri(uri, folder)
      refreshStorageStats()
      onResult(
        res.fold(
          onSuccess = { "Imported ${it.name}" },
          onFailure = { "Import failed: ${it.localizedMessage}" },
        )
      )
    }

  fun createVaultFolder(name: String, parent: String = "Root") =
    viewModelScope.launch { repository.createVaultFolder(name, parent) }

  fun renameVaultFile(file: VaultFileEntity, newName: String) =
    viewModelScope.launch { repository.renameVaultFile(file, newName) }

  fun moveVaultFile(file: VaultFileEntity, folder: String) =
    viewModelScope.launch { repository.moveVaultFile(file, folder) }

  fun deleteVaultFile(file: VaultFileEntity) =
    viewModelScope.launch {
      repository.deleteVaultFile(file)
      refreshStorageStats()
    }

  // Settings & Security
  fun completeTutorial() =
    viewModelScope.launch { repository.settingsStore.setTutorialCompleted(true) }

  fun replayTutorial() =
    viewModelScope.launch { repository.settingsStore.setTutorialCompleted(false) }

  fun setThemeMode(mode: ThemeMode) =
    viewModelScope.launch { repository.settingsStore.setThemeMode(mode) }

  fun setColorPalette(palette: ColorPaletteTheme) =
    viewModelScope.launch { repository.settingsStore.setColorPalette(palette) }

  fun setLanguage(language: AppLanguage) =
    viewModelScope.launch { repository.settingsStore.setLanguage(language) }

  fun setFontScale(scale: FontScaleOption) =
    viewModelScope.launch { repository.settingsStore.setFontScale(scale) }

  fun setIconSize(size: IconSizeOption) =
    viewModelScope.launch { repository.settingsStore.setIconSize(size) }

  fun setReducedMotion(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setReducedMotion(enabled) }

  fun setHighContrast(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setHighContrast(enabled) }

  fun setPreferExternalTab(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setPreferExternalTab(enabled) }

  fun setAutoSaveWorkspaces(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setAutoSaveWorkspaces(enabled) }

  fun setDataSaver(enabled: Boolean) {
    viewModelScope.launch { repository.settingsStore.setDataSaver(enabled) }
    webViewManager.updateDataSaver(enabled)
  }

  fun setNotifyDownloads(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setNotifyDownloads(enabled) }

  fun setNotifyApiErrors(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setNotifyApiErrors(enabled) }

  fun setNotifyNetwork(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setNotifyNetwork(enabled) }

  fun setNotifyBackup(enabled: Boolean) =
    viewModelScope.launch { repository.settingsStore.setNotifyBackup(enabled) }

  fun setAppLockPin(pin: String) {
    repository.secureKeyStore.savePin(pin)
    viewModelScope.launch { repository.settingsStore.setAppLockEnabled(true) }
  }

  fun disableAppLock() {
    repository.secureKeyStore.clearPin()
    _isAppLocked.value = false
    viewModelScope.launch { repository.settingsStore.setAppLockEnabled(false) }
  }

  fun verifyPinAndUnlock(pin: String): Boolean {
    val ok = pin == repository.secureKeyStore.getPin()
    if (ok) _isAppLocked.value = false
    return ok
  }

  fun refreshStorageStats() =
    viewModelScope.launch { _storageStats.value = repository.computeStorageStats() }

  fun clearAppAndWebViewCache() =
    viewModelScope.launch {
      repository.clearAppCache()
      webViewManager.clearAllCaches()
      refreshStorageStats()
      repository.logActivity("Storage", "Cache Cleared", "Application & WebView caches cleared")
    }

  fun clearWorkspaceMetadata() =
    viewModelScope.launch {
      workspaces.value.forEach { webViewManager.destroyWorkspace(it.id) }
      repository.clearAllWorkspaces()
      _activeWorkspaceId.value = null
      _splitSecondaryWorkspaceId.value = null
      repository.logActivity("Workspace", "Cleared Metadata", "All workspace metadata cleared")
    }

  fun markNotificationsRead() = viewModelScope.launch { repository.markAllLogsRead() }

  fun clearNotifications() = viewModelScope.launch { repository.clearAllLogs() }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MainViewModel(application) as T
    }
  }
}
