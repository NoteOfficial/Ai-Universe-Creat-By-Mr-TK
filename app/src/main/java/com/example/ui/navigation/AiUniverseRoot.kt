package com.example.ui.navigation

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiPlatform
import com.example.data.model.AiRegistry
import com.example.security.AppIntegrityGuard
import com.example.ui.apichat.ApiChatScreen
import com.example.ui.details.AiDetailsScreen
import com.example.ui.files.FileManagerScreen
import com.example.ui.home.HomeScreen
import com.example.ui.localization.LocalUiStrings
import com.example.ui.localization.stringsFor
import com.example.ui.profile.*
import com.example.ui.projects.ProjectManagerScreen
import com.example.ui.settings.PinLockOverlay
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AiUniverseTheme
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.NeuCard
import com.example.ui.tools.*
import com.example.ui.tutorial.TutorialScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.workspace.CustomTabHelper
import com.example.ui.workspace.FloatingMinimizedOrbsOverlay
import com.example.ui.workspace.WorkspaceScreen
import com.example.ui.workspaces.WorkspaceManagerScreen

sealed interface AppRoute {
  data object Home : AppRoute

  data object Profile : AppRoute

  data class Details(val aiId: String) : AppRoute

  data class Workspace(val workspaceId: String) : AppRoute

  data object ApiChat : AppRoute

  data object FileManager : AppRoute

  data object ProjectManager : AppRoute

  data object WorkspaceManager : AppRoute

  data class AddEditCustomAi(val aiId: String? = null) : AppRoute

  data object CompareAi : AppRoute

  data object Clipboard : AppRoute

  data object Notifications : AppRoute

  data object Settings : AppRoute

  data object ReportProblem : AppRoute

  data object About : AppRoute

  data object Privacy : AppRoute

  data object Terms : AppRoute

  data class CookieManager(
    val initialUrl: String = "",
    val returnRoute: AppRoute = Settings,
  ) : AppRoute
}

@Composable
fun AiUniverseRoot(viewModel: MainViewModel) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val platforms by viewModel.platforms.collectAsStateWithLifecycle()
  val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
  val projects by viewModel.projects.collectAsStateWithLifecycle()
  val apiProviders by viewModel.apiProviders.collectAsStateWithLifecycle()
  val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
  val vaultFiles by viewModel.vaultFiles.collectAsStateWithLifecycle()
  val logs by viewModel.activityLogs.collectAsStateWithLifecycle()
  val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
  val clipboardHistory by viewModel.clipboardHistory.collectAsStateWithLifecycle()
  val splitSecondaryId by viewModel.splitSecondaryWorkspaceId.collectAsStateWithLifecycle()
  val isApiGenerating by viewModel.isApiGenerating.collectAsStateWithLifecycle()
  val availabilityCache by viewModel.availabilityCache.collectAsStateWithLifecycle()
  val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
  val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val selectedPricing by viewModel.selectedPricing.collectAsStateWithLifecycle()
  val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
  val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

  val strings = remember(settings.language) { stringsFor(settings.language) }
  var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Home) }
  var previousRoute by remember { mutableStateOf<AppRoute>(AppRoute.Home) }
  val homeListState = rememberLazyListState()

  val context = LocalContext.current
  val integrity = remember(context) { AppIntegrityGuard.verify(context) }

  fun navigateTo(route: AppRoute) {
    if (currentRoute != route) {
      previousRoute = currentRoute
      currentRoute = route
    }
  }

  fun launchAiWorkspace(platform: AiPlatform) {
    val wsId = viewModel.openOrSwitchWorkspace(platform)
    navigateTo(AppRoute.Workspace(wsId))
  }

  CompositionLocalProvider(LocalUiStrings provides strings) {
    AiUniverseTheme(
      themeMode = settings.themeMode,
      colorPalette = settings.colorPalette,
      highContrast = settings.highContrast,
      fontScale = settings.fontScale,
      reducedMotion = settings.reducedMotion,
    ) {
      val neu = LocalNeuColors.current

      if (!integrity.isTrusted) {
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
              Icon(Icons.Rounded.GppBad, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(52.dp))
              Text(
                text = strings.integrityFailedTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = neu.primaryText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
              Text(
                text = "${strings.integrityFailedDesc}\n(${integrity.reason})",
                style = MaterialTheme.typography.bodyMedium,
                color = neu.secondaryText,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
        return@AiUniverseTheme
      }

      if (isAppLocked) {
        PinLockOverlay(strings = strings, onVerifyPin = { pin -> viewModel.verifyPinAndUnlock(pin) })
        return@AiUniverseTheme
      }

      if (!settings.tutorialCompleted) {
        TutorialScreen(
          strings = strings,
          reducedMotion = settings.reducedMotion,
          onFinish = { viewModel.completeTutorial() },
        )
        return@AiUniverseTheme
      }

      if (currentRoute is AppRoute.Profile) {
        BackHandler { currentRoute = AppRoute.Home }
      }

      val isTopLevelTab = currentRoute is AppRoute.Home || currentRoute is AppRoute.Profile

      val navItems =
        listOf(
          Triple("home", strings.navHome, Icons.Rounded.Home to Icons.Outlined.Home),
          Triple("arena_ai", strings.navArena, Icons.Rounded.AutoAwesome to Icons.Outlined.AutoAwesome),
          Triple("v0", strings.navV0, Icons.Rounded.Code to Icons.Outlined.Code),
          Triple("google_ai_studio", strings.navStudio, Icons.Rounded.Science to Icons.Outlined.Science),
          Triple("profile", strings.navProfile, Icons.Rounded.Person to Icons.Outlined.Person),
        )

      fun onNavItemSelected(key: String) {
        when (key) {
          "home" -> navigateTo(AppRoute.Home)
          "profile" -> navigateTo(AppRoute.Profile)
          else -> {
            val targetPlatform =
              platforms.find { it.id == key } ?: AiRegistry.findById(key)
            if (targetPlatform != null) {
              launchAiWorkspace(targetPlatform)
            }
          }
        }
      }

      val selectedNavKey =
        when (val r = currentRoute) {
          AppRoute.Home -> "home"
          AppRoute.Profile -> "profile"
          is AppRoute.Workspace -> {
            val ws = workspaces.find { it.id == r.workspaceId }
            when (ws?.aiId) {
              "arena_ai" -> "arena_ai"
              "v0" -> "v0"
              "google_ai_studio" -> "google_ai_studio"
              else -> ""
            }
          }
          else -> ""
        }

      BoxWithConstraints(modifier = Modifier.fillMaxSize().background(neu.background)) {
        val useNavRail = maxWidth >= 720.dp && isTopLevelTab

        Scaffold(
          containerColor = neu.background,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          bottomBar = {
            if (isTopLevelTab && !useNavRail) {
              NavigationBar(
                containerColor = neu.card,
                tonalElevation = 6.dp,
                windowInsets = WindowInsets.navigationBars,
              ) {
                navItems.forEach { (key, label, icons) ->
                  val selected = selectedNavKey == key
                  val iconScale by
                    animateFloatAsState(
                      targetValue = if (selected && !settings.reducedMotion) 1.06f else 1f,
                      animationSpec =
                        if (settings.reducedMotion) snap()
                        else tween(durationMillis = 150, easing = FastOutSlowInEasing),
                      label = "nav_icon_scale_$key",
                    )
                  NavigationBarItem(
                    selected = selected,
                    onClick = { onNavItemSelected(key) },
                    icon = {
                      Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label,
                        modifier =
                          Modifier.size((22f * settings.iconSize.scale).dp).graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                          },
                      )
                    },
                    label = {
                      Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                      )
                    },
                    modifier = Modifier.testTag("nav_$key"),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavRail) {
              NavigationRail(
                containerColor = neu.card,
                windowInsets = WindowInsets.safeDrawing,
              ) {
                Spacer(Modifier.height(12.dp))
                navItems.forEach { (key, label, icons) ->
                  val selected = selectedNavKey == key
                  val iconScale by
                    animateFloatAsState(
                      targetValue = if (selected && !settings.reducedMotion) 1.06f else 1f,
                      animationSpec =
                        if (settings.reducedMotion) snap()
                        else tween(durationMillis = 150, easing = FastOutSlowInEasing),
                      label = "rail_icon_scale_$key",
                    )
                  NavigationRailItem(
                    selected = selected,
                    onClick = { onNavItemSelected(key) },
                    icon = {
                      Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label,
                        modifier = Modifier.graphicsLayer {
                          scaleX = iconScale
                          scaleY = iconScale
                        },
                      )
                    },
                    label = { Text(label, maxLines = 1) },
                  )
                }
              }
            }

            Box(
              modifier =
                Modifier.weight(1f)
                  .fillMaxHeight()
                  .let { if (isTopLevelTab) it.windowInsetsPadding(WindowInsets.statusBars) else it }
            ) {
              val animDuration = if (settings.reducedMotion) 0 else 150
              AnimatedContent(
                targetState = currentRoute,
                contentKey = { route ->
                  when (route) {
                    AppRoute.Home -> "home"
                    AppRoute.Profile -> "profile"
                    is AppRoute.Details -> "details_${route.aiId}"
                    is AppRoute.Workspace -> "ws_${route.workspaceId}"
                    AppRoute.ApiChat -> "apichat"
                    AppRoute.FileManager -> "files"
                    AppRoute.ProjectManager -> "projects"
                    AppRoute.WorkspaceManager -> "workspaces"
                    is AppRoute.AddEditCustomAi -> "customai_${route.aiId.orEmpty()}"
                    AppRoute.CompareAi -> "compare"
                    AppRoute.Clipboard -> "clipboard"
                    AppRoute.Notifications -> "notifications"
                    AppRoute.Settings -> "settings"
                    AppRoute.ReportProblem -> "report"
                    AppRoute.About -> "about"
                    AppRoute.Privacy -> "privacy"
                    AppRoute.Terms -> "terms"
                    is AppRoute.CookieManager -> "cookies_${route.initialUrl}"
                  }
                },
                transitionSpec = {
                  fadeIn(animationSpec = tween(animDuration, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween((animDuration / 2).coerceAtLeast(1)))
                },
                label = "route_animated_content",
              ) { route ->
                when (route) {
                  AppRoute.Home -> {
                    HomeScreen(
                      strings = strings,
                      platforms = platforms,
                      workspaces = workspaces,
                      projects = projects,
                      logs = logs,
                      networkStatus = networkStatus,
                      searchQuery = searchQuery,
                      selectedCategory = selectedCategory,
                      selectedPricing = selectedPricing,
                      sortOption = sortOption,
                      recentSearches = recentSearches,
                      listState = homeListState,
                      onSearchChange = viewModel::setSearchQuery,
                      onCommitSearch = viewModel::commitSearchHistory,
                      onSelectCategory = viewModel::setSelectedCategory,
                      onSelectPricing = viewModel::setSelectedPricing,
                      onSelectSort = viewModel::setSortOption,
                      onOpenAiDetails = { ai -> navigateTo(AppRoute.Details(ai.id)) },
                      onQuickStartAi = { ai -> launchAiWorkspace(ai) },
                      onToggleFavorite = viewModel::toggleFavorite,
                      onTogglePin = viewModel::togglePin,
                      onReorderAi = viewModel::movePlatformOrder,
                      onRestoreWorkspace = { wsId ->
                        viewModel.switchToWorkspace(wsId)
                        navigateTo(AppRoute.Workspace(wsId))
                      },
                      onCloseWorkspace = viewModel::closeWorkspace,
                      onOpenApiChat = { navigateTo(AppRoute.ApiChat) },
                      onOpenFileManager = { navigateTo(AppRoute.FileManager) },
                      onOpenDownloads = {
                        runCatching {
                          context.startActivity(
                            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          )
                        }.onFailure { navigateTo(AppRoute.FileManager) }
                      },
                      onOpenProjectManager = { navigateTo(AppRoute.ProjectManager) },
                      onOpenWorkspaceManager = { navigateTo(AppRoute.WorkspaceManager) },
                      onOpenAddCustomAi = { navigateTo(AppRoute.AddEditCustomAi(null)) },
                      onOpenComparison = { navigateTo(AppRoute.CompareAi) },
                      onOpenClipboard = { navigateTo(AppRoute.Clipboard) },
                      onOpenNotifications = { navigateTo(AppRoute.Notifications) },
                      onOpenSettings = { navigateTo(AppRoute.Settings) },
                    )
                  }

                  AppRoute.Profile -> {
                    ProfileScreen(
                      strings = strings,
                      onOpenSettings = { navigateTo(AppRoute.Settings) },
                      onOpenAbout = { navigateTo(AppRoute.About) },
                      onReplayTutorial = { viewModel.replayTutorial() },
                      onOpenReportProblem = { navigateTo(AppRoute.ReportProblem) },
                      onOpenPrivacyPolicy = { navigateTo(AppRoute.Privacy) },
                      onOpenTerms = { navigateTo(AppRoute.Terms) },
                    )
                  }

                  is AppRoute.Details -> {
                    val platform = platforms.find { it.id == route.aiId } ?: platforms.first()
                    AiDetailsScreen(
                      platform = platform,
                      strings = strings,
                      availabilityResult = availabilityCache[platform.id],
                      onBack = { currentRoute = AppRoute.Home },
                      onStartNow = { launchAiWorkspace(platform) },
                      onOpenOfficialExternal = { url -> CustomTabHelper.openOfficialWebsite(context, url) },
                      onToggleFavorite = { viewModel.toggleFavorite(platform.id) },
                      onTogglePin = { viewModel.togglePin(platform.id) },
                      onCheckAvailability = { viewModel.checkUrlAvailability(platform.id, platform.officialUrl) },
                      onEditCustomAi =
                        if (platform.isCustom) {
                          { navigateTo(AppRoute.AddEditCustomAi(platform.id)) }
                        } else null,
                      onDuplicateCustomAi =
                        if (platform.isCustom) {
                          {
                            viewModel.duplicateCustomAi(platform)
                            Toast.makeText(context, "Duplicated ${platform.name}", Toast.LENGTH_SHORT).show()
                          }
                        } else null,
                      onDeleteCustomAi =
                        if (platform.isCustom) {
                          {
                            viewModel.deleteCustomAi(platform.id)
                            currentRoute = AppRoute.Home
                          }
                        } else null,
                    )
                  }

                  is AppRoute.Workspace -> {
                    val primary = workspaces.find { it.id == route.workspaceId }
                    if (primary == null) {
                      LaunchedEffect(route.workspaceId) { currentRoute = AppRoute.Home }
                    } else {
                      val secondary = splitSecondaryId?.let { sid -> workspaces.find { it.id == sid } }
                      WorkspaceScreen(
                        primaryWorkspace = primary,
                        secondaryWorkspace = secondary,
                        allPlatforms = platforms,
                        allWorkspaces = workspaces,
                        webViewManager = viewModel.webViewManager,
                        strings = strings,
                        isConnected = networkStatus.isConnected,
                        dataSaver = settings.dataSaver,
                        preferExternalTab = settings.preferExternalCustomTab,
                        onCopyUrl = viewModel::copyToClipboard,
                        onSaveProject = { name, url, aiId, aiName, cat ->
                          viewModel.saveProject(null, name, url, aiId, aiName, cat, "Saved from workspace")
                        },
                        onUpdateNotes = viewModel::updateWorkspaceNotes,
                        onToggleLock = viewModel::toggleWorkspaceLock,
                        onMinimizeWorkspace = { wsId ->
                          viewModel.minimizeWorkspace(wsId)
                          currentRoute = AppRoute.Home
                        },
                        onCloseWorkspace = { wsId ->
                          viewModel.closeWorkspace(wsId)
                          currentRoute = AppRoute.Home
                        },
                        onExitView = {
                          currentRoute =
                            if (previousRoute is AppRoute.Workspace || previousRoute is AppRoute.Details) {
                              AppRoute.Home
                            } else {
                              previousRoute
                            }
                        },
                        onOpenFileManager = { navigateTo(AppRoute.FileManager) },
                        onOpenWorkspaceManager = { navigateTo(AppRoute.WorkspaceManager) },
                        onOpenCookieManager = { url ->
                          navigateTo(AppRoute.CookieManager(initialUrl = url, returnRoute = AppRoute.Workspace(primary.id)))
                        },
                        onSelectSecondaryWorkspace = viewModel::setSplitSecondaryWorkspace,
                        onSwapSplit = viewModel::swapSplitWorkspaces,
                      )
                    }
                  }

                  AppRoute.ApiChat -> {
                    ApiChatScreen(
                      providers = apiProviders,
                      sessions = chatSessions,
                      isGenerating = isApiGenerating,
                      messagesProvider = viewModel.repository::messagesForSession,
                      getApiKey = viewModel::getApiKey,
                      onTestProvider = viewModel::testApiProvider,
                      onSaveProvider = viewModel::saveApiProvider,
                      onDeleteProvider = viewModel::deleteApiProvider,
                      onCreateSession = viewModel::createChatSession,
                      onUpdateSession = viewModel::updateChatSession,
                      onDeleteSession = viewModel::deleteChatSession,
                      onSendMessage = viewModel::sendApiMessage,
                      onStopGeneration = viewModel::stopApiGeneration,
                      onRegenerate = viewModel::regenerateLastResponse,
                      onEditMessage = viewModel::editAndResendUserMessage,
                      onDeleteMessage = viewModel::deleteChatMessage,
                      onCopyText = viewModel::copyToClipboard,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.FileManager -> {
                    FileManagerScreen(
                      files = vaultFiles,
                      storageStats = storageStats,
                      onImportUri = { uri: Uri, folder: String, cb: (String) -> Unit ->
                        viewModel.importVaultFile(uri, folder, cb)
                      },
                      onCreateFolder = viewModel::createVaultFolder,
                      onRenameFile = viewModel::renameVaultFile,
                      onMoveFile = viewModel::moveVaultFile,
                      onDeleteFile = viewModel::deleteVaultFile,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.ProjectManager -> {
                    ProjectManagerScreen(
                      projects = projects,
                      platforms = platforms,
                      onSaveProject = viewModel::saveProject,
                      onToggleFavorite = viewModel::toggleProjectFavorite,
                      onDeleteProject = viewModel::deleteProject,
                      onCopyUrl = viewModel::copyToClipboard,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.WorkspaceManager -> {
                    WorkspaceManagerScreen(
                      workspaces = workspaces,
                      platforms = platforms,
                      onOpenOrRestore = { wsId ->
                        viewModel.switchToWorkspace(wsId)
                        navigateTo(AppRoute.Workspace(wsId))
                      },
                      onMinimize = viewModel::minimizeWorkspace,
                      onRename = viewModel::renameWorkspace,
                      onUpdateNotes = viewModel::updateWorkspaceNotes,
                      onToggleLock = viewModel::toggleWorkspaceLock,
                      onClose = viewModel::closeWorkspace,
                      onClearAllMetadata = viewModel::clearWorkspaceMetadata,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  is AppRoute.AddEditCustomAi -> {
                    val target = route.aiId?.let { id -> platforms.find { it.id == id } }
                    AddEditCustomAiScreen(
                      existing = target,
                      onSave = viewModel::saveCustomAi,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.CompareAi -> {
                    AiComparisonScreen(
                      platforms = platforms,
                      onLaunchAi = { launchAiWorkspace(it) },
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.Clipboard -> {
                    GlobalClipboardScreen(
                      items = clipboardHistory,
                      onCopy = viewModel::copyToClipboard,
                      onDelete = viewModel::removeClipboardItem,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.Notifications -> {
                    NotificationsCenterScreen(
                      logs = logs,
                      onMarkAllRead = viewModel::markNotificationsRead,
                      onClearAll = viewModel::clearNotifications,
                      onBack = { currentRoute = AppRoute.Home },
                    )
                  }

                  AppRoute.Settings -> {
                    SettingsScreen(
                      settings = settings,
                      strings = strings,
                      storageStats = storageStats,
                      onSetThemeMode = viewModel::setThemeMode,
                      onSetColorPalette = viewModel::setColorPalette,
                      onSetLanguage = viewModel::setLanguage,
                      onSetFontScale = viewModel::setFontScale,
                      onSetIconSize = viewModel::setIconSize,
                      onSetReducedMotion = viewModel::setReducedMotion,
                      onSetHighContrast = viewModel::setHighContrast,
                      onSetPreferExternalTab = viewModel::setPreferExternalTab,
                      onSetAutoSaveWorkspaces = viewModel::setAutoSaveWorkspaces,
                      onSetDataSaver = viewModel::setDataSaver,
                      onSetNotifyDownloads = viewModel::setNotifyDownloads,
                      onSetNotifyApiErrors = viewModel::setNotifyApiErrors,
                      onSetNotifyNetwork = viewModel::setNotifyNetwork,
                      onSetNotifyBackup = viewModel::setNotifyBackup,
                      onSetAppLockPin = viewModel::setAppLockPin,
                      onDisableAppLock = viewModel::disableAppLock,
                      onClearCache = viewModel::clearAppAndWebViewCache,
                      onClearWorkspaceMetadata = viewModel::clearWorkspaceMetadata,
                      onOpenCookieManager = {
                        navigateTo(AppRoute.CookieManager(initialUrl = "", returnRoute = AppRoute.Settings))
                      },
                      onReplayTutorial = viewModel::replayTutorial,
                      onExportBackup = viewModel.repository::exportBackupJson,
                      onImportBackup = viewModel.repository::importBackupJson,
                      onBack = { currentRoute = AppRoute.Profile },
                    )
                  }

                  AppRoute.ReportProblem -> {
                    ReportProblemScreen(onBack = { currentRoute = AppRoute.Profile })
                  }

                  AppRoute.About -> {
                    AboutAppScreen(onBack = { currentRoute = AppRoute.Profile })
                  }

                  AppRoute.Privacy -> {
                    PrivacyPolicyScreen(onBack = { currentRoute = AppRoute.Profile })
                  }

                  AppRoute.Terms -> {
                    TermsConditionsScreen(onBack = { currentRoute = AppRoute.Profile })
                  }

                  is AppRoute.CookieManager -> {
                    CookieManagerScreen(
                      initialUrl = route.initialUrl,
                      onCopyText = viewModel::copyToClipboard,
                      onBack = { currentRoute = route.returnRoute },
                    )
                  }
                }
              }
            }
          }
        }

        // Floating Circular Minimized Workspace Orbs
        val minimizedList = remember(workspaces) { workspaces.filter { it.isMinimized } }
        FloatingMinimizedOrbsOverlay(
          minimizedWorkspaces = minimizedList,
          platforms = platforms,
          onRestoreWorkspace = { wsId ->
            viewModel.switchToWorkspace(wsId)
            navigateTo(AppRoute.Workspace(wsId))
          },
          onCloseWorkspace = viewModel::closeWorkspace,
        )
      }
    }
  }
}
