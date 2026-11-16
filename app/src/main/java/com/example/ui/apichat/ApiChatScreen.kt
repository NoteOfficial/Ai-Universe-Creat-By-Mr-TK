package com.example.ui.apichat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ApiChatMessageEntity
import com.example.data.local.ApiChatSessionEntity
import com.example.data.local.ApiProviderEntity
import com.example.data.model.ApiProviderType
import com.example.data.network.ApiAttachmentPayload
import com.example.data.network.ApiResponseResult
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiChatScreen(
  providers: List<ApiProviderEntity>,
  sessions: List<ApiChatSessionEntity>,
  isGenerating: Boolean,
  messagesProvider: (String) -> Flow<List<ApiChatMessageEntity>>,
  getApiKey: (String) -> String,
  onTestProvider: suspend (ApiProviderType, String, String, String) -> ApiResponseResult,
  onSaveProvider: (String?, String, ApiProviderType, String, String, String, String) -> Unit,
  onDeleteProvider: (String) -> Unit,
  onCreateSession: (ApiProviderEntity) -> String,
  onUpdateSession: (ApiChatSessionEntity) -> Unit,
  onDeleteSession: (String) -> Unit,
  onSendMessage: (ApiChatSessionEntity, ApiProviderEntity, String, ApiAttachmentPayload?) -> Unit,
  onStopGeneration: () -> Unit,
  onRegenerate: (ApiChatSessionEntity, ApiProviderEntity, List<ApiChatMessageEntity>) -> Unit,
  onEditMessage: (ApiChatSessionEntity, ApiProviderEntity, ApiChatMessageEntity, String, List<ApiChatMessageEntity>) -> Unit,
  onDeleteMessage: (String) -> Unit,
  onCopyText: (String, String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  var activeSessionId by remember { mutableStateOf(sessions.firstOrNull()?.id) }
  var showProviderDialog by remember { mutableStateOf<ApiProviderEntity?>(null) }
  var isAddingProvider by remember { mutableStateOf(false) }
  var showHistorySheet by remember { mutableStateOf(false) }
  var showSessionSettings by remember { mutableStateOf(false) }
  var editingMessage by remember { mutableStateOf<ApiChatMessageEntity?>(null) }

  var inputText by remember { mutableStateOf("") }
  var pendingAttachment by remember { mutableStateOf<ApiAttachmentPayload?>(null) }

  val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()
  val activeProvider =
    providers.find { it.id == activeSession?.providerId } ?: providers.firstOrNull()

  val messages by
    remember(activeSession?.id) {
      activeSession?.id?.let { messagesProvider(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  // Image Picker (Photo Picker - zero permission)
  val imagePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        runCatching {
          val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
          val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          if (bytes != null && bytes.size <= 4 * 1024 * 1024) {
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            pendingAttachment = ApiAttachmentPayload("image_attachment", mime, base64Data = b64)
          } else {
            Toast.makeText(context, "Image too large (max 4 MB)", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }

  // Document/Code File Picker
  val filePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        runCatching {
          val mime = context.contentResolver.getType(uri) ?: "text/plain"
          val text =
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
          if (text != null) {
            pendingAttachment =
              ApiAttachmentPayload(
                name = uri.lastPathSegment ?: "document.txt",
                mimeType = mime,
                textContent = text,
              )
          }
        }
      }
    }

  // Zero-permission System Speech Recognizer
  val voiceLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      if (res.resultCode == Activity.RESULT_OK) {
        val spoken =
          res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
          inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
        }
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // Top Toolbar
    Surface(color = neu.card, tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = neu.primaryText)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = activeSession?.title ?: strings.apiChatTitle,
            style = MaterialTheme.typography.titleMedium,
            color = neu.primaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              if (activeProvider != null) "${activeProvider.displayName} • ${activeSession?.model ?: activeProvider.model}"
              else strings.configureProviderHint,
            style = MaterialTheme.typography.labelSmall,
            color = neu.secondaryText,
          )
        }
        IconButton(onClick = { showHistorySheet = true }) {
          Icon(Icons.Rounded.History, contentDescription = strings.chatHistoryTitle, tint = neu.primaryText)
        }
        IconButton(onClick = { showSessionSettings = true }, enabled = activeSession != null) {
          Icon(Icons.Rounded.Tune, contentDescription = strings.settings, tint = neu.primaryText)
        }
        IconButton(onClick = { isAddingProvider = true }) {
          Icon(Icons.Rounded.Key, contentDescription = strings.addApiProvider, tint = neu.accent)
        }
      }
    }

    // Provider Quick Chips
    if (providers.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        providers.take(4).forEach { prov ->
          val selected = prov.id == activeProvider?.id
          FilterChip(
            selected = selected,
            onClick = {
              val matchSession = sessions.firstOrNull { it.providerId == prov.id }
              activeSessionId = matchSession?.id ?: onCreateSession(prov)
            },
            label = { Text(prov.displayName, maxLines = 1) },
            trailingIcon = {
              IconButton(
                onClick = { showProviderDialog = prov },
                modifier = Modifier.size(18.dp),
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${prov.displayName}", modifier = Modifier.size(14.dp))
              }
            },
          )
        }
        if (activeProvider != null) {
          AssistChip(
            onClick = { activeSessionId = onCreateSession(activeProvider) },
            label = { Text(strings.newChat) },
            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
          )
        }
      }
    }

    // Main Chat Body
    if (providers.isEmpty()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        NeuCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = neu.accent, modifier = Modifier.size(48.dp))
            Text(
              strings.byokTitle,
              style = MaterialTheme.typography.titleLarge,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            Text(
              strings.byokDescription,
              style = MaterialTheme.typography.bodyMedium,
              color = neu.secondaryText,
            )
            SpringButton(
              onClick = { isAddingProvider = true },
              modifier = Modifier.fillMaxWidth().testTag("add_api_provider_button"),
            ) {
              Text(strings.addApiProvider)
            }
          }
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (messages.isEmpty()) {
          item {
            NeuCard(modifier = Modifier.fillMaxWidth()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  "Ready with ${activeProvider?.displayName}",
                  style = MaterialTheme.typography.titleMedium,
                  color = neu.primaryText,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  "Model: ${activeSession?.model ?: activeProvider?.model} • Temp: ${activeSession?.temperature ?: 0.7f}",
                  style = MaterialTheme.typography.labelSmall,
                  color = neu.secondaryText,
                )
                Text(
                  "Send a message, attach code/images, or tap the mic for voice input. Real API responses and real errors will appear here.",
                  style = MaterialTheme.typography.bodySmall,
                  color = neu.secondaryText,
                )
              }
            }
          }
        }

        items(messages, key = { it.id }) { msg ->
          val isUser = msg.role == "user"
          val isError = msg.role == "error"
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
          ) {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color =
                when {
                  isUser -> neu.accent
                  isError -> ErrorRed.copy(alpha = 0.16f)
                  else -> neu.card
                },
              tonalElevation = 2.dp,
              modifier = Modifier.widthIn(max = 340.dp),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!msg.attachmentName.isNullOrBlank()) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.14f),
                  ) {
                    Text(
                      text = "Attached: ${msg.attachmentName}",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isUser) Color.White else neu.secondaryText,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
                FormattedMessageContent(
                  content = msg.content,
                  textColor =
                    when {
                      isUser -> Color.White
                      isError -> ErrorRed
                      else -> neu.primaryText
                    },
                  onCopyCode = { code ->
                    onCopyText("Code Block", code)
                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                  },
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              IconButton(
                onClick = {
                  onCopyText("API Chat", msg.content)
                  Toast.makeText(context, "Copied message", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(30.dp),
              ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = neu.secondaryText)
              }
              if (isUser) {
                IconButton(
                  onClick = { editingMessage = msg },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = neu.secondaryText)
                }
              } else if (msg == messages.lastOrNull() && activeSession != null && activeProvider != null) {
                IconButton(
                  onClick = { onRegenerate(activeSession, activeProvider, messages) },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.Rounded.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(14.dp), tint = neu.secondaryText)
                }
              }
              IconButton(
                onClick = { onDeleteMessage(msg.id) },
                modifier = Modifier.size(30.dp),
              ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = neu.secondaryText)
              }
            }
          }
        }

        if (isGenerating) {
          item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Text(strings.waitingApiResponse, style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
              TextButton(onClick = onStopGeneration) {
                Text(strings.stopLabel, color = ErrorRed)
              }
            }
          }
        }
      }

      // Attachment Preview Bar
      pendingAttachment?.let { att ->
        Surface(color = neu.surfaceInset, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(att.name, style = MaterialTheme.typography.labelMedium, color = neu.primaryText)
            IconButton(onClick = { pendingAttachment = null }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Rounded.Close, contentDescription = strings.removeLabel, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Input Bar
      Surface(color = neu.card, tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          IconButton(
            onClick = {
              imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
          ) {
            Icon(Icons.Rounded.Image, contentDescription = strings.attachBtn, tint = neu.secondaryText)
          }
          IconButton(
            onClick = {
              filePicker.launch(arrayOf("text/*", "application/json", "*/*"))
            }
          ) {
            Icon(Icons.Rounded.AttachFile, contentDescription = strings.attachBtn, tint = neu.secondaryText)
          }
          IconButton(
            onClick = {
              val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                  putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                  )
                }
              runCatching { voiceLauncher.launch(intent) }.onFailure {
                Toast.makeText(context, "Speech recognition unavailable on this device", Toast.LENGTH_SHORT).show()
              }
            }
          ) {
            Icon(Icons.Rounded.Mic, contentDescription = "Voice Input", tint = neu.secondaryText)
          }
          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text(String.format(strings.askPlaceholderFmt, activeProvider?.displayName ?: "AI")) },
            maxLines = 4,
            modifier = Modifier.weight(1f).testTag("api_chat_input"),
            shape = RoundedCornerShape(16.dp),
          )
          IconButton(
            onClick = {
              if (isGenerating) {
                onStopGeneration()
              } else if (activeProvider != null && (inputText.isNotBlank() || pendingAttachment != null)) {
                val sess =
                  activeSession
                    ?: run {
                      val newId = onCreateSession(activeProvider)
                      activeSessionId = newId
                      ApiChatSessionEntity(
                        id = newId,
                        title = inputText.trim().take(36).ifBlank { "New ${activeProvider.displayName} Chat" },
                        providerId = activeProvider.id,
                        model = activeProvider.model,
                        systemPrompt = activeProvider.systemPrompt,
                      )
                    }
                onSendMessage(sess, activeProvider, inputText, pendingAttachment)
                inputText = ""
                pendingAttachment = null
              }
            },
            modifier = Modifier.testTag("api_chat_send_button"),
          ) {
            Icon(
              imageVector = if (isGenerating) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.Send,
              contentDescription = if (isGenerating) "Stop" else "Send",
              tint = if (isGenerating) ErrorRed else neu.accent,
            )
          }
        }
      }
    }
  }

  // Add / Edit Provider Dialog
  if (isAddingProvider || showProviderDialog != null) {
    val target = showProviderDialog
    ApiProviderConfigDialog(
      existing = target,
      existingKey = target?.let { getApiKey(it.id) } ?: "",
      onTestConnection = onTestProvider,
      onSave = { id, name, type, endpoint, model, sysPrompt, key ->
        onSaveProvider(id, name, type, endpoint, model, sysPrompt, key)
        isAddingProvider = false
        showProviderDialog = null
      },
      onDelete = { id ->
        onDeleteProvider(id)
        isAddingProvider = false
        showProviderDialog = null
      },
      onDismiss = {
        isAddingProvider = false
        showProviderDialog = null
      },
    )
  }

  // Chat History Modal Sheet
  if (showHistorySheet) {
    var histSearch by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Chat History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          if (activeSession != null && messages.isNotEmpty()) {
            TextButton(
              onClick = {
                val exported =
                  messages.joinToString("\n\n") { "**${it.role.uppercase()}**:\n${it.content}" }
                val share =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, activeSession.title)
                    putExtra(Intent.EXTRA_TEXT, exported)
                  }
                context.startActivity(Intent.createChooser(share, "Export Chat"))
              }
            ) {
              Text("Export Chat")
            }
          }
        }
        OutlinedTextField(
          value = histSearch,
          onValueChange = { histSearch = it },
          label = { Text("Search chats") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        val filtered =
          sessions.filter {
            histSearch.isBlank() || it.title.contains(histSearch, ignoreCase = true)
          }
        LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
          items(filtered, key = { it.id }) { sess ->
            ListItem(
              headlineContent = { Text(sess.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              supportingContent = { Text(sess.model, style = MaterialTheme.typography.labelSmall) },
              trailingContent = {
                IconButton(onClick = { onDeleteSession(sess.id) }) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete chat", tint = ErrorRed)
                }
              },
              modifier =
                Modifier.fillMaxWidth().clickable {
                  activeSessionId = sess.id
                  showHistorySheet = false
                },
            )
          }
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }

  // Session Model, Temperature, Title, System Prompt Dialog
  if (showSessionSettings && activeSession != null) {
    var titleDraft by remember(activeSession.id) { mutableStateOf(activeSession.title) }
    var modelDraft by remember(activeSession.id) { mutableStateOf(activeSession.model) }
    var promptDraft by remember(activeSession.id) { mutableStateOf(activeSession.systemPrompt) }
    var tempDraft by remember(activeSession.id) { mutableFloatStateOf(activeSession.temperature) }

    AlertDialog(
      onDismissRequest = { showSessionSettings = false },
      title = { Text("Chat Session Settings") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleDraft,
            onValueChange = { titleDraft = it },
            label = { Text("Chat Title") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = modelDraft,
            onValueChange = { modelDraft = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = promptDraft,
            onValueChange = { promptDraft = it },
            label = { Text("System Prompt") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
          Text("Temperature: ${String.format("%.2f", tempDraft)}")
          Slider(value = tempDraft, onValueChange = { tempDraft = it }, valueRange = 0f..1.5f)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateSession(
              activeSession.copy(
                title = titleDraft.ifBlank { activeSession.title },
                model = modelDraft.ifBlank { activeSession.model },
                systemPrompt = promptDraft,
                temperature = tempDraft,
              )
            )
            showSessionSettings = false
          }
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSessionSettings = false }) { Text("Cancel") }
      },
    )
  }

  // Edit User Message Dialog
  editingMessage?.let { targetMsg ->
    var editedText by remember(targetMsg.id) { mutableStateOf(targetMsg.content) }
    AlertDialog(
      onDismissRequest = { editingMessage = null },
      title = { Text("Edit & Resend") },
      text = {
        OutlinedTextField(
          value = editedText,
          onValueChange = { editedText = it },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (activeSession != null && activeProvider != null && editedText.isNotBlank()) {
              onEditMessage(activeSession, activeProvider, targetMsg, editedText, messages)
            }
            editingMessage = null
          }
        ) {
          Text("Resend")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingMessage = null }) { Text("Cancel") }
      },
    )
  }
}
