package com.example.ui.projects

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectEntity
import com.example.data.model.AiPlatform
import com.example.data.network.UrlValidator
import com.example.ui.components.formatTimestamp
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.*
import com.example.ui.workspace.CustomTabHelper

private enum class ProjectSection(val label: String) {
  ALL("All Projects"),
  RECENT("Recent"),
  FAVORITES("Favorites"),
  SAVED("Saved"),
}

@Composable
fun ProjectManagerScreen(
  projects: List<ProjectEntity>,
  platforms: List<AiPlatform>,
  onSaveProject: (String?, String, String, String, String, String, String, Boolean) -> Unit,
  onToggleFavorite: (ProjectEntity) -> Unit,
  onDeleteProject: (String) -> Unit,
  onCopyUrl: (String, String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  var section by remember { mutableStateOf(ProjectSection.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }

  val filtered =
    remember(projects, section, searchQuery) {
      val bySec =
        when (section) {
          ProjectSection.ALL,
          ProjectSection.SAVED -> projects
          ProjectSection.RECENT -> projects.sortedByDescending { it.createdAt }.take(15)
          ProjectSection.FAVORITES -> projects.filter { it.isFavorite }
        }
      if (searchQuery.isBlank()) bySec
      else
        bySec.filter {
          it.name.contains(searchQuery, ignoreCase = true) ||
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
          text = strings.universalProjectManager,
          style = MaterialTheme.typography.titleMedium,
          color = neu.primaryText,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        SpringIconButton(
          onClick = {
            val exportText =
              projects.joinToString("\n") { "${it.name} | ${it.aiName} | ${it.url}" }
            val share =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "AI UNIVERSE Projects")
                putExtra(Intent.EXTRA_TEXT, exportText.ifBlank { strings.noProjectsFound })
              }
            context.startActivity(Intent.createChooser(share, strings.shareLabel))
          }
        ) {
          Icon(Icons.Rounded.IosShare, contentDescription = strings.shareLabel, tint = neu.primaryText)
        }
        SpringIconButton(onClick = { showAddDialog = true }) {
          Icon(Icons.Rounded.Add, contentDescription = strings.addProject, tint = neu.accent)
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ProjectSection.entries.forEach { s ->
          val label =
            when (s) {
              ProjectSection.ALL -> strings.allProjectsTab
              ProjectSection.RECENT -> strings.recentTab
              ProjectSection.FAVORITES -> strings.favoritesTab
              ProjectSection.SAVED -> strings.savedTab
            }
          SpringFilterChip(selected = section == s, onClick = { section = s }, label = { Text(label) })
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text(strings.searchProjectsHint) },
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
            Icon(Icons.Rounded.Workspaces, contentDescription = null, tint = neu.secondaryText, modifier = Modifier.size(44.dp))
            Text(strings.noProjectsFound, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
            SpringButton(onClick = { showAddDialog = true }) { Text(strings.addProject) }
          }
        }
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
          items(filtered, key = { it.id }) { proj ->
            NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(proj.name, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
                    Text(
                      "${proj.aiName} • ${proj.category} • ${formatTimestamp(proj.createdAt)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = neu.secondaryText,
                    )
                  }
                  IconButton(onClick = { onToggleFavorite(proj) }) {
                    Icon(
                      if (proj.isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                      contentDescription = "Favorite project",
                      tint = if (proj.isFavorite) WarningAmber else neu.secondaryText,
                    )
                  }
                }
                Text(
                  proj.url,
                  style = MaterialTheme.typography.labelSmall,
                  color = neu.accent,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (proj.notes.isNotBlank()) {
                  Text(proj.notes, style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                ) {
                  TextButton(onClick = { editingProject = proj }) { Text(strings.edit) }
                  TextButton(
                    onClick = {
                      onCopyUrl(proj.name, proj.url)
                      Toast.makeText(context, "${strings.copyLink} ✓", Toast.LENGTH_SHORT).show()
                    }
                  ) {
                    Text(strings.copyLink)
                  }
                  TextButton(onClick = { CustomTabHelper.openOfficialWebsite(context, proj.url) }) {
                    Text(strings.openLabel)
                  }
                  IconButton(onClick = { onDeleteProject(proj.id) }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = ErrorRed)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog || editingProject != null) {
    val target = editingProject
    var name by remember { mutableStateOf(target?.name ?: "") }
    var url by remember { mutableStateOf(target?.url ?: "") }
    var aiName by remember { mutableStateOf(target?.aiName ?: (platforms.firstOrNull()?.name ?: "v0")) }
    var notes by remember { mutableStateOf(target?.notes ?: "") }

    AlertDialog(
      onDismissRequest = {
        showAddDialog = false
        editingProject = null
      },
      title = { Text(if (target == null) "Add Project Record" else "Edit Project") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Name") }, singleLine = true)
          OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Project URL (https://...)") }, singleLine = true)
          OutlinedTextField(value = aiName, onValueChange = { aiName = it }, label = { Text("AI Provider") }, singleLine = true)
          OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val normalized = UrlValidator.normalizeUrl(url)
            if (name.isNotBlank() && UrlValidator.isValidHttpsUrl(normalized)) {
              onSaveProject(
                target?.id,
                name,
                normalized,
                target?.aiId ?: "",
                aiName,
                target?.category ?: "General",
                notes,
                target?.isFavorite ?: false,
              )
              showAddDialog = false
              editingProject = null
            } else {
              Toast.makeText(context, "Enter a valid project name and URL", Toast.LENGTH_SHORT).show()
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showAddDialog = false
            editingProject = null
          }
        ) {
          Text("Cancel")
        }
      },
    )
  }
}
