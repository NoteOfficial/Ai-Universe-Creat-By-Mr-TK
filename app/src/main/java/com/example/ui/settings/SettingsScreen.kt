package com.example.ui.settings

import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.data.local.IconSizeOption
import com.example.data.repository.StorageStats
import com.example.ui.components.formatBytes
import com.example.ui.components.formatTimestamp
import com.example.ui.localization.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  settings: AppSettings,
  strings: UiStrings,
  storageStats: StorageStats,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetColorPalette: (ColorPaletteTheme) -> Unit,
  onSetLanguage: (AppLanguage) -> Unit,
  onSetFontScale: (FontScaleOption) -> Unit,
  onSetIconSize: (IconSizeOption) -> Unit,
  onSetReducedMotion: (Boolean) -> Unit,
  onSetHighContrast: (Boolean) -> Unit,
  onSetPreferExternalTab: (Boolean) -> Unit,
  onSetAutoSaveWorkspaces: (Boolean) -> Unit,
  onSetDataSaver: (Boolean) -> Unit,
  onSetNotifyDownloads: (Boolean) -> Unit,
  onSetNotifyApiErrors: (Boolean) -> Unit,
  onSetNotifyNetwork: (Boolean) -> Unit,
  onSetNotifyBackup: (Boolean) -> Unit,
  onSetAppLockPin: (String) -> Unit,
  onDisableAppLock: () -> Unit,
  onClearCache: () -> Unit,
  onClearWorkspaceMetadata: () -> Unit,
  onOpenCookieManager: () -> Unit,
  onReplayTutorial: () -> Unit,
  onExportBackup: suspend () -> String,
  onImportBackup: suspend (String) -> Result<Int>,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val scope = rememberCoroutineScope()
  var showPinDialog by remember { mutableStateOf(false) }

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          runCatching {
            val json = onExportBackup()
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
          }.onFailure {
            Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          runCatching {
            val text =
              context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: ""
            onImportBackup(text).fold(
              onSuccess = { count ->
                Toast.makeText(context, "Restored $count records", Toast.LENGTH_SHORT).show()
              },
              onFailure = {
                Toast.makeText(context, "Import error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
              },
            )
          }
        }
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Surface(color = neu.card, tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = neu.primaryText)
        }
        Text(strings.settings, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
      }
    }

    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Appearance & 5 Colorful Themes
      SettingsSectionCard(strings.appearance) {
        Text(strings.colorTheme, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ColorPaletteTheme.entries.forEach { palette ->
            val selected = settings.colorPalette == palette
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier =
                Modifier.clip(RoundedCornerShape(14.dp))
                  .background(
                    if (selected) palette.previewPrimary.copy(alpha = 0.14f) else neu.surfaceInset
                  )
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.previewPrimary else neu.border,
                    shape = RoundedCornerShape(14.dp),
                  )
                  .clickable { onSetColorPalette(palette) }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("theme_palette_${palette.name}"),
            ) {
              Box(
                modifier =
                  Modifier.size(34.dp)
                    .clip(CircleShape)
                    .background(
                      Brush.linearGradient(listOf(palette.previewPrimary, palette.previewSecondary))
                    ),
                contentAlignment = Alignment.Center,
              ) {
                if (selected) {
                  Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Text(
                text = strings.paletteName(palette),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) neu.accent else neu.primaryText,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              )
            }
          }
        }

        Text(strings.themeModeLabel, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ThemeMode.entries.forEach { mode ->
            SpringFilterChip(
              selected = settings.themeMode == mode,
              onClick = { onSetThemeMode(mode) },
              label = { Text(strings.themeModeName(mode)) },
            )
          }
        }
        Text(strings.fontSize, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FontScaleOption.entries.forEach { opt ->
            SpringFilterChip(
              selected = settings.fontScale == opt,
              onClick = { onSetFontScale(opt) },
              label = { Text(strings.fontScaleName(opt)) },
            )
          }
        }
        Text(strings.iconSize, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          IconSizeOption.entries.forEach { opt ->
            SpringFilterChip(
              selected = settings.iconSize == opt,
              onClick = { onSetIconSize(opt) },
              label = { Text(strings.iconSizeName(opt)) },
            )
          }
        }
        SettingsToggleRow(strings.reducedMotionTitle, strings.reducedMotionSub, settings.reducedMotion, onSetReducedMotion)
        SettingsToggleRow(strings.highContrastTitle, strings.highContrastSub, settings.highContrast, onSetHighContrast)
      }

      // Language
      SettingsSectionCard(strings.languageLabel) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AppLanguage.entries.forEach { lang ->
            SpringFilterChip(
              selected = settings.language == lang,
              onClick = { onSetLanguage(lang) },
              label = { Text(lang.displayName) },
              modifier = Modifier.testTag("lang_${lang.code}"),
            )
          }
        }
      }

      // Security
      SettingsSectionCard(strings.securityPrivacy) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.appLockPinTitle, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
            Text(
              if (settings.appLockEnabled) strings.appLockEnabledSub else strings.appLockDisabledSub,
              style = MaterialTheme.typography.bodySmall,
              color = neu.secondaryText,
            )
          }
          if (settings.appLockEnabled) {
            SpringTextButton(onClick = onDisableAppLock) { Text(strings.disableLabel, color = ErrorRed) }
          } else {
            SpringButton(onClick = { showPinDialog = true }) { Text(strings.setPin) }
          }
        }
        SpringButton(
          onClick = onOpenCookieManager,
          modifier = Modifier.fillMaxWidth().testTag("settings_cookie_manager_btn"),
        ) {
          Icon(Icons.Rounded.Cookie, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.cookies.manageCookiesBtn)
        }
        SpringOutlinedButton(
          onClick = {
            runCatching {
              CookieManager.getInstance().removeAllCookies(null)
              CookieManager.getInstance().flush()
            }
            Toast.makeText(context, strings.cookiesClearedToast, Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.clearWebViewCookies)
        }
      }

      // Browser & Workspace
      SettingsSectionCard(strings.browserWorkspace) {
        SettingsToggleRow(
          strings.preferCustomTabTitle,
          strings.preferCustomTabSub,
          settings.preferExternalCustomTab,
          onSetPreferExternalTab,
        )
        SettingsToggleRow(
          strings.autoSaveWsTitle,
          strings.autoSaveWsSub,
          settings.autoSaveWorkspaces,
          onSetAutoSaveWorkspaces,
        )
        SettingsToggleRow(
          strings.dataSaverTitle,
          strings.dataSaverSub,
          settings.dataSaver,
          onSetDataSaver,
        )
        SpringOutlinedButton(
          onClick = {
            onClearWorkspaceMetadata()
            Toast.makeText(context, strings.wsMetadataClearedToast, Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.clearWsMetadata)
        }
      }

      // Notifications
      SettingsSectionCard(strings.notificationsTitle) {
        SettingsToggleRow(strings.notifyDownloadsTitle, strings.notifyDownloadsSub, settings.notifyDownloads, onSetNotifyDownloads)
        SettingsToggleRow(strings.notifyApiErrorsTitle, strings.notifyApiErrorsSub, settings.notifyApiErrors, onSetNotifyApiErrors)
        SettingsToggleRow(strings.notifyNetworkTitle, strings.notifyNetworkSub, settings.notifyNetwork, onSetNotifyNetwork)
        SettingsToggleRow(strings.notifyBackupTitle, strings.notifyBackupSub, settings.notifyBackup, onSetNotifyBackup)
      }

      // Storage & Cache
      SettingsSectionCard(strings.storageCacheTitle) {
        Text("${strings.vaultFilesLabel}: ${formatBytes(storageStats.vaultBytes)} (${storageStats.fileCount})", style = MaterialTheme.typography.bodySmall, color = neu.primaryText)
        Text("${strings.appCacheLabel}: ${formatBytes(storageStats.cacheBytes)}", style = MaterialTheme.typography.bodySmall, color = neu.primaryText)
        Text("${strings.databaseLabel}: ${formatBytes(storageStats.databaseBytes)}", style = MaterialTheme.typography.bodySmall, color = neu.primaryText)
        SpringButton(
          onClick = {
            onClearCache()
            Toast.makeText(context, strings.cacheClearedToast, Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.clearCacheBtn)
        }
      }

      // Backup & Restore
      SettingsSectionCard(strings.backupRestoreTitle) {
        Text(
          "${strings.lastBackupLabel}: ${formatTimestamp(settings.lastBackupAt, strings.never)}",
          style = MaterialTheme.typography.bodySmall,
          color = neu.secondaryText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          SpringButton(
            onClick = { exportLauncher.launch("ai_universe_backup.json") },
            modifier = Modifier.weight(1f),
          ) {
            Text(strings.exportJson)
          }
          SpringOutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.weight(1f),
          ) {
            Text(strings.importJson)
          }
        }
      }

      // Tutorial Replay
      SpringOutlinedButton(
        onClick = onReplayTutorial,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Rounded.Replay, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.replayTutorialBtn)
      }
    }
  }

  if (showPinDialog) {
    var pinInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      title = { Text(strings.setPinDialogTitle) },
      text = {
        OutlinedTextField(
          value = pinInput,
          onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
          label = { Text(strings.enter4Digits) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (pinInput.length == 4) {
              onSetAppLockPin(pinInput)
              showPinDialog = false
              Toast.makeText(context, strings.appLockEnabledSub, Toast.LENGTH_SHORT).show()
            }
          },
          enabled = pinInput.length == 4,
        ) {
          Text(strings.enableLock)
        }
      },
      dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) } },
    )
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  val neu = LocalNeuColors.current
  NeuCard(modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = neu.accent, fontWeight = FontWeight.Bold)
      content()
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  val neu = LocalNeuColors.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun PinLockOverlay(
  strings: UiStrings = LocalUiStrings.current,
  onVerifyPin: (String) -> Boolean,
) {
  val neu = LocalNeuColors.current
  var pin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier.fillMaxSize().background(neu.background).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    NeuCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = neu.accent, modifier = Modifier.size(48.dp))
        Text(strings.appLockLockedTitle, style = MaterialTheme.typography.headlineSmall, color = neu.primaryText, fontWeight = FontWeight.Bold)
        OutlinedTextField(
          value = pin,
          onValueChange = {
            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
              pin = it
              error = false
            }
          },
          label = { Text(strings.enter4Digits) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          isError = error,
          singleLine = true,
        )
        if (error) {
          Text(strings.incorrectPin, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
        }
        SpringButton(
          onClick = {
            if (!onVerifyPin(pin)) {
              error = true
              pin = ""
            }
          },
          enabled = pin.length == 4,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.unlockBtn)
        }
      }
    }
  }
}
