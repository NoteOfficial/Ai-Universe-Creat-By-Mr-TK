package com.example.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.localization.AppLanguage
import com.example.ui.theme.ColorPaletteTheme
import com.example.ui.theme.FontScaleOption
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_universe_prefs")

enum class IconSizeOption(val scale: Float) {
  COMPACT(0.88f),
  NORMAL(1.0f),
  LARGE(1.16f),
}

@Immutable
data class AppSettings(
  val tutorialCompleted: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val colorPalette: ColorPaletteTheme = ColorPaletteTheme.COSMIC_VIOLET,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val fontScale: FontScaleOption = FontScaleOption.NORMAL,
  val iconSize: IconSizeOption = IconSizeOption.NORMAL,
  val reducedMotion: Boolean = false,
  val highContrast: Boolean = false,
  val preferExternalCustomTab: Boolean = false,
  val autoSaveWorkspaces: Boolean = true,
  val dataSaver: Boolean = false,
  val appLockEnabled: Boolean = false,
  val notifyDownloads: Boolean = true,
  val notifyApiErrors: Boolean = true,
  val notifyNetwork: Boolean = true,
  val notifyBackup: Boolean = true,
  val lastBackupAt: Long = 0L,
)

class SettingsDataStore(private val context: Context) {
  private val bootPrefs =
    context.getSharedPreferences("ai_universe_boot_prefs", Context.MODE_PRIVATE)

  private object Keys {
    val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val COLOR_PALETTE = stringPreferencesKey("color_palette")
    val LANGUAGE = stringPreferencesKey("language")
    val FONT_SCALE = stringPreferencesKey("font_scale")
    val ICON_SIZE = stringPreferencesKey("icon_size")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val PREFER_EXTERNAL_TAB = booleanPreferencesKey("prefer_external_tab")
    val AUTO_SAVE_WORKSPACES = booleanPreferencesKey("auto_save_workspaces")
    val DATA_SAVER = booleanPreferencesKey("data_saver")
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val NOTIFY_DOWNLOADS = booleanPreferencesKey("notify_downloads")
    val NOTIFY_API_ERRORS = booleanPreferencesKey("notify_api_errors")
    val NOTIFY_NETWORK = booleanPreferencesKey("notify_network")
    val NOTIFY_BACKUP = booleanPreferencesKey("notify_backup")
    val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
  }

  /**
   * Reads critical startup flags synchronously from SharedPreferences so cold start never flashes
   * or re-opens Tutorial after the user already completed/skipped it.
   */
  fun readSyncInitialSettings(): AppSettings {
    val tutorialDone = bootPrefs.getBoolean("tutorial_completed", false)
    val themeMode =
      runCatching {
        ThemeMode.valueOf(bootPrefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
      }.getOrDefault(ThemeMode.SYSTEM)
    val colorPalette =
      runCatching {
        ColorPaletteTheme.valueOf(
          bootPrefs.getString("color_palette", ColorPaletteTheme.COSMIC_VIOLET.name)
            ?: ColorPaletteTheme.COSMIC_VIOLET.name
        )
      }.getOrDefault(ColorPaletteTheme.COSMIC_VIOLET)
    val lang =
      if (bootPrefs.getString("language", AppLanguage.ENGLISH.code) == AppLanguage.BANGLA.code)
        AppLanguage.BANGLA
      else AppLanguage.ENGLISH
    val appLock = bootPrefs.getBoolean("app_lock_enabled", false)
    return AppSettings(
      tutorialCompleted = tutorialDone,
      themeMode = themeMode,
      colorPalette = colorPalette,
      language = lang,
      appLockEnabled = appLock,
    )
  }

  val settingsFlow: Flow<AppSettings> =
    context.dataStore.data.map { prefs ->
      val tutorialDone =
        prefs[Keys.TUTORIAL_COMPLETED] ?: bootPrefs.getBoolean("tutorial_completed", false)
      val themeMode =
        runCatching {
          ThemeMode.valueOf(
            prefs[Keys.THEME_MODE]
              ?: bootPrefs.getString("theme_mode", ThemeMode.SYSTEM.name)
              ?: ThemeMode.SYSTEM.name
          )
        }.getOrDefault(ThemeMode.SYSTEM)
      val colorPalette =
        runCatching {
          ColorPaletteTheme.valueOf(
            prefs[Keys.COLOR_PALETTE]
              ?: bootPrefs.getString("color_palette", ColorPaletteTheme.COSMIC_VIOLET.name)
              ?: ColorPaletteTheme.COSMIC_VIOLET.name
          )
        }.getOrDefault(ColorPaletteTheme.COSMIC_VIOLET)
      val languageCode =
        prefs[Keys.LANGUAGE] ?: bootPrefs.getString("language", AppLanguage.ENGLISH.code)

      AppSettings(
        tutorialCompleted = tutorialDone,
        themeMode = themeMode,
        colorPalette = colorPalette,
        language =
          if (languageCode == AppLanguage.BANGLA.code) AppLanguage.BANGLA else AppLanguage.ENGLISH,
        fontScale =
          runCatching { FontScaleOption.valueOf(prefs[Keys.FONT_SCALE] ?: "NORMAL") }
            .getOrDefault(FontScaleOption.NORMAL),
        iconSize =
          runCatching { IconSizeOption.valueOf(prefs[Keys.ICON_SIZE] ?: "NORMAL") }
            .getOrDefault(IconSizeOption.NORMAL),
        reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
        highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
        preferExternalCustomTab = prefs[Keys.PREFER_EXTERNAL_TAB] ?: false,
        autoSaveWorkspaces = prefs[Keys.AUTO_SAVE_WORKSPACES] ?: true,
        dataSaver = prefs[Keys.DATA_SAVER] ?: false,
        appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: bootPrefs.getBoolean("app_lock_enabled", false),
        notifyDownloads = prefs[Keys.NOTIFY_DOWNLOADS] ?: true,
        notifyApiErrors = prefs[Keys.NOTIFY_API_ERRORS] ?: true,
        notifyNetwork = prefs[Keys.NOTIFY_NETWORK] ?: true,
        notifyBackup = prefs[Keys.NOTIFY_BACKUP] ?: true,
        lastBackupAt = prefs[Keys.LAST_BACKUP_AT] ?: 0L,
      )
    }

  suspend fun setTutorialCompleted(completed: Boolean) {
    bootPrefs.edit().putBoolean("tutorial_completed", completed).commit()
    context.dataStore.edit { it[Keys.TUTORIAL_COMPLETED] = completed }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    bootPrefs.edit().putString("theme_mode", mode.name).apply()
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setColorPalette(palette: ColorPaletteTheme) {
    bootPrefs.edit().putString("color_palette", palette.name).apply()
    context.dataStore.edit { it[Keys.COLOR_PALETTE] = palette.name }
  }

  suspend fun setLanguage(language: AppLanguage) {
    bootPrefs.edit().putString("language", language.code).apply()
    context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setFontScale(scale: FontScaleOption) {
    context.dataStore.edit { it[Keys.FONT_SCALE] = scale.name }
  }

  suspend fun setIconSize(size: IconSizeOption) {
    context.dataStore.edit { it[Keys.ICON_SIZE] = size.name }
  }

  suspend fun setReducedMotion(enabled: Boolean) {
    context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
  }

  suspend fun setHighContrast(enabled: Boolean) {
    context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
  }

  suspend fun setPreferExternalTab(enabled: Boolean) {
    context.dataStore.edit { it[Keys.PREFER_EXTERNAL_TAB] = enabled }
  }

  suspend fun setAutoSaveWorkspaces(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AUTO_SAVE_WORKSPACES] = enabled }
  }

  suspend fun setDataSaver(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DATA_SAVER] = enabled }
  }

  suspend fun setAppLockEnabled(enabled: Boolean) {
    bootPrefs.edit().putBoolean("app_lock_enabled", enabled).apply()
    context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
  }

  suspend fun setNotifyDownloads(enabled: Boolean) {
    context.dataStore.edit { it[Keys.NOTIFY_DOWNLOADS] = enabled }
  }

  suspend fun setNotifyApiErrors(enabled: Boolean) {
    context.dataStore.edit { it[Keys.NOTIFY_API_ERRORS] = enabled }
  }

  suspend fun setNotifyNetwork(enabled: Boolean) {
    context.dataStore.edit { it[Keys.NOTIFY_NETWORK] = enabled }
  }

  suspend fun setNotifyBackup(enabled: Boolean) {
    context.dataStore.edit { it[Keys.NOTIFY_BACKUP] = enabled }
  }

  suspend fun updateLastBackupAt(timestamp: Long) {
    context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = timestamp }
  }
}
