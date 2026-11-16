package com.example.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiPlatform
import com.example.data.network.AvailabilityResult
import com.example.ui.components.AiBrandBadge
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.PricingBadge
import com.example.ui.localization.UiStrings
import com.example.ui.localization.categoryName
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiDetailsScreen(
  platform: AiPlatform,
  strings: UiStrings,
  availabilityResult: AvailabilityResult?,
  onBack: () -> Unit,
  onStartNow: () -> Unit,
  onOpenOfficialExternal: (String) -> Unit,
  onToggleFavorite: () -> Unit,
  onTogglePin: () -> Unit,
  onCheckAvailability: () -> Unit,
  onEditCustomAi: (() -> Unit)? = null,
  onDuplicateCustomAi: (() -> Unit)? = null,
  onDeleteCustomAi: (() -> Unit)? = null,
) {
  BackHandler(onBack = onBack)
  val neu = LocalNeuColors.current
  var checking by remember { mutableStateOf(false) }

  LaunchedEffect(availabilityResult) { checking = false }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // Top App Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      NeuIconButton(onClick = onBack, modifier = Modifier.testTag("details_back_button")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = neu.primaryText)
      }
      Text(
        text = strings.aiDetailsTitle,
        style = MaterialTheme.typography.titleLarge,
        color = neu.primaryText,
        fontWeight = FontWeight.Bold,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NeuIconButton(onClick = onToggleFavorite) {
          Icon(
            imageVector =
              if (platform.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = strings.favorites,
            tint = if (platform.isFavorite) ErrorRed else neu.primaryText,
          )
        }
        NeuIconButton(onClick = onTogglePin) {
          Icon(
            imageVector = if (platform.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
            contentDescription = strings.pin,
            tint = if (platform.isPinned) neu.accent else neu.primaryText,
          )
        }
      }
    }

    Column(
      modifier =
        Modifier.weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Identity Hero Card
      NeuCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            AiBrandBadge(platform = platform, size = 64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = platform.name,
                style = MaterialTheme.typography.headlineSmall,
                color = neu.primaryText,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = strings.categoryName(platform.category),
                style = MaterialTheme.typography.labelLarge,
                color = neu.accent,
              )
              Text(
                text = platform.officialUrl,
                style = MaterialTheme.typography.labelSmall,
                color = neu.secondaryText,
              )
            }
          }
          Text(
            text = platform.shortDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = neu.primaryText,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PricingBadge(platform.pricingType)
            CompatibilityBadge(platform.compatibility)
          }
        }
      }

      // Main Purpose & Best Use Cases
      NeuCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(strings.mainPurpose, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
          Text(platform.mainPurpose, style = MaterialTheme.typography.bodyMedium, color = neu.secondaryText)
          HorizontalDivider(color = neu.border.copy(alpha = 0.5f))
          Text(strings.bestUseCases, style = MaterialTheme.typography.titleSmall, color = neu.primaryText, fontWeight = FontWeight.SemiBold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            platform.bestUseCases.forEach { uc ->
              AssistChip(onClick = {}, label = { Text(uc) })
            }
          }
          Text(strings.keyFeatures, style = MaterialTheme.typography.titleSmall, color = neu.primaryText, fontWeight = FontWeight.SemiBold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            platform.features.forEach { feat ->
              SuggestionChip(onClick = {}, label = { Text(feat) })
            }
          }
        }
      }

      // Capabilities Support Matrix
      NeuCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.capabilitiesTitle, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CapabilityTag(strings.capCoding, platform.capabilities.coding)
            CapabilityTag(strings.capImage, platform.capabilities.image)
            CapabilityTag(strings.capVideo, platform.capabilities.video)
            CapabilityTag(strings.capAudio, platform.capabilities.audio)
            CapabilityTag(strings.capFileUpload, platform.capabilities.fileUpload)
          }
        }
      }

      // Technical & Access Details
      NeuCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          DetailRow(strings.pricingDetailsLabel, platform.pricingDetails)
          DetailRow(strings.loginReqLabel, platform.loginRequirement)
          DetailRow(strings.apiAvailabilityLabel, platform.apiAvailability)
          DetailRow(strings.approvedOriginsLabel, platform.approvedOrigins.joinToString(", "))
          DetailRow(strings.knownLimitationsLabel, platform.knownLimitations)
          DetailRow(strings.lastVerifiedLabel, platform.lastVerifiedDate)
        }
      }

      // Live Website Availability Check
      NeuCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              strings.webAvailabilityTitle,
              style = MaterialTheme.typography.titleSmall,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            SpringOutlinedButton(
              onClick = {
                checking = true
                onCheckAvailability()
              },
              enabled = !checking,
            ) {
              if (checking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
              }
              Text(strings.checkLiveBtn)
            }
          }
          if (availabilityResult != null) {
            Text(
              text = availabilityResult.message,
              style = MaterialTheme.typography.bodySmall,
              color = if (availabilityResult.isReachable) SuccessGreen else ErrorRed,
              fontWeight = FontWeight.SemiBold,
            )
          } else {
            Text(
              text = strings.checkLiveHint,
              style = MaterialTheme.typography.bodySmall,
              color = neu.secondaryText,
            )
          }
        }
      }

      // Custom AI Management Controls
      if (platform.isCustom) {
        NeuCard(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            if (onEditCustomAi != null) {
              SpringTextButton(onClick = onEditCustomAi) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.edit)
              }
            }
            if (onDuplicateCustomAi != null) {
              SpringTextButton(onClick = onDuplicateCustomAi) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.duplicate)
              }
            }
            if (onDeleteCustomAi != null) {
              SpringTextButton(onClick = onDeleteCustomAi) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.delete, color = ErrorRed)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }

    // Bottom Action Bar with START NOW + Official Website
    Surface(
      color = neu.card,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SpringOutlinedButton(
          onClick = { onOpenOfficialExternal(platform.officialUrl) },
          modifier = Modifier.weight(1f).height(52.dp).testTag("details_external_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.officialWebsite, maxLines = 1)
        }
        SpringButton(
          onClick = onStartNow,
          modifier = Modifier.weight(1.25f).height(52.dp).testTag("details_start_now_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
          contentPadding = PaddingValues(),
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(listOf(neu.accent, neu.accentSecondary)),
                  RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(strings.startNow, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CapabilityTag(label: String, supported: Boolean) {
  val fg = if (supported) SuccessGreen else LocalNeuColors.current.secondaryText
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = fg.copy(alpha = if (supported) 0.14f else 0.08f),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = if (supported) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(14.dp),
      )
      Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
  }
}

@Composable
private fun DetailRow(title: String, value: String) {
  val neu = LocalNeuColors.current
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = neu.primaryText, fontWeight = FontWeight.SemiBold)
    Text(value, style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
  }
}
