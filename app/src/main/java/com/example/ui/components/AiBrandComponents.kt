package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.model.AiCategory
import com.example.data.model.AiPlatform
import com.example.data.model.CompatibilityStatus
import com.example.data.model.NetworkStatus
import com.example.data.model.PricingType
import com.example.ui.localization.LocalUiStrings
import com.example.ui.localization.compatName
import com.example.ui.localization.pricingName
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiBrandBadge(
  platform: AiPlatform,
  size: Dp = 46.dp,
  isCircular: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val shape = remember(isCircular, size) { if (isCircular) CircleShape else RoundedCornerShape(size * 0.28f) }
  val innerShape = remember(isCircular, size) { if (isCircular) CircleShape else RoundedCornerShape(size * 0.22f) }
  val gradient = remember(platform.brandColorHex, platform.secondaryColorHex) {
    Brush.linearGradient(colors = listOf(platform.primaryColor, platform.secondaryColor))
  }
  val imageRequest = remember(platform.officialFaviconUrl, context) {
    ImageRequest.Builder(context)
      .data(platform.officialFaviconUrl)
      .size(256)
      .crossfade(false)
      .build()
  }
  val painter = rememberAsyncImagePainter(model = imageRequest, filterQuality = FilterQuality.High)
  val isLoaded = painter.state is AsyncImagePainter.State.Success

  Box(
    modifier =
      modifier
        .size(size)
        .clip(shape)
        .background(gradient)
        .border(1.2.dp, Color.White.copy(alpha = 0.45f), shape),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize(0.86f)
          .clip(innerShape)
          .background(if (isLoaded) Color.White else Color.Transparent),
      contentAlignment = Alignment.Center,
    ) {
      if (!isLoaded) {
        AiFallbackGlyph(platform = platform, size = size, shape = innerShape)
      }
      Image(
        painter = painter,
        contentDescription = "${platform.name} official logo",
        modifier = Modifier.fillMaxSize(0.82f).clip(innerShape),
        contentScale = ContentScale.Fit,
      )
    }
  }
}

@Composable
private fun AiFallbackGlyph(platform: AiPlatform, size: Dp, shape: androidx.compose.ui.graphics.Shape) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.linearGradient(listOf(platform.primaryColor, platform.secondaryColor)),
          shape,
        ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val c = Offset(this.size.width / 2f, this.size.height / 2f)
      val r = this.size.minDimension * 0.42f
      when (platform.category) {
        AiCategory.CODING,
        AiCategory.WEBSITE_BUILDER -> {
          drawCircle(Color.White.copy(alpha = 0.22f), radius = r, center = c, style = Stroke(width = 2.5f))
        }
        AiCategory.IMAGE,
        AiCategory.VIDEO -> {
          drawCircle(Color.White.copy(alpha = 0.24f), radius = r * 0.9f, center = c)
        }
        else -> {
          drawCircle(Color.White.copy(alpha = 0.20f), radius = r, center = c, style = Stroke(width = 2f))
        }
      }
    }
    Text(
      text = platform.monogram.take(2),
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = (size.value * 0.30f).sp,
      fontFamily = SpaceGroteskFontFamily,
    )
  }
}

@Composable
fun PricingBadge(pricingType: PricingType, modifier: Modifier = Modifier) {
  val strings = LocalUiStrings.current
  val (bg, fg) =
    when (pricingType) {
      PricingType.COMPLETELY_FREE -> SuccessGreen.copy(alpha = 0.14f) to SuccessGreen
      PricingType.FREE_AND_PAID -> InfoCyan.copy(alpha = 0.14f) to InfoCyan
      PricingType.PAID -> WarningAmber.copy(alpha = 0.14f) to WarningAmber
    }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = bg,
  ) {
    Text(
      text = strings.pricingName(pricingType),
      color = fg,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun CompatibilityBadge(status: CompatibilityStatus, modifier: Modifier = Modifier) {
  val strings = LocalUiStrings.current
  val (bg, fg) =
    when (status) {
      CompatibilityStatus.IN_APP_WEBVIEW -> SuccessGreen.copy(alpha = 0.14f) to SuccessGreen
      CompatibilityStatus.EXTERNAL_TAB_RECOMMENDED -> WarningAmber.copy(alpha = 0.14f) to WarningAmber
      CompatibilityStatus.COMPATIBILITY_UNKNOWN ->
        LocalNeuColors.current.secondaryText.copy(alpha = 0.14f) to LocalNeuColors.current.secondaryText
    }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = bg,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Public,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = strings.compatName(status),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun NetworkStatusPill(status: NetworkStatus, modifier: Modifier = Modifier) {
  val strings = LocalUiStrings.current
  val fg = if (status.isConnected) SuccessGreen else ErrorRed
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = fg.copy(alpha = 0.14f),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = if (status.isConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
        contentDescription = if (status.isConnected) strings.online else strings.offline,
        tint = fg,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = if (status.isConnected) status.transportLabel else strings.offline,
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

fun formatBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
  val mb = kb / 1024.0
  if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
  val gb = mb / 1024.0
  return String.format(Locale.US, "%.2f GB", gb)
}

fun formatTimestamp(ts: Long, neverText: String = "Never"): String {
  if (ts <= 0L) return neverText
  val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
  return fmt.format(Date(ts))
}
