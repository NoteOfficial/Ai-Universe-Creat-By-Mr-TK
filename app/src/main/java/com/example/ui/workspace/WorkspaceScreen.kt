package com.example.ui.workspace

import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.WorkspaceEntity
import com.example.data.model.AiPlatform
import com.example.data.network.OriginCheckStatus
import com.example.ui.components.AiBrandBadge
import com.example.ui.localization.UiStrings
import com.example.ui.theme.*

@Composable
fun WorkspaceScreen(
  primaryWorkspace: WorkspaceEntity,
  secondaryWorkspace: WorkspaceEntity?,
  allPlatforms: List<AiPlatform>,
  allWorkspaces: List<WorkspaceEntity>,
  webViewManager: WorkspaceWebViewManager,
  strings: UiStrings,
  isConnected: Boolean,
  dataSaver: Boolean,
  preferExternalTab: Boolean,
  onCopyUrl: (label: String, url: String) -> Unit,
  onSaveProject: (name: String, url: String, aiId: String, aiName: String, category: String) -> Unit,
  onUpdateNotes: (workspaceId: String, notes: String) -> Unit,
  onToggleLock: (workspaceId: String) -> Unit,
  onMinimizeWorkspace: (workspaceId: String) -> Unit,
  onCloseWorkspace: (workspaceId: String) -> Unit,
  onExitView: () -> Unit,
  onOpenFileManager: () -> Unit,
  onOpenWorkspaceManager: () -> Unit,
  onOpenCookieManager: (currentUrl: String) -> Unit,
  onSelectSecondaryWorkspace: (String?) -> Unit,
  onSwapSplit: () -> Unit,
) {
  BackHandler {
    if (!webViewManager.goBack(primaryWorkspace.id)) {
      onExitView()
    }
  }

  val neu = LocalNeuColors.current
  var isFullscreen by remember { mutableStateOf(false) }
  var splitRatio by remember { mutableFloatStateOf(0.5f) }
  var showSplitPicker by remember { mutableStateOf(false) }

  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    val canSplitScreen = maxWidth >= 720.dp
    val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

    if (canSplitScreen && secondaryWorkspace != null) {
      Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(splitRatio.coerceIn(0.25f, 0.75f)).fillMaxHeight()) {
          SingleWorkspacePane(
            workspace = primaryWorkspace,
            platform = allPlatforms.find { it.id == primaryWorkspace.aiId },
            webViewManager = webViewManager,
            strings = strings,
            isConnected = isConnected,
            dataSaver = dataSaver,
            preferExternalTab = preferExternalTab,
            isFullscreen = isFullscreen,
            canSplitScreen = true,
            isSplitActive = true,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onCopyUrl = onCopyUrl,
            onSaveProject = onSaveProject,
            onUpdateNotes = onUpdateNotes,
            onToggleLock = onToggleLock,
            onMinimize = { onMinimizeWorkspace(primaryWorkspace.id) },
            onClose = { onCloseWorkspace(primaryWorkspace.id) },
            onExitView = onExitView,
            onOpenFileManager = onOpenFileManager,
            onOpenWorkspaceManager = onOpenWorkspaceManager,
            onOpenCookieManager = onOpenCookieManager,
            onRequestSplit = { showSplitPicker = true },
            onSwapSplit = onSwapSplit,
          )
        }

        // Adjustable Divider
        Box(
          modifier =
            Modifier.width(10.dp)
              .fillMaxHeight()
              .background(neu.border)
              .pointerInput(totalWidthPx) {
                detectDragGestures { change, dragAmount ->
                  change.consume()
                  val delta = dragAmount.x / totalWidthPx
                  splitRatio = (splitRatio + delta).coerceIn(0.28f, 0.72f)
                }
              },
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.width(4.dp)
                .height(36.dp)
                .background(neu.accent, RoundedCornerShape(2.dp))
          )
        }

        Box(modifier = Modifier.weight((1f - splitRatio).coerceIn(0.25f, 0.75f)).fillMaxHeight()) {
          SingleWorkspacePane(
            workspace = secondaryWorkspace,
            platform = allPlatforms.find { it.id == secondaryWorkspace.aiId },
            webViewManager = webViewManager,
            strings = strings,
            isConnected = isConnected,
            dataSaver = dataSaver,
            preferExternalTab = preferExternalTab,
            isFullscreen = isFullscreen,
            canSplitScreen = true,
            isSplitActive = true,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onCopyUrl = onCopyUrl,
            onSaveProject = onSaveProject,
            onUpdateNotes = onUpdateNotes,
            onToggleLock = onToggleLock,
            onMinimize = { onMinimizeWorkspace(secondaryWorkspace.id) },
            onClose = { onSelectSecondaryWorkspace(null) },
            onExitView = { onSelectSecondaryWorkspace(null) },
            onOpenFileManager = onOpenFileManager,
            onOpenWorkspaceManager = onOpenWorkspaceManager,
            onOpenCookieManager = onOpenCookieManager,
            onRequestSplit = { showSplitPicker = true },
            onSwapSplit = onSwapSplit,
          )
        }
      }
    } else {
      SingleWorkspacePane(
        workspace = primaryWorkspace,
        platform = allPlatforms.find { it.id == primaryWorkspace.aiId },
        webViewManager = webViewManager,
        strings = strings,
        isConnected = isConnected,
        dataSaver = dataSaver,
        preferExternalTab = preferExternalTab,
        isFullscreen = isFullscreen,
        canSplitScreen = canSplitScreen,
        isSplitActive = false,
        onToggleFullscreen = { isFullscreen = !isFullscreen },
        onCopyUrl = onCopyUrl,
        onSaveProject = onSaveProject,
        onUpdateNotes = onUpdateNotes,
        onToggleLock = onToggleLock,
        onMinimize = { onMinimizeWorkspace(primaryWorkspace.id) },
        onClose = { onCloseWorkspace(primaryWorkspace.id) },
        onExitView = onExitView,
        onOpenFileManager = onOpenFileManager,
        onOpenWorkspaceManager = onOpenWorkspaceManager,
        onOpenCookieManager = onOpenCookieManager,
        onRequestSplit = { showSplitPicker = true },
        onSwapSplit = onSwapSplit,
      )
    }
  }

  if (showSplitPicker) {
    val others = allWorkspaces.filter { it.id != primaryWorkspace.id }
    AlertDialog(
      onDismissRequest = { showSplitPicker = false },
      title = { Text("Split-Screen Second Window") },
      text = {
        if (others.isEmpty()) {
          Text("Open another AI workspace first to compare or work side-by-side.")
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            others.forEach { ws ->
              OutlinedButton(
                onClick = {
                  onSelectSecondaryWorkspace(ws.id)
                  showSplitPicker = false
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(ws.title)
              }
            }
          }
        }
      },
      confirmButton = {
        if (secondaryWorkspace != null) {
          TextButton(
            onClick = {
              onSelectSecondaryWorkspace(null)
              showSplitPicker = false
            }
          ) {
            Text("Close Split")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showSplitPicker = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SingleWorkspacePane(
  workspace: WorkspaceEntity,
  platform: AiPlatform?,
  webViewManager: WorkspaceWebViewManager,
  strings: UiStrings,
  isConnected: Boolean,
  dataSaver: Boolean,
  preferExternalTab: Boolean,
  isFullscreen: Boolean,
  canSplitScreen: Boolean,
  isSplitActive: Boolean,
  onToggleFullscreen: () -> Unit,
  onCopyUrl: (label: String, url: String) -> Unit,
  onSaveProject: (name: String, url: String, aiId: String, aiName: String, category: String) -> Unit,
  onUpdateNotes: (workspaceId: String, notes: String) -> Unit,
  onToggleLock: (workspaceId: String) -> Unit,
  onMinimize: () -> Unit,
  onClose: () -> Unit,
  onExitView: () -> Unit,
  onOpenFileManager: () -> Unit,
  onOpenWorkspaceManager: () -> Unit,
  onOpenCookieManager: (currentUrl: String) -> Unit,
  onRequestSplit: () -> Unit,
  onSwapSplit: () -> Unit,
) {
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val runtimeMap by webViewManager.states.collectAsState()
  val runtime = runtimeMap[workspace.id] ?: WorkspaceRuntimeState(currentUrl = workspace.currentUrl)

  var showMoreMenu by remember { mutableStateOf(false) }
  var showNotesDialog by remember { mutableStateOf(false) }
  var showSiteInfoDialog by remember { mutableStateOf(false) }
  var showSaveProjectDialog by remember { mutableStateOf(false) }
  var forceExternalMode by remember(workspace.id, preferExternalTab) {
    mutableStateOf(preferExternalTab)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Native AI UNIVERSE Workspace Toolbar: Back | AI Logo + Name | Copy | Refresh | More
    if (!isFullscreen) {
      Surface(
        color = neu.card,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = {
                if (!webViewManager.goBack(workspace.id)) {
                  onExitView()
                }
              },
              modifier = Modifier.testTag("ws_back_button"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = neu.primaryText)
            }

            Row(
              modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (platform != null) {
                AiBrandBadge(platform = platform, size = 32.dp)
              }
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = workspace.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = neu.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (workspace.isLocked) {
                    Icon(
                      Icons.Rounded.Lock,
                      contentDescription = "Locked",
                      tint = WarningAmber,
                      modifier = Modifier.size(13.dp),
                    )
                  }
                }
                Text(
                  text = runtime.currentUrl.ifBlank { workspace.officialUrl },
                  style = MaterialTheme.typography.labelSmall,
                  color = neu.secondaryText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            // Copy & Refresh MUST be adjacent
            IconButton(
              onClick = {
                val url = webViewManager.getCurrentUrl(workspace.id) ?: workspace.currentUrl
                onCopyUrl(workspace.aiName, url)
                Toast.makeText(context, "Copied URL", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.testTag("ws_copy_button"),
            ) {
              Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy URL", tint = neu.primaryText)
            }

            IconButton(
              onClick = { webViewManager.refreshOnly(workspace.id) },
              enabled = !runtime.isLoading,
              modifier = Modifier.testTag("ws_refresh_button"),
            ) {
              if (runtime.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Workspace", tint = neu.accent)
              }
            }

            Box {
              IconButton(
                onClick = { showMoreMenu = true },
                modifier = Modifier.testTag("ws_more_button"),
              ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = neu.primaryText)
              }
              DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                DropdownMenuItem(
                  text = { Text(strings.minimizeAndExit) },
                  leadingIcon = { Icon(Icons.Rounded.Minimize, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    onMinimize()
                  },
                )
                DropdownMenuItem(
                  text = { Text(strings.exitView) },
                  leadingIcon = { Icon(Icons.Rounded.ExitToApp, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    onExitView()
                  },
                )
                DropdownMenuItem(
                  text = { Text(strings.closeWorkspace, color = ErrorRed) },
                  leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, tint = ErrorRed) },
                  onClick = {
                    showMoreMenu = false
                    onClose()
                  },
                )
                HorizontalDivider()
                DropdownMenuItem(
                  text = { Text(if (isFullscreen) "Exit Fullscreen" else "Fullscreen") },
                  leadingIcon = { Icon(Icons.Rounded.Fullscreen, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    onToggleFullscreen()
                  },
                )
                DropdownMenuItem(
                  text = { Text("Share URL") },
                  leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    val url = webViewManager.getCurrentUrl(workspace.id) ?: workspace.currentUrl
                    val share =
                      Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, workspace.aiName)
                        putExtra(Intent.EXTRA_TEXT, url)
                      }
                    context.startActivity(Intent.createChooser(share, "Share ${workspace.aiName}"))
                  },
                )
                DropdownMenuItem(
                  text = { Text("Open in Chrome Custom Tab") },
                  leadingIcon = { Icon(Icons.Rounded.OpenInBrowser, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    val url = webViewManager.getCurrentUrl(workspace.id) ?: workspace.currentUrl
                    CustomTabHelper.openOfficialWebsite(context, url)
                  },
                )
                DropdownMenuItem(
                  text = { Text("Save as Project") },
                  leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    showSaveProjectDialog = true
                  },
                )
                DropdownMenuItem(
                  text = { Text("Workspace Notes") },
                  leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    showNotesDialog = true
                  },
                )
                DropdownMenuItem(
                  text = { Text("Files & Uploads") },
                  leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    onOpenFileManager()
                  },
                )
                DropdownMenuItem(
                  text = { Text("Website Information") },
                  leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    showSiteInfoDialog = true
                  },
                )
                DropdownMenuItem(
                  text = { Text(if (workspace.isLocked) "Unlock Workspace" else "Lock Workspace") },
                  leadingIcon = {
                    Icon(
                      if (workspace.isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                      contentDescription = null,
                    )
                  },
                  onClick = {
                    showMoreMenu = false
                    onToggleLock(workspace.id)
                  },
                )
                DropdownMenuItem(
                  text = { Text(strings.cookies.manageCookiesTitle) },
                  leadingIcon = { Icon(Icons.Rounded.Cookie, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    val url = webViewManager.getCurrentUrl(workspace.id) ?: workspace.officialUrl
                    onOpenCookieManager(url)
                  },
                )
                DropdownMenuItem(
                  text = { Text(strings.workspaceManager) },
                  leadingIcon = { Icon(Icons.Rounded.ViewCarousel, contentDescription = null) },
                  onClick = {
                    showMoreMenu = false
                    onOpenWorkspaceManager()
                  },
                )
                if (canSplitScreen) {
                  DropdownMenuItem(
                    text = { Text(if (isSplitActive) "Change Split Window" else "Split-Screen View") },
                    leadingIcon = { Icon(Icons.Rounded.VerticalSplit, contentDescription = null) },
                    onClick = {
                      showMoreMenu = false
                      onRequestSplit()
                    },
                  )
                  if (isSplitActive) {
                    DropdownMenuItem(
                      text = { Text("Swap Left / Right") },
                      leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                      onClick = {
                        showMoreMenu = false
                        onSwapSplit()
                      },
                    )
                  }
                }
              }
            }
          }

          if (runtime.isLoading) {
            LinearProgressIndicator(
              progress = { (runtime.progress / 100f).coerceIn(0.05f, 1f) },
              modifier = Modifier.fillMaxWidth().height(3.dp),
              color = neu.accent,
            )
          }
        }
      }
    }

    // Identity Warning Banner if navigated off approved origins
    if (runtime.originStatus == OriginCheckStatus.UNVERIFIED_EXTERNAL && runtime.blockedHostWarning != null) {
      Surface(color = WarningAmber.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
            Text(
              text = runtime.blockedHostWarning,
              style = MaterialTheme.typography.bodySmall,
              color = neu.primaryText,
            )
          }
          TextButton(onClick = { webViewManager.loadUrl(workspace.id, workspace.officialUrl) }) {
            Text("Reset Official URL")
          }
        }
      }
    }

    // Main WebView / External Tab / Error Container
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when {
        !isConnected && runtime.errorTitle != null -> {
          WorkspaceErrorCard(
            title = "Network Disconnected",
            detail = "No active internet connection. Reconnect and tap Retry.",
            officialUrl = workspace.officialUrl,
            onRetry = { webViewManager.refreshOnly(workspace.id) },
            onOpenExternal = { CustomTabHelper.openOfficialWebsite(context, workspace.officialUrl) },
            onBack = onExitView,
          )
        }
        runtime.errorTitle != null -> {
          WorkspaceErrorCard(
            title = runtime.errorTitle,
            detail = runtime.errorDetail ?: "Unable to load ${workspace.officialUrl}.",
            officialUrl = workspace.officialUrl,
            onRetry = { webViewManager.refreshOnly(workspace.id) },
            onOpenExternal = { CustomTabHelper.openOfficialWebsite(context, workspace.officialUrl) },
            onBack = onExitView,
          )
        }
        forceExternalMode -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            NeuCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, tint = neu.accent, modifier = Modifier.size(44.dp))
                Text(
                  text = "${workspace.aiName} • Chrome Custom Tab Mode",
                  style = MaterialTheme.typography.titleMedium,
                  color = neu.primaryText,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Official URL: ${workspace.officialUrl}\n\nExternal tab mode preserves workspace metadata inside AI UNIVERSE while opening the official site in Chrome Custom Tabs.",
                  style = MaterialTheme.typography.bodySmall,
                  color = neu.secondaryText,
                )
                Button(
                  onClick = { CustomTabHelper.openOfficialWebsite(context, workspace.currentUrl.ifBlank { workspace.officialUrl }) },
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Launch Official Website Tab")
                }
                OutlinedButton(
                  onClick = { forceExternalMode = false },
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Switch to In-App WebView")
                }
              }
            }
          }
        }
        else -> {
          val approvedOrigins = platform?.approvedOrigins ?: listOf()
          key(workspace.id) {
            AndroidView(
              modifier = Modifier.fillMaxSize(),
              factory = { ctx ->
                val container =
                  FrameLayout(ctx).apply {
                    layoutParams =
                      ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                      )
                  }
                val wv =
                  webViewManager.getOrCreateWebView(
                    workspaceId = workspace.id,
                    initialUrl = workspace.currentUrl.ifBlank { workspace.officialUrl },
                    approvedOrigins = approvedOrigins,
                    dataSaver = dataSaver,
                    hostContext = ctx,
                  )
                if (wv != null) {
                  (wv.parent as? ViewGroup)?.removeView(wv)
                  container.addView(
                    wv,
                    FrameLayout.LayoutParams(
                      FrameLayout.LayoutParams.MATCH_PARENT,
                      FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                  )
                }
                container
              },
              update = { container ->
                val wv =
                  webViewManager.getOrCreateWebView(
                    workspaceId = workspace.id,
                    initialUrl = workspace.currentUrl.ifBlank { workspace.officialUrl },
                    approvedOrigins = approvedOrigins,
                    dataSaver = dataSaver,
                    hostContext = container.context,
                  )
                if (wv != null && (container.childCount == 0 || container.getChildAt(0) !== wv)) {
                  (wv.parent as? ViewGroup)?.removeView(wv)
                  container.removeAllViews()
                  container.addView(
                    wv,
                    FrameLayout.LayoutParams(
                      FrameLayout.LayoutParams.MATCH_PARENT,
                      FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                  )
                }
              },
              onRelease = { container ->
                container.removeAllViews()
                webViewManager.detachHostContext(workspace.id)
              },
            )
          }
        }
      }

      if (isFullscreen) {
        FloatingActionButton(
          onClick = onToggleFullscreen,
          modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
          containerColor = neu.card,
        ) {
          Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit Fullscreen", tint = neu.primaryText)
        }
      }
    }
  }

  // Notes Dialog
  if (showNotesDialog) {
    var noteDraft by remember(workspace.id) { mutableStateOf(workspace.notes) }
    AlertDialog(
      onDismissRequest = { showNotesDialog = false },
      title = { Text("Workspace Notes • ${workspace.aiName}") },
      text = {
        OutlinedTextField(
          value = noteDraft,
          onValueChange = { noteDraft = it },
          label = { Text("Prompts, links, or session notes") },
          minLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateNotes(workspace.id, noteDraft)
            showNotesDialog = false
          }
        ) {
          Text(strings.save)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNotesDialog = false }) { Text(strings.cancel) }
      },
    )
  }

  // Website Info Dialog
  if (showSiteInfoDialog) {
    AlertDialog(
      onDismissRequest = { showSiteInfoDialog = false },
      title = { Text("Website Verification") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("AI Provider: ${workspace.aiName}", fontWeight = FontWeight.Bold)
          Text("Official URL: ${workspace.officialUrl}", style = MaterialTheme.typography.bodySmall)
          Text("Current URL: ${runtime.currentUrl}", style = MaterialTheme.typography.bodySmall)
          Text("Origin Status: ${runtime.originStatus.name}", style = MaterialTheme.typography.bodySmall)
          Text(
            "Cookies and authentication sessions inside WebView are managed natively by Android Chromium CookieManager.",
            style = MaterialTheme.typography.bodySmall,
            color = neu.secondaryText,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showSiteInfoDialog = false }) { Text("Close") }
      },
    )
  }

  // Save Project Dialog
  if (showSaveProjectDialog) {
    val currentUrl = webViewManager.getCurrentUrl(workspace.id) ?: workspace.currentUrl
    var projName by remember { mutableStateOf("${workspace.aiName} Project") }
    AlertDialog(
      onDismissRequest = { showSaveProjectDialog = false },
      title = { Text("Save Current URL to Projects") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = projName,
            onValueChange = { projName = it },
            label = { Text("Project Name") },
            modifier = Modifier.fillMaxWidth(),
          )
          Text(currentUrl, style = MaterialTheme.typography.labelSmall, color = neu.secondaryText)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveProject(
              projName.ifBlank { workspace.aiName },
              currentUrl,
              workspace.aiId,
              workspace.aiName,
              platform?.category?.label ?: "General",
            )
            showSaveProjectDialog = false
            Toast.makeText(context, "Project saved", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Save Project")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveProjectDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun WorkspaceErrorCard(
  title: String,
  detail: String,
  officialUrl: String,
  onRetry: () -> Unit,
  onOpenExternal: () -> Unit,
  onBack: () -> Unit,
) {
  val neu = LocalNeuColors.current
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    NeuCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = neu.primaryText, fontWeight = FontWeight.Bold)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = neu.secondaryText)
        Text(officialUrl, style = MaterialTheme.typography.labelSmall, color = neu.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Retry") }
          OutlinedButton(onClick = onOpenExternal, modifier = Modifier.weight(1f)) {
            Text("Custom Tab")
          }
        }
        TextButton(onClick = onBack) { Text("Back") }
      }
    }
  }
}
