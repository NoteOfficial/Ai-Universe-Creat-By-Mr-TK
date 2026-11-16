package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AiCategory(val label: String) {
  GENERAL("General AI"),
  CODING("Coding AI"),
  WEBSITE_BUILDER("Website Builder"),
  IMAGE("Image Generation"),
  VIDEO("Video Generation"),
  AUDIO("Audio & Music"),
  RESEARCH("Research"),
  PRODUCTIVITY("Productivity"),
  OTHER("Other"),
}

enum class PricingType(val badge: String) {
  COMPLETELY_FREE("Completely Free"),
  FREE_AND_PAID("Free + Paid"),
  PAID("Paid"),
}

enum class CompatibilityStatus(val badge: String) {
  IN_APP_WEBVIEW("In-App WebView"),
  EXTERNAL_TAB_RECOMMENDED("External Tab Recommended"),
  COMPATIBILITY_UNKNOWN("Compatibility Unknown"),
}

@Immutable
data class AiCapabilities(
  val coding: Boolean = false,
  val image: Boolean = false,
  val video: Boolean = false,
  val audio: Boolean = false,
  val fileUpload: Boolean = false,
)

@Immutable
data class AiPlatform(
  val id: String,
  val name: String,
  val officialUrl: String,
  val approvedOrigins: List<String>,
  val monogram: String,
  val brandColorHex: Long,
  val secondaryColorHex: Long,
  val category: AiCategory,
  val shortDescription: String,
  val mainPurpose: String,
  val bestUseCases: List<String>,
  val features: List<String>,
  val capabilities: AiCapabilities,
  val pricingType: PricingType,
  val pricingDetails: String,
  val loginRequirement: String,
  val apiAvailability: String,
  val compatibility: CompatibilityStatus,
  val externalBrowserSupported: Boolean = true,
  val knownLimitations: String,
  val lastVerifiedDate: String = "2026-11-13",
  val isCustom: Boolean = false,
  val isFavorite: Boolean = false,
  val isPinned: Boolean = false,
  val customOrder: Int = 999,
) {
  val primaryColor: Color
    get() = Color(brandColorHex)

  val secondaryColor: Color
    get() = Color(secondaryColorHex)

  val officialFaviconUrl: String by lazy(LazyThreadSafetyMode.NONE) {
    val host =
      runCatching { android.net.Uri.parse(officialUrl).host }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: approvedOrigins.firstOrNull()
        ?: "google.com"
    "https://www.google.com/s2/favicons?domain=$host&sz=256"
  }
}

enum class ApiProviderType(
  val displayName: String,
  val defaultEndpoint: String,
  val defaultModel: String,
  val availableModels: List<String>,
) {
  OPENAI(
    displayName = "OpenAI",
    defaultEndpoint = "https://api.openai.com/v1/chat/completions",
    defaultModel = "gpt-4o-mini",
    availableModels = listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1", "o3-mini"),
  ),
  GEMINI(
    displayName = "Google Gemini",
    defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta",
    defaultModel = "gemini-2.5-flash",
    availableModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"),
  ),
  CLAUDE(
    displayName = "Anthropic Claude",
    defaultEndpoint = "https://api.anthropic.com/v1/messages",
    defaultModel = "claude-3-7-sonnet-latest",
    availableModels =
      listOf("claude-3-7-sonnet-latest", "claude-3-5-haiku-latest", "claude-3-5-sonnet-latest"),
  ),
  DEEPSEEK(
    displayName = "DeepSeek",
    defaultEndpoint = "https://api.deepseek.com/chat/completions",
    defaultModel = "deepseek-chat",
    availableModels = listOf("deepseek-chat", "deepseek-reasoner"),
  ),
  CUSTOM(
    displayName = "Custom Compatible API",
    defaultEndpoint = "https://api.openai.com/v1/chat/completions",
    defaultModel = "custom-model",
    availableModels = listOf("custom-model"),
  ),
}

@Immutable
data class ClipboardItem(
  val id: String,
  val label: String,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
)

@Immutable
data class NetworkStatus(
  val isConnected: Boolean = true,
  val transportLabel: String = "Wi-Fi",
)
