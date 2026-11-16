package com.example.ui.apichat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.ApiProviderEntity
import com.example.data.model.ApiProviderType
import com.example.data.network.ApiResponseResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProviderConfigDialog(
  existing: ApiProviderEntity?,
  existingKey: String,
  onTestConnection: suspend (ApiProviderType, String, String, String) -> ApiResponseResult,
  onSave: (String?, String, ApiProviderType, String, String, String, String) -> Unit,
  onDelete: ((String) -> Unit)?,
  onDismiss: () -> Unit,
) {
  val initialType =
    existing?.let { runCatching { ApiProviderType.valueOf(it.providerType) }.getOrNull() }
      ?: ApiProviderType.OPENAI

  var providerType by remember { mutableStateOf(initialType) }
  var displayName by remember { mutableStateOf(existing?.displayName ?: initialType.displayName) }
  var endpoint by remember { mutableStateOf(existing?.endpoint ?: initialType.defaultEndpoint) }
  var model by remember { mutableStateOf(existing?.model ?: initialType.defaultModel) }
  var systemPrompt by remember { mutableStateOf(existing?.systemPrompt ?: "You are a helpful AI assistant.") }
  var apiKey by remember { mutableStateOf(existingKey) }
  var showKey by remember { mutableStateOf(false) }
  var testing by remember { mutableStateOf(false) }
  var testStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Add API AI Provider" else "Edit API Provider") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          "Your API key is encrypted locally via Android EncryptedSharedPreferences and sent only directly to the provider's official HTTPS endpoint.",
          style = MaterialTheme.typography.bodySmall,
          color = LocalNeuColors.current.secondaryText,
        )

        // Provider Selector
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ApiProviderType.entries.forEach { type ->
            FilterChip(
              selected = providerType == type,
              onClick = {
                providerType = type
                if (existing == null) {
                  displayName = type.displayName
                  endpoint = type.defaultEndpoint
                  model = type.defaultModel
                }
              },
              label = { Text(type.displayName) },
            )
          }
        }

        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("Display Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text("API Key") },
          singleLine = true,
          visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showKey = !showKey }) {
              Icon(
                if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = "Toggle key visibility",
              )
            }
          },
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = endpoint,
          onValueChange = { endpoint = it },
          label = { Text("Endpoint URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = model,
          onValueChange = { model = it },
          label = { Text("Model ID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // Quick model suggestions
        if (providerType.availableModels.size > 1) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            providerType.availableModels.forEach { m ->
              AssistChip(onClick = { model = m }, label = { Text(m) })
            }
          }
        }

        OutlinedTextField(
          value = systemPrompt,
          onValueChange = { systemPrompt = it },
          label = { Text("Default System Prompt") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = {
              testing = true
              testStatus = null
              scope.launch {
                val res = onTestConnection(providerType, endpoint, model, apiKey)
                testing = false
                testStatus =
                  when (res) {
                    is ApiResponseResult.Success -> true to "Connected: ${res.text.take(60)}"
                    is ApiResponseResult.Error -> false to "${res.title}: ${res.detail}"
                  }
              }
            },
            enabled = !testing,
          ) {
            if (testing) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(6.dp))
            }
            Text("Test Connection")
          }
          if (existing != null && onDelete != null) {
            TextButton(onClick = { onDelete(existing.id) }) {
              Text("Delete", color = ErrorRed)
            }
          }
        }

        testStatus?.let { (ok, msg) ->
          Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color = if (ok) SuccessGreen else ErrorRed,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(existing?.id, displayName, providerType, endpoint, model, systemPrompt, apiKey)
        },
        enabled = displayName.isNotBlank() && endpoint.isNotBlank() && model.isNotBlank(),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun FormattedMessageContent(
  content: String,
  textColor: Color,
  onCopyCode: (String) -> Unit,
) {
  val neu = LocalNeuColors.current
  val segments = remember(content) { parseMarkdownCodeBlocks(content) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    segments.forEach { seg ->
      if (seg.isCode) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (neu.isDark) Color(0xFF141519) else Color(0xFF1E2430))
              .padding(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = seg.language.ifBlank { "code" },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD),
            )
            IconButton(
              onClick = { onCopyCode(seg.text) },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "Copy code",
                tint = Color.White,
                modifier = Modifier.size(15.dp),
              )
            }
          }
          Spacer(Modifier.height(4.dp))
          Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
              text = seg.text,
              fontFamily = JetBrainsMonoFontFamily,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFE5E7EB),
            )
          }
        }
      } else {
        Text(
          text = seg.text,
          style = MaterialTheme.typography.bodyMedium,
          color = textColor,
        )
      }
    }
  }
}

private data class MessageSegment(
  val text: String,
  val isCode: Boolean,
  val language: String = "",
)

private fun parseMarkdownCodeBlocks(raw: String): List<MessageSegment> {
  val regex = Regex("```([a-zA-Z0-9_-]*)\\n([\\s\\S]*?)```")
  val result = mutableListOf<MessageSegment>()
  var lastIndex = 0
  regex.findAll(raw).forEach { match ->
    if (match.range.first > lastIndex) {
      val plain = raw.substring(lastIndex, match.range.first).trim()
      if (plain.isNotEmpty()) result.add(MessageSegment(plain, isCode = false))
    }
    result.add(
      MessageSegment(
        text = match.groupValues[2].trimEnd(),
        isCode = true,
        language = match.groupValues[1],
      )
    )
    lastIndex = match.range.last + 1
  }
  if (lastIndex < raw.length) {
    val tail = raw.substring(lastIndex).trim()
    if (tail.isNotEmpty()) result.add(MessageSegment(tail, isCode = false))
  }
  if (result.isEmpty()) result.add(MessageSegment(raw, isCode = false))
  return result
}
