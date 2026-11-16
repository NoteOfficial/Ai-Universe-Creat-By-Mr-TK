package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.AppDatabase
import com.example.data.local.SecureKeyStore
import com.example.data.local.SettingsDataStore
import com.example.data.network.ApiChatClient
import com.example.data.network.UrlAvailabilityChecker
import com.example.data.repository.AiUniverseRepository
import com.example.ui.workspace.WorkspaceWebViewManager

class AiUniverseApp : Application(), ImageLoaderFactory {
  lateinit var database: AppDatabase
    private set

  lateinit var settingsDataStore: SettingsDataStore
    private set

  lateinit var secureKeyStore: SecureKeyStore
    private set

  lateinit var repository: AiUniverseRepository
    private set

  lateinit var webViewManager: WorkspaceWebViewManager
    private set

  lateinit var apiChatClient: ApiChatClient
    private set

  lateinit var urlAvailabilityChecker: UrlAvailabilityChecker
    private set

  override fun onCreate() {
    super.onCreate()
    database = AppDatabase.getInstance(this)
    settingsDataStore = SettingsDataStore(this)
    secureKeyStore = SecureKeyStore(this)
    repository = AiUniverseRepository(this, database, settingsDataStore, secureKeyStore)
    webViewManager = WorkspaceWebViewManager(this)
    apiChatClient = ApiChatClient()
    urlAvailabilityChecker = UrlAvailabilityChecker()
  }

  override fun newImageLoader(): ImageLoader =
    ImageLoader.Builder(this)
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(0.25)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("coil_logo_cache"))
          .maxSizeBytes(32L * 1024L * 1024L)
          .build()
      }
      .crossfade(120)
      .allowHardware(true)
      .respectCacheHeaders(false)
      .build()
}
