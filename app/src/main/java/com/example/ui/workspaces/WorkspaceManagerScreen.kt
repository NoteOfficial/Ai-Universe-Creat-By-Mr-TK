package com.example.ui.workspaces

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WorkspaceEntity
import com.example.data.model.AiPlatform
import com.example.ui.components.AiBrandBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.*

@Composable
fun WorkspaceManagerScreen(
  workspaces: List<WorkspaceEntity>,
  platforms: List<AiPlatform>,
  onOpenOrRestore: (String) -> Unit,
  onMinimize: (String) -> Unit,
  onRename: (String, String) -> Unit,
  onUpdateNotes: (String, String) -> Unit,
  onToggleLock: (String) -> Unit,
  onClose: (String) -> Unit,
  onClearAllMetadata: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current
  var searchQuery by remember { mutableStateOf("") }
  var filterTab by remember { mutableIntStateOf(0) } // 0=All, 1=Active, 2=Minimized
  var renamingWs by remember { mutableStateOf<WorkspaceEntity?>(null) }
  var notesWs by remember { mutableStateOf<WorkspaceEntity?>(null) }

  val filtered =
    remember(workspaces, searchQuery, filterTab) {
      val byTab =
        when (filterTab) {
          1 -> workspaces.filter { !it.isMinimized }
          2 -> workspaces.filter { it.isMinimized }
          else -> workspaces
        }
      if (searchQuery.isBlank()) byTab
      else
        byTab.filter {
          it.title.contains(searchQuery, ignoreCase = true) ||
            it.aiName.contains(searchQuery, ignoreCase = true) ||
            it.notes.contains(searchQuery, ignoreCase = true)
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
        SpringIconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = neu.primaryText)
        }
        Text(
          text = "${strings.workspaceManagerTitle} (${workspaces.size})",
          style = MaterialTheme.typography.titleMedium,
          color = neu.primaryText,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        if (workspaces.isNotEmpty()) {
          SpringTextButton(onClick = onClearAllMetadata) {
            Text(strings.clearAll, color = ErrorRed)
          }
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpringFilterChip(selected = filterTab == 0, onClick = { filterTab = 0 }, label = { Text("${strings.allTab} (${workspaces.size})") })
        SpringFilterChip(
          selected = filterTab == 1,
          onClick = { filterTab = 1 },
          label = { Text("${strings.activeLabel} (${workspaces.count { !it.isMinimized }})") },
        )
        SpringFilterChip(
          selected = filterTab == 2,
          onClick = { filterTab = 2 },
          label = { Text("${strings.minimizedLabel} (${workspaces.count { it.isMinimized }})") },
        )
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text(strings.searchWorkspacesHint) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      )

      if (filtered.isEmpty()) {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.ViewCarousel, contentDescription = null, tint = neu.secondaryText, modifier = Modifier.size(44.dp))
            Text(strings.noWorkspacesHere, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
          }
        }
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
          items(filtered, key = { it.id }) { ws ->
            val ai = platforms.find { it.id == ws.aiId }
            Box(modifier = Modifier.animateItem()) {
              NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false, onClick = { onOpenOrRestore(ws.id) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    if (ai != null) AiBrandBadge(platform = ai, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ws.title, style = MaterialTheme.typography.titleSmall, color = neu.primaryText, fontWeight = FontWeight.Bold)
                        if (ws.isLocked) {
                          Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = WarningAmber, modifier = Modifier.size(14.dp))
                        }
                      }
                      Text(
                        "${if (ws.isMinimized) "Minimized" else "Active"} • ${formatTimestamp(ws.lastActiveAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ws.isMinimized) WarningAmber else SuccessGreen,
                      )
                    }
                    IconButton(onClick = { onToggleLock(ws.id) }) {
                      Icon(
                        if (ws.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = "Lock toggle",
                        tint = neu.secondaryText,
                      )
                    }
                  }
                  Text(
                    ws.currentUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (ws.notes.isNotBlank()) {
                    Text(ws.notes, style = MaterialTheme.typography.bodySmall, color = neu.primaryText)
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { notesWs = ws }) { Text(strings.notesLabel) }
                    TextButton(onClick = { renamingWs = ws }) { Text(strings.renameLabel) }
                    if (!ws.isMinimized) {
                      TextButton(onClick = { onMinimize(ws.id) }) { Text(strings.minimizeLabel) }
                    }
                    TextButton(onClick = { onOpenOrRestore(ws.id) }) {
                      Text(if (ws.isMinimized) strings.restoreLabel else strings.switchLabel)
                    }
                    if (!ws.isLocked) {
                      TextButton(onClick = { onClose(ws.id) }) { Text(strings.closeLabel, color = ErrorRed) }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  renamingWs?.let { target ->
    var title by remember(target.id) { mutableStateOf(target.title) }
    AlertDialog(
      onDismissRequest = { renamingWs = null },
      title = { Text("Rename Workspace") },
      text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) },
      confirmButton = {
        Button(
          onClick = {
            onRename(target.id, title)
            renamingWs = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = { TextButton(onClick = { renamingWs = null }) { Text("Cancel") } },
    )
  }

  notesWs?.let { target ->
    var notes by remember(target.id) { mutableStateOf(target.notes) }
    AlertDialog(
      onDismissRequest = { notesWs = null },
      title = { Text("Workspace Notes") },
      text = { OutlinedTextField(value = notes, onValueChange = { notes = it }, minLines = 3) },
      confirmButton = {
        Button(
          onClick = {
            onUpdateNotes(target.id, notes)
            notesWs = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = { TextButton(onClick = { notesWs = null }) { Text("Cancel") } },
    )
  }
}
