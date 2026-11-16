package com.example.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.NeuCard

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
  BackHandler(onBack = onBack)
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current
  val isBangla = strings.navHome == "হোম"

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
        Text(strings.aboutApp, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
      }
    }

    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      NeuCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          Image(
            painter = painterResource(id = R.drawable.ic_launcher_fg_art),
            contentDescription = "AI UNIVERSE Logo",
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
          )
          Column {
            Text("AI UNIVERSE", style = MaterialTheme.typography.headlineSmall, color = neu.primaryText, fontWeight = FontWeight.Bold)
            Text(
              "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${BuildConfig.APPLICATION_ID}",
              style = MaterialTheme.typography.labelMedium,
              color = neu.accent,
            )
            Text("Developer: Mr TK (＞━╬٨-ﮩﮩT♡ᴋ٨ـﮩﮩـ╬━＜)", style = MaterialTheme.typography.bodySmall, color = neu.secondaryText)
          }
        }
      }

      if (isBangla) {
        InfoSectionCard(
          "মূল ফিচারসমূহ",
          "• ২৯+ ভেরিফায়েড অফিসিয়াল এআই প্ল্যাটফর্ম ও আনলিমিটেড কাস্টম এআই।\n" +
            "• প্রতিটি এআই-এর জন্য আলাদা ওয়েবভিউ ওয়ার্কস্পেস ও পৃথক রিফ্রেশ সুবিধা।\n" +
            "• ট্যাবলেট ও বড় স্ক্রিনে স্প্লিট-স্ক্রিন ওয়ার্কস্পেস।\n" +
            "• নিজের API Key (OpenAI, Gemini, Claude, DeepSeek) দিয়ে সরাসরি API চ্যাট।\n" +
            "• ফাইল ম্যানেজার, প্রজেক্ট ম্যানেজার এবং ৫টি রঙিন থিম।",
        )
        InfoSectionCard(
          "নিরাপত্তা ও প্রাইভেসি",
          "• API Key এবং অ্যাপ লক PIN আপনার ডিভাইসে এনক্রিপ্টেড অবস্থায় থাকে।\n" +
            "• প্রতিটি ওয়েবসাইটের আসল ডোমেইন যাচাই ও HTTPS নিরাপত্তা।\n" +
            "• অ্যাপ ক্লোন ও মডিফাই প্রতিরোধে সিগনেচার ও প্যাকেজ সুরক্ষা।",
        )
        InfoSectionCard(
          "যোগাযোগ ও কপিরাইট",
          "Telegram: t.me/tkParsonalChatBot\nEmail: tkofficial68@gmail.com\n© 2026 Mr TK. সর্বস্বত্ব সংরক্ষিত।",
        )
      } else {
        InfoSectionCard(
          "Core Features",
          "• Verified directory of 29+ official AI websites plus unlimited custom AI entries.\n" +
            "• Independent native Android WebView workspaces with isolated per-workspace Refresh and Chrome Custom Tab fallback.\n" +
            "• Side-by-side Split-Screen Workspace on tablets & foldables.\n" +
            "• Bring Your Own Key (BYOK) Native API Chat for OpenAI, Google Gemini, Anthropic Claude, DeepSeek, and Custom endpoints.\n" +
            "• Universal File Manager (Photo Picker + SAF), Project Manager, and 5 colorful Material themes.",
        )
        InfoSectionCard(
          "Security & Privacy",
          "• BYOK API keys and optional App Lock PIN are stored locally using Android EncryptedSharedPreferences.\n" +
            "• Official AI URLs are validated against approved origins before loading.\n" +
            "• Package & APK signature integrity checks plus R8 obfuscation protect the app against unauthorized modification.",
        )
        InfoSectionCard(
          "Permissions",
          "• INTERNET & ACCESS_NETWORK_STATE for loading verified AI websites, live availability checks, and BYOK API requests.\n" +
            "• Zero-permission Android Photo Picker, Storage Access Framework, and Speech Recognizer intents are used for media, files, and voice.",
        )
        InfoSectionCard(
          "Contact & Copyright",
          "Telegram: t.me/tkParsonalChatBot\nEmail: tkofficial68@gmail.com\n© 2026 Mr TK. All rights reserved.",
        )
      }
    }
  }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
  BackHandler(onBack = onBack)
  val strings = LocalUiStrings.current
  val isBangla = strings.navHome == "হোম"
  LegalDocumentScreen(
    title = strings.privacyPolicy,
    sections =
      if (isBangla) {
        listOf(
          "১. লোকাল স্টোরেজ" to
            "AI UNIVERSE আপনার কাস্টম এআই, ওয়ার্কস্পেস তথ্য, প্রজেক্ট লিংক, ফাইল এবং চ্যাট ইতিহাস আপনার ফোনেই (Room ও DataStore) সংরক্ষণ করে।",
          "২. এনক্রিপ্টেড API Key" to
            "OpenAI, Gemini, Claude বা DeepSeek-এর API Key ফোনে এনক্রিপ্টেড থাকে এবং সরাসরি প্রোভাইডারের অফিসিয়াল HTTPS সার্ভারে যায়।",
          "৩. তৃতীয় পক্ষের ওয়েবসাইট" to
            "কোনো এআই ওয়েবসাইট ওপেন করলে আপনি সরাসরি সেই প্ল্যাটফর্মের সাথে যুক্ত হন এবং তাদের প্রাইভেসি নীতি প্রযোজ্য হয়।",
          "৪. কোনো গোপন ট্র্যাকিং নেই" to
            "AI UNIVERSE আপনার ব্যক্তিগত চ্যাট বা ডাটা গোপনে কোনো সার্ভারে পাঠায় না।",
        )
      } else {
        listOf(
          "1. Local-First Data" to
            "AI UNIVERSE stores your custom AI entries, workspace metadata, project links, file vault, and API chat history locally on your Android device using Room and DataStore.",
          "2. Encrypted API Keys" to
            "API keys you configure for OpenAI, Google Gemini, Anthropic Claude, DeepSeek, or custom endpoints are encrypted on-device via Android EncryptedSharedPreferences and sent directly from your device to the provider's official HTTPS endpoint.",
          "3. Third-Party AI Websites" to
            "When you open an official AI platform inside a Workspace or Chrome Custom Tab, you interact directly with that third-party provider. Each third-party AI provider has its own privacy policy, terms, and cookie controls managed by Android Chromium CookieManager.",
          "4. No Secret Telemetry" to
            "AI UNIVERSE does not collect hidden analytics, does not silently transmit crash reports, and never reads or scrapes private third-party AI chats.",
        )
      },
    onBack = onBack,
  )
}

@Composable
fun TermsConditionsScreen(onBack: () -> Unit) {
  BackHandler(onBack = onBack)
  val strings = LocalUiStrings.current
  val isBangla = strings.navHome == "হোম"
  LegalDocumentScreen(
    title = strings.termsConditions,
    sections =
      if (isBangla) {
        listOf(
          "১. অফিসিয়াল ওয়েবসাইট ও ট্রেডমার্ক" to
            "AI UNIVERSE তৃতীয় পক্ষের অফিসিয়াল এআই প্ল্যাটফর্মগুলোর লিংক একত্র করে। সকল ট্রেডমার্ক তাদের মূল মালিকদের।",
          "২. ব্যবহারকারীর দায়িত্ব" to
            "তৃতীয় পক্ষের এআই ওয়েবসাইট বা API ব্যবহারের ক্ষেত্রে তাদের শর্তাবলী ও লিমিট মেনে চলার দায়িত্ব ব্যবহারকারীর।",
          "৩. নিরাপত্তা ও আসল ডোমেইন" to
            "অপরিচিত বা অননুমোদিত ডোমেইনে কখনোই লগইন তথ্য দেবেন না।",
          "৪. যোগাযোগ" to
            "সহায়তা বা মতামতের জন্য Mr TK-এর সাথে যোগাযোগ করুন: tkofficial68@gmail.com বা t.me/tkParsonalChatBot।",
        )
      } else {
        listOf(
          "1. Official Websites & Trademarks" to
            "AI UNIVERSE organizes verified links to third-party AI platforms. All third-party names, logos, and services belong to their respective owners.",
          "2. User Responsibility" to
            "You are responsible for complying with the terms of service, pricing, and usage limits of any third-party AI website or API provider you connect.",
          "3. Security & Authenticity" to
            "Do not enter credentials into unverified origins. AI UNIVERSE provides origin validation indicators to help you confirm official domains.",
          "4. Contact" to
            "For support or feedback, contact Mr TK at tkofficial68@gmail.com or t.me/tkParsonalChatBot.",
        )
      },
    onBack = onBack,
  )
}

@Composable
private fun LegalDocumentScreen(
  title: String,
  sections: List<Pair<String, String>>,
  onBack: () -> Unit,
) {
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
        Text(title, style = MaterialTheme.typography.titleMedium, color = neu.primaryText, fontWeight = FontWeight.Bold)
      }
    }
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      sections.forEach { (heading, body) -> InfoSectionCard(heading, body) }
    }
  }
}

@Composable
private fun InfoSectionCard(heading: String, body: String) {
  val neu = LocalNeuColors.current
  NeuCard(modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(heading, style = MaterialTheme.typography.titleSmall, color = neu.primaryText, fontWeight = FontWeight.Bold)
      Text(body, style = MaterialTheme.typography.bodyMedium, color = neu.secondaryText)
    }
  }
}
