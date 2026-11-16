package com.example.ui.files

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.VaultFileEntity
import com.example.data.repository.StorageStats
import com.example.ui.components.formatBytes
import com.example.ui.components.formatTimestamp
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.*
import java.io.File

@Composable
fun FileManagerScreen(
  files: List<VaultFileEntity>,
  storageStats: StorageStats,
  onImportUri: (android.net.Uri, String, (String) -> Unit) -> Unit,
  onCreateFolder: (String, String) -> Unit,
  onRenameFile: (VaultFileEntity, String) -> Unit,
  onMoveFile: (VaultFileEntity, String) -> Unit,
  onDeleteFile: (VaultFileEntity) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  var currentFolder by remember { mutableStateOf("Root") }
  BackHandler {
    if (currentFolder != "Root") currentFolder = "Root" else onBack()
  }
  var searchQuery by remember { mutableStateOf("") }
  var sortByName by remember { mutableStateOf(false) }
  var showNewFolderDialog by remember { mutableStateOf(false) }
  var previewFile by remember { mutableStateOf<VaultFileEntity?>(null) }
  var renamingFile by remember { mutableStateOf<VaultFileEntity?>(null) }
  var movingFile by remember { mutableStateOf<VaultFileEntity?>(null) }
  var exportSourceFile by remember { mutableStateOf<VaultFileEntity?>(null) }

  val docPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      uris.forEach { uri ->
        onImportUri(uri, currentFolder) { msg ->
          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
      }
    }

  val mediaPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
      uris.forEach { uri ->
        onImportUri(uri, currentFolder) { msg ->
          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
      }
    }

  val exportSaver =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { destUri ->
      val src = exportSourceFile
      if (destUri != null && src != null) {
        runCatching {
          File(src.localPath).inputStream().use { input ->
            context.contentResolver.openOutputStream(destUri)?.use { output ->
              input.copyTo(output)
            }
          }
          Toast.makeText(context, "Saved ${src.name}", Toast.LENGTH_SHORT).show()
        }.onFailure {
          Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
      }
      exportSourceFile = null
    }

  val allFolders = remember(files) { listOf("Root") + files.filter { it.isFolder }.map { it.name }.distinct() }
  val visibleFiles =
    remember(files, currentFolder, searchQuery, sortByName) {
      val list =
        files.filter { f ->
          val inFolder = if (searchQuery.isBlank()) f.folder == currentFolder else true
          val matchQuery =
            searchQuery.isBlank() || f.name.contains(searchQuery, ignoreCase = true)
          inFolder && matchQuery
        }
      if (sortByName) list.sortedBy { it.name.lowercase() } else list
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // Toolbar
    Surface(color = neu.card, tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SpringIconButton(
          onClick = {
            if (currentFolder != "Root") currentFolder = "Root" else onBack()
          }
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = neu.primaryText)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = strings.universalFileManager,
            style = MaterialTheme.typography.titleMedium,
            color = neu.primaryText,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "$currentFolder • ${storageStats.fileCount} (${formatBytes(storageStats.vaultBytes)})",
            style = MaterialTheme.typography.labelSmall,
            color = neu.secondaryText,
          )
        }
        SpringIconButton(onClick = { sortByName = !sortByName }) {
          Icon(Icons.Rounded.SortByAlpha, contentDescription = strings.sort, tint = if (sortByName) neu.accent else neu.primaryText)
        }
        SpringIconButton(onClick = { showNewFolderDialog = true }) {
          Icon(Icons.Rounded.CreateNewFolder, contentDescription = strings.createFolder, tint = neu.accent)
        }
      }
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .animateContentSize(animationSpec = tween(200, easing = FastOutSlowInEasing)),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      val reducedMotion = LocalReducedMotion.current
      // Folder Breadcrumbs with smooth open/close reveal
      AnimatedVisibility(
        visible = allFolders.size > 1 || currentFolder != "Root",
        enter =
          if (reducedMotion) fadeIn(tween(0))
          else expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
        exit =
          if (reducedMotion) fadeOut(tween(0))
          else shrinkVertically(tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(140)),
      ) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          allFolders.forEach { folderName ->
            SpringFilterChip(
              selected = currentFolder == folderName,
              onClick = { currentFolder = folderName },
              label = { Text(folderName) },
              leadingIcon = {
                Icon(
                  if (currentFolder == folderName) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
            )
          }
        }
      }

      // Upload Action Bar
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SpringButton(
          onClick = {
            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
          },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.mediaPicker)
        }
        SpringOutlinedButton(
          onClick = { docPicker.launch(arrayOf("*/*")) },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.importDocs)
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text(strings.searchFilesHint) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      )

      AnimatedContent(
        targetState = currentFolder,
        contentKey = { it },
        modifier = Modifier.weight(1f).fillMaxWidth(),
        transitionSpec = {
          if (reducedMotion) {
            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
          } else {
            val openingFolder = targetState != "Root"
            if (openingFolder) {
              (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 4 } +
                fadeIn(tween(180, easing = FastOutSlowInEasing))) togetherWith
                (slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 5 } +
                  fadeOut(tween(140)))
            } else {
              (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 4 } +
                fadeIn(tween(180, easing = FastOutSlowInEasing))) togetherWith
                (slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 5 } +
                  fadeOut(tween(140)))
            }
          }
        },
        label = "folder_open_close_transition",
      ) { activeFolder ->
        val folderItems =
          remember(visibleFiles, activeFolder, searchQuery) {
            visibleFiles.filter { f ->
              if (searchQuery.isBlank()) f.folder == activeFolder else true
            }
          }
        if (folderItems.isEmpty()) {
            NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = neu.secondaryText, modifier = Modifier.size(44.dp))
              Text(String.format(strings.noFilesInFolder, activeFolder), style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
              Text(
                "JPG, PNG, WEBP, MP4, MOV, MP3, WAV, PDF, DOCX, XLSX, PPTX, ZIP, TXT, HTML, CSS, JS, PY, KT",
                style = MaterialTheme.typography.bodySmall,
                color = neu.secondaryText,
              )
              if (activeFolder != "Root") {
                SpringOutlinedButton(onClick = { currentFolder = "Root" }) {
                  Text(strings.closeFolder)
                }
              }
            }
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(folderItems, key = { it.id }, contentType = { if (it.isFolder) "folder" else "file" }) { file ->
              var showMenu by remember { mutableStateOf(false) }
              Box(modifier = Modifier.animateItem()) {
                NeuCard(
                  modifier = Modifier.fillMaxWidth(),
                  elevated = false,
                  onClick = {
                    if (file.isFolder) {
                      currentFolder = file.name
                    } else {
                      previewFile = file
                    }
                  },
                ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Icon(
                    imageVector =
                      when {
                        file.isFolder -> Icons.Rounded.Folder
                        file.mimeType.startsWith("image/") -> Icons.Rounded.Image
                        file.mimeType.startsWith("video/") -> Icons.Rounded.Movie
                        file.mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile
                        else -> Icons.Rounded.InsertDriveFile
                      },
                    contentDescription = null,
                    tint = if (file.isFolder) WarningAmber else neu.accent,
                    modifier = Modifier.size(34.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      file.name,
                      style = MaterialTheme.typography.titleSmall,
                      color = neu.primaryText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      if (file.isFolder) "Folder • Tap to open"
                      else "${formatBytes(file.sizeBytes)} • ${file.mimeType} • ${formatTimestamp(file.createdAt)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = neu.secondaryText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Box {
                    SpringIconButton(onClick = { showMenu = true }) {
                      Icon(Icons.Rounded.MoreVert, contentDescription = "File options", tint = neu.primaryText)
                    }
                  DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!file.isFolder) {
                      DropdownMenuItem(
                        text = { Text("Preview") },
                        onClick = {
                          showMenu = false
                          previewFile = file
                        },
                      )
                      DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                          showMenu = false
                          runCatching {
                            val uri =
                              FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                File(file.localPath),
                              )
                            val share =
                              Intent(Intent.ACTION_SEND).apply {
                                type = file.mimeType
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                              }
                            context.startActivity(Intent.createChooser(share, "Share ${file.name}"))
                          }
                        },
                      )
                      DropdownMenuItem(
                        text = { Text("Save to Device Downloads") },
                        onClick = {
                          showMenu = false
                          exportSourceFile = file
                          exportSaver.launch(file.name)
                        },
                      )
                      DropdownMenuItem(
                        text = { Text("Move to Folder") },
                        onClick = {
                          showMenu = false
                          movingFile = file
                        },
                      )
                    }
                    DropdownMenuItem(
                      text = { Text("Rename") },
                      onClick = {
                        showMenu = false
                        renamingFile = file
                      },
                    )
                    DropdownMenuItem(
                      text = { Text("Delete", color = ErrorRed) },
                      onClick = {
                        showMenu = false
                        onDeleteFile(file)
                      },
                    )
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
  }

  if (showNewFolderDialog) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showNewFolderDialog = false },
      title = { Text(strings.createFolder) },
      text = {
        OutlinedTextField(
          value = folderName,
          onValueChange = { folderName = it },
          label = { Text(strings.folderNameLabel) },
          singleLine = true,
        )
      },
      confirmButton = {
        SpringButton(
          onClick = {
            if (folderName.isNotBlank()) {
              onCreateFolder(folderName, currentFolder)
              showNewFolderDialog = false
            }
          }
        ) {
          Text(strings.createLabel)
        }
      },
      dismissButton = {
        SpringTextButton(onClick = { showNewFolderDialog = false }) { Text(strings.cancel) }
      },
    )
  }

  renamingFile?.let { target ->
    var newName by remember(target.id) { mutableStateOf(target.name) }
    AlertDialog(
      onDismissRequest = { renamingFile = null },
      title = { Text("Rename") },
      text = {
        OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
      },
      confirmButton = {
        SpringButton(
          onClick = {
            if (newName.isNotBlank()) onRenameFile(target, newName)
            renamingFile = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        SpringTextButton(onClick = { renamingFile = null }) { Text("Cancel") }
      },
    )
  }

  movingFile?.let { target ->
    AlertDialog(
      onDismissRequest = { movingFile = null },
      title = { Text("Move ${target.name}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          allFolders.forEach { folder ->
            SpringOutlinedButton(
              onClick = {
                onMoveFile(target, folder)
                movingFile = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(folder)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = { SpringTextButton(onClick = { movingFile = null }) { Text("Cancel") } },
    )
  }

  previewFile?.let { file ->
    val diskFile = File(file.localPath)
    val textSnippet =
      remember(file.id) {
        if (
          file.mimeType.startsWith("text/") ||
            file.name.endsWith(".json") ||
            file.name.endsWith(".py") ||
            file.name.endsWith(".js") ||
            file.name.endsWith(".kt") ||
            file.name.endsWith(".html") ||
            file.name.endsWith(".css")
        ) {
          runCatching { diskFile.readText().take(4000) }.getOrNull()
        } else {
          null
        }
      }

    AlertDialog(
      onDismissRequest = { previewFile = null },
      title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (file.mimeType.startsWith("image/") && diskFile.exists()) {
            AsyncImage(
              model = diskFile,
              contentDescription = file.name,
              modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Fit,
            )
          } else if (textSnippet != null) {
            Text(
              text = textSnippet,
              fontFamily = JetBrainsMonoFontFamily,
              style = MaterialTheme.typography.bodySmall,
            )
          } else {
            Text("Type: ${file.mimeType}\nSize: ${formatBytes(file.sizeBytes)}\nFolder: ${file.folder}")
          }
        }
      },
      confirmButton = {
        SpringTextButton(onClick = { previewFile = null }) { Text("Close") }
      },
    )
  }
}
