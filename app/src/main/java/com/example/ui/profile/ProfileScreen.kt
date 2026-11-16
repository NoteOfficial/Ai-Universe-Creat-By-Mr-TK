package com.example.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.localization.UiStrings
import com.example.ui.theme.*
import com.example.ui.workspace.CustomTabHelper

@Composable
fun ProfileScreen(
  strings: UiStrings,
  onOpenSettings: () -> Unit,
  onOpenAbout: () -> Unit,
  onReplayTutorial: () -> Unit,
  onOpenReportProblem: () -> Unit,
  onOpenPrivacyPolicy: () -> Unit,
  onOpenTerms: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val neu = LocalNeuColors.current

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(neu.background)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = strings.navProfile,
      style = MaterialTheme.typography.headlineMedium,
      color = neu.primaryText,
      fontWeight = FontWeight.Bold,
    )

    // Developer Identity Card
    NeuCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier.size(104.dp)
              .clip(CircleShape)
              .background(Color.Black)
              .border(
                width = 3.dp,
                brush = Brush.linearGradient(listOf(neu.accent, neu.accentSecondary)),
                shape = CircleShape,
              )
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_tk_official_svg),
            contentDescription = "Mr TK Official Profile Logo SVG",
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentScale = ContentScale.Fit,
          )
        }

        Text(
          text = "Mr TK",
          style = MaterialTheme.typography.headlineSmall,
          color = neu.primaryText,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "＞━╬٨-ﮩﮩT♡ᴋ٨ـﮩﮩـ╬━＜",
          style = MaterialTheme.typography.titleSmall,
          color = neu.accent,
        )
        Text(
          text = strings.tagline,
          style = MaterialTheme.typography.bodySmall,
          color = neu.secondaryText,
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SpringButton(
            onClick = { CustomTabHelper.openOfficialWebsite(context, "https://t.me/tkParsonalChatBot") },
            modifier = Modifier.weight(1f).testTag("profile_telegram_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Telegram")
          }
          SpringOutlinedButton(
            onClick = {
              val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                  data = Uri.parse("mailto:tkofficial68@gmail.com")
                  putExtra(Intent.EXTRA_SUBJECT, "AI UNIVERSE Contact")
                }
              runCatching { context.startActivity(intent) }.onFailure {
                Toast.makeText(context, "Email: tkofficial68@gmail.com", Toast.LENGTH_LONG).show()
              }
            },
            modifier = Modifier.weight(1f).testTag("profile_email_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Email")
          }
        }
      }
    }

    // Contact Details Info
    NeuCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Rounded.AlternateEmail, contentDescription = null, tint = neu.accent)
          Column {
            Text("Telegram", style = MaterialTheme.typography.labelMedium, color = neu.secondaryText)
            Text("t.me/tkParsonalChatBot", style = MaterialTheme.typography.bodyMedium, color = neu.primaryText)
          }
        }
        HorizontalDivider(color = neu.border.copy(alpha = 0.5f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Rounded.MailOutline, contentDescription = null, tint = neu.accentSecondary)
          Column {
            Text("Email", style = MaterialTheme.typography.labelMedium, color = neu.secondaryText)
            Text("tkofficial68@gmail.com", style = MaterialTheme.typography.bodyMedium, color = neu.primaryText)
          }
        }
      }
    }

    // Colorful Menu Actions
    ProfileMenuRow(
      title = strings.settings,
      subtitle = strings.profileSettingsSub,
      icon = Icons.Rounded.Settings,
      accentTint = VibrantPaletteAccents[0],
      onClick = onOpenSettings,
      testTag = "profile_settings_item",
    )
    ProfileMenuRow(
      title = strings.aboutApp,
      subtitle = strings.profileAboutSub,
      icon = Icons.Rounded.Info,
      accentTint = VibrantPaletteAccents[1],
      onClick = onOpenAbout,
      testTag = "profile_about_item",
    )
    ProfileMenuRow(
      title = strings.tutorial,
      subtitle = strings.profileTutorialSub,
      icon = Icons.Rounded.School,
      accentTint = VibrantPaletteAccents[2],
      onClick = onReplayTutorial,
      testTag = "profile_tutorial_item",
    )
    ProfileMenuRow(
      title = strings.reportProblem,
      subtitle = strings.profileReportSub,
      icon = Icons.Rounded.BugReport,
      accentTint = VibrantPaletteAccents[3],
      onClick = onOpenReportProblem,
      testTag = "profile_report_item",
    )
    ProfileMenuRow(
      title = strings.privacyPolicy,
      subtitle = strings.profilePrivacySub,
      icon = Icons.Rounded.PrivacyTip,
      accentTint = VibrantPaletteAccents[5],
      onClick = onOpenPrivacyPolicy,
      testTag = "profile_privacy_item",
    )
    ProfileMenuRow(
      title = strings.termsConditions,
      subtitle = strings.profileTermsSub,
      icon = Icons.Rounded.Gavel,
      accentTint = VibrantPaletteAccents[6],
      onClick = onOpenTerms,
      testTag = "profile_terms_item",
    )
  }
}

@Composable
private fun ProfileMenuRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentTint: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val neu = LocalNeuColors.current
  NeuCard(
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    cornerRadius = 18.dp,
    elevated = false,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(42.dp)
            .clip(CircleShape)
            .background(accentTint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = accentTint)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = neu.primaryText, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
      }
      Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = neu.secondaryText)
    }
  }
}
