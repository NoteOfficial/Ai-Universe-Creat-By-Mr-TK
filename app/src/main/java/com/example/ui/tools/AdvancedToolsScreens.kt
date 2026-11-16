package com.example.ui.tools

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityLogEntity
import com.example.data.model.*
import com.example.data.network.UrlValidator
import com.example.ui.components.AiBrandBadge
import com.example.ui.components.PricingBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.localization.LocalUiStrings
import com.example.ui.localization.categoryName
import com.example.ui.localization.pricingName
import com.example.ui.theme.*

@Composable
fun AddEditCustomAiScreen(
  existing: AiPlatform?,
  onSave: (String?, String, String, String, AiCategory, PricingType, String, Long) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  var name by remember { mutableStateOf(existing?.name ?: "") }
  var url by remember { mutableStateOf(existing?.officialUrl ?: "https://") }
  var description by remember { mutableStateOf(existing?.shortDescription ?: "") }
  var category by remember { mutableStateOf(existing?.category ?: AiCategory.OTHER) }
  var pricingType by remember { mutableStateOf(existing?.pricingType ?: PricingType.FREE_AND_PAID) }
  var pricingDetails by remember { mutableStateOf(existing?.pricingDetails ?: "") }
  var selectedColor by remember { mutableLongStateOf(existing?.brandColorHex ?: 0xFF536DFE) }

  val palette = listOf(0xFF536DFE, 0xFF10A37F, 0xFF9333EA, 0xFFE11D48, 0xFFF26207, 0xFF0078D4, 0xFF20808D)

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
        Text(
          if (existing == null) strings.addCustomAi else "${strings.edit} ${existing.name}",
          style = MaterialTheme.typography.titleMedium,
          color = neu.primaryText,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("AI Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = url,
        onValueChange = { url = it },
        label = { Text("Official Website URL (https://...)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      Text(strings.categoryLabel, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiCategory.entries.forEach { cat ->
          SpringFilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(strings.categoryName(cat)) })
        }
      }

      Text(strings.pricingDetailsLabel, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PricingType.entries.forEach { pt ->
          SpringFilterChip(selected = pricingType == pt, onClick = { pricingType = pt }, label = { Text(strings.pricingName(pt)) })
        }
      }

      OutlinedTextField(
        value = pricingDetails,
        onValueChange = { pricingDetails = it },
        label = { Text("Pricing Details (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Badge Accent Color", style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        palette.forEach { hex ->
          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(CircleShape)
                .background(Color(hex))
                .border(
                  width = if (selectedColor == hex) 3.dp else 0.dp,
                  color = neu.primaryText,
                  shape = CircleShape,
                )
                .clickable { selectedColor = hex }
          )
        }
      }

      Spacer(Modifier.height(8.dp))
      Button(
        onClick = {
          val normalized = UrlValidator.normalizeUrl(url)
          if (name.isBlank() || !UrlValidator.isValidHttpsUrl(normalized)) {
            Toast.makeText(context, "Please enter a valid AI name and HTTPS URL", Toast.LENGTH_SHORT).show()
          } else {
            onSave(existing?.id, name, normalized, description.ifBlank { "Custom AI Website" }, category, pricingType, pricingDetails, selectedColor)
            onBack()
          }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text("Save Website AI")
      }
    }
  }
}

@Composable
fun AiComparisonScreen(
  platforms: List<AiPlatform>,
  onLaunchAi: (AiPlatform) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current
  var selectedIds by remember {
    mutableStateOf(platforms.take(2).map { it.id })
  }
  val compared = remember(platforms, selectedIds) { platforms.filter { it.id in selectedIds } }

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
        Text("${strings.aiComparisonTitle} (${compared.size}/3)", style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
      }
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Select 2–3 platforms to compare", style = MaterialTheme.typography.labelLarge, color = neu.secondaryText)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        platforms.forEach { ai ->
          val active = ai.id in selectedIds
          FilterChip(
            selected = active,
            onClick = {
              selectedIds =
                if (active) selectedIds - ai.id
                else if (selectedIds.size < 3) selectedIds + ai.id
                else selectedIds.drop(1) + ai.id
            },
            label = { Text(ai.name) },
          )
        }
      }

      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
        items(compared, key = { it.id }) { ai ->
          NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  AiBrandBadge(platform = ai, size = 38.dp)
                  Column {
                    Text(ai.name, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
                    Text(ai.category.label, style = MaterialTheme.typography.labelSmall, color = neu.secondaryText)
                  }
                }
                PricingBadge(ai.pricingType)
              }
              Text("Purpose: ${ai.mainPurpose}", style = MaterialTheme.typography.bodySmall, color = neu.primaryText)
              Text("Capabilities: " +
                listOfNotNull(
                  if (ai.capabilities.coding) "Coding" else null,
                  if (ai.capabilities.image) "Image" else null,
                  if (ai.capabilities.video) "Video" else null,
                  if (ai.capabilities.audio) "Audio" else null,
                  if (ai.capabilities.fileUpload) "Files" else null,
                ).joinToString(" • ").ifBlank { "Text Chat" },
                style = MaterialTheme.typography.labelMedium,
                color = neu.accent,
              )
              Text("Login: ${ai.loginRequirement}", style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
              Text("Limitations: ${ai.knownLimitations}", style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
              Button(onClick = { onLaunchAi(ai) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open ${ai.name}")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun GlobalClipboardScreen(
  items: List<ClipboardItem>,
  onCopy: (String, String) -> Unit,
  onDelete: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

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
        Text("${strings.globalClipboardTitle} (${items.size})", style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
      }
    }

    if (items.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = neu.secondaryText, modifier = Modifier.size(44.dp))
            Text(strings.clipboardEmpty, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
          }
        }
      }
    } else {
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { item ->
          NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.label, style = MaterialTheme.typography.labelLarge, color = neu.accent, fontWeight = FontWeight.Bold)
                Text(formatTimestamp(item.timestamp, strings.never), style = MaterialTheme.typography.labelSmall, color = neu.secondaryText)
              }
              Text(item.content, style = MaterialTheme.typography.bodySmall, color = neu.primaryText)
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                  onClick = {
                    onCopy(item.label, item.content)
                    Toast.makeText(context, "${strings.copyAgain} ✓", Toast.LENGTH_SHORT).show()
                  }
                ) {
                  Text(strings.copyAgain)
                }
                TextButton(onClick = { onDelete(item.id) }) { Text(strings.removeLabel, color = ErrorRed) }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun NotificationsCenterScreen(
  logs: List<ActivityLogEntity>,
  onMarkAllRead: () -> Unit,
  onClearAll: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  LaunchedEffect(Unit) { onMarkAllRead() }

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
        Text(
          strings.notificationsCenterTitle,
          style = MaterialTheme.typography.titleMedium,
          color = neu.primaryText,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        if (logs.isNotEmpty()) {
          TextButton(onClick = onClearAll) { Text(strings.clearAll, color = ErrorRed) }
        }
      }
    }

    if (logs.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = neu.secondaryText, modifier = Modifier.size(44.dp))
            Text(strings.noNotifications, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
          }
        }
      }
    } else {
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(logs, key = { it.id }) { log ->
          NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(
                imageVector = if (log.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircleOutline,
                contentDescription = null,
                tint = if (log.isError) ErrorRed else SuccessGreen,
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("${log.category} • ${log.title}", style = MaterialTheme.typography.titleSmall, color = neu.primaryText, fontWeight = FontWeight.Bold)
                }
                Text(log.message, style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
                Text(formatTimestamp(log.timestamp, strings.never), style = MaterialTheme.typography.labelSmall, color = neu.secondaryText)
              }
            }
          }
        }
      }
    }
  }
}
