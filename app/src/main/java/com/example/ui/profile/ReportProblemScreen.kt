package com.example.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.*

private val reportCategories =
  listOf(
    "UI Problem",
    "Wrong AI Website",
    "Wrong URL",
    "Website Not Opening",
    "Refresh Problem",
    "Login Problem",
    "Upload Problem",
    "Download Problem",
    "Workspace Problem",
    "API Problem",
    "Security Problem",
    "Other",
  )

@Composable
fun ReportProblemScreen(
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  var selectedCategory by remember { mutableStateOf(reportCategories.first()) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var screenshotUri by remember { mutableStateOf<Uri?>(null) }
  var includeDeviceInfo by remember { mutableStateOf(true) }

  val screenshotPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      screenshotUri = uri
    }

  val deviceInfo = remember {
    "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) • ${Build.MANUFACTURER} ${Build.MODEL}"
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
        Text(strings.reportProblem, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
      }
    }

    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(strings.categoryLabel, style = MaterialTheme.typography.labelLarge, color = neu.primaryText)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reportCategories.forEach { cat ->
          SpringFilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
        }
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(strings.problemTitleLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text(strings.detailedDescLabel) },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )

      NeuCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.optionalScreenshot, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
            Text(
              screenshotUri?.lastPathSegment ?: strings.noScreenshot,
              style = MaterialTheme.typography.bodySmall,
              color = neu.secondaryText,
            )
          }
          SpringOutlinedButton(
            onClick = {
              screenshotPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
          ) {
            Text(if (screenshotUri == null) strings.attachBtn else strings.changeBtn)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(strings.includeDeviceInfo, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
          Text(deviceInfo, style = MaterialTheme.typography.labelSmall, color = neu.secondaryText)
        }
        Switch(checked = includeDeviceInfo, onCheckedChange = { includeDeviceInfo = it })
      }

      Text(
        "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Recipient: tkofficial68@gmail.com",
        style = MaterialTheme.typography.bodySmall,
        color = neu.secondaryText,
      )

      SpringButton(
        onClick = {
          if (title.isBlank() || description.isBlank()) {
            Toast.makeText(context, strings.enterTitleAndDesc, Toast.LENGTH_SHORT).show()
            return@SpringButton
          }
          val body =
            buildString {
              appendLine("This report has been generated from the AI UNIVERSE Web Application.")
              appendLine()
              appendLine("Platform: Android Native App (${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE})")
              appendLine("Category: $selectedCategory")
              appendLine("Title: $title")
              appendLine("Description:")
              appendLine(description)
              if (includeDeviceInfo) {
                appendLine()
                appendLine("Device Info: $deviceInfo")
              }
            }

          val intent =
            if (screenshotUri != null) {
              Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("tkofficial68@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "AI UNIVERSE - User Problem Report")
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, screenshotUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
            } else {
              Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:tkofficial68@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "AI UNIVERSE - User Problem Report")
                putExtra(Intent.EXTRA_TEXT, body)
              }
            }
          runCatching { context.startActivity(Intent.createChooser(intent, strings.reportProblem)) }
            .onFailure {
              Toast.makeText(context, "Email: tkofficial68@gmail.com", Toast.LENGTH_LONG).show()
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Rounded.Email, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.prepareEmailReport)
      }
    }
  }
}
