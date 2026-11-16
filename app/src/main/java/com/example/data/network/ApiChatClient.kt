package com.example.data.network

import com.example.data.model.ApiProviderType
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ApiAttachmentPayload(
  val name: String,
  val mimeType: String,
  val base64Data: String? = null,
  val textContent: String? = null,
)

data class ApiChatMessagePayload(
  val role: String, // "user" or "assistant"
  val content: String,
  val attachment: ApiAttachmentPayload? = null,
)

sealed class ApiResponseResult {
  data class Success(val text: String, val modelUsed: String) : ApiResponseResult()

  data class Error(val title: String, val detail: String) : ApiResponseResult()
}

class ApiChatClient {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(25, TimeUnit.SECONDS)
      .readTimeout(90, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  @Volatile private var activeCall: Call? = null

  fun cancelActiveCall() {
    activeCall?.cancel()
    activeCall = null
  }

  suspend fun testConnection(
    providerType: ApiProviderType,
    endpoint: String,
    model: String,
    apiKey: String,
  ): ApiResponseResult {
    if (apiKey.isBlank()) {
      return ApiResponseResult.Error("Missing API Key", "Please enter a valid API key before testing.")
    }
    return sendMessage(
      providerType = providerType,
      endpoint = endpoint,
      model = model,
      apiKey = apiKey,
      systemPrompt = "You are a concise connection check assistant.",
      temperature = 0.2f,
      history = listOf(ApiChatMessagePayload(role = "user", content = "Reply with only: OK")),
    )
  }

  suspend fun sendMessage(
    providerType: ApiProviderType,
    endpoint: String,
    model: String,
    apiKey: String,
    systemPrompt: String,
    temperature: Float,
    history: List<ApiChatMessagePayload>,
  ): ApiResponseResult =
    withContext(Dispatchers.IO) {
      if (apiKey.isBlank()) {
        return@withContext ApiResponseResult.Error(
          "API Key Required",
          "No API key is saved for this provider. Add your key in API Settings.",
        )
      }
      val cleanEndpoint = endpoint.trim().ifBlank { providerType.defaultEndpoint }
      if (!UrlValidator.isValidHttpsUrl(cleanEndpoint)) {
        return@withContext ApiResponseResult.Error(
          "Invalid Endpoint URL",
          "Endpoint must be a valid https:// URL.",
        )
      }

      try {
        val request =
          when (providerType) {
            ApiProviderType.GEMINI ->
              buildGeminiRequest(cleanEndpoint, model, apiKey, systemPrompt, temperature, history)
            ApiProviderType.CLAUDE ->
              buildClaudeRequest(cleanEndpoint, model, apiKey, systemPrompt, temperature, history)
            ApiProviderType.OPENAI,
            ApiProviderType.DEEPSEEK,
            ApiProviderType.CUSTOM ->
              buildOpenAiStyleRequest(cleanEndpoint, model, apiKey, systemPrompt, temperature, history)
          }

        val call = client.newCall(request)
        activeCall = call
        call.execute().use { response ->
          activeCall = null
          val bodyStr = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            val parsedError = extractErrorMessage(bodyStr)
            return@withContext ApiResponseResult.Error(
              title = "HTTP ${response.code} (${providerType.displayName})",
              detail = parsedError.ifBlank { response.message.ifBlank { "Request rejected by API server." } },
            )
          }
          val replyText =
            when (providerType) {
              ApiProviderType.GEMINI -> parseGeminiResponse(bodyStr)
              ApiProviderType.CLAUDE -> parseClaudeResponse(bodyStr)
              else -> parseOpenAiStyleResponse(bodyStr)
            }
          if (replyText.isBlank()) {
            ApiResponseResult.Error(
              "Empty Response",
              "The API returned HTTP ${response.code} with no text content.",
            )
          } else {
            ApiResponseResult.Success(text = replyText, modelUsed = model)
          }
        }
      } catch (e: IOException) {
        activeCall = null
        if (e.message?.contains("Canceled", ignoreCase = true) == true) {
          ApiResponseResult.Error("Generation Stopped", "Request was stopped by user.")
        } else {
          ApiResponseResult.Error(
            "Network Error",
            e.localizedMessage ?: "Unable to reach the provider endpoint.",
          )
        }
      } catch (e: Exception) {
        activeCall = null
        ApiResponseResult.Error(
          "API Client Error",
          e.localizedMessage ?: "Unexpected error parsing API response.",
        )
      }
    }

  private fun buildOpenAiStyleRequest(
    endpoint: String,
    model: String,
    apiKey: String,
    systemPrompt: String,
    temperature: Float,
    history: List<ApiChatMessagePayload>,
  ): Request {
    val messagesArray = JSONArray()
    if (systemPrompt.isNotBlank()) {
      messagesArray.put(
        JSONObject().apply {
          put("role", "system")
          put("content", systemPrompt)
        }
      )
    }

    history.forEach { msg ->
      val attachment = msg.attachment
      if (attachment != null && attachment.base64Data != null && attachment.mimeType.startsWith("image/")) {
        val parts =
          JSONArray().apply {
            put(
              JSONObject().apply {
                put("type", "text")
                put("text", msg.content.ifBlank { "Analyze this image." })
              }
            )
            put(
              JSONObject().apply {
                put("type", "image_url")
                put(
                  "image_url",
                  JSONObject().put("url", "data:${attachment.mimeType};base64,${attachment.base64Data}"),
                )
              }
            )
          }
        messagesArray.put(
          JSONObject().apply {
            put("role", msg.role)
            put("content", parts)
          }
        )
      } else {
        val fullText =
          if (attachment?.textContent != null) {
            "${msg.content}\n\n[Attached File: ${attachment.name}]\n```\n${attachment.textContent.take(16000)}\n```"
          } else {
            msg.content
          }
        messagesArray.put(
          JSONObject().apply {
            put("role", msg.role)
            put("content", fullText)
          }
        )
      }
    }

    val payload =
      JSONObject().apply {
        put("model", model.trim())
        put("messages", messagesArray)
        put("temperature", temperature.toDouble())
      }

    return Request.Builder()
      .url(endpoint)
      .header("Authorization", "Bearer ${apiKey.trim()}")
      .header("Content-Type", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
  }

  private fun buildGeminiRequest(
    baseEndpoint: String,
    model: String,
    apiKey: String,
    systemPrompt: String,
    temperature: Float,
    history: List<ApiChatMessagePayload>,
  ): Request {
    val cleanBase = baseEndpoint.trimEnd('/')
    val fullUrl =
      if (cleanBase.contains(":generateContent")) {
        cleanBase
      } else {
        "$cleanBase/models/${model.trim()}:generateContent"
      }

    val contents = JSONArray()
    history.forEach { msg ->
      val parts = JSONArray()
      val attachment = msg.attachment
      val textWithFile =
        if (attachment?.textContent != null) {
          "${msg.content}\n\n[Attached File: ${attachment.name}]\n${attachment.textContent.take(16000)}"
        } else {
          msg.content
        }
      if (textWithFile.isNotBlank()) {
        parts.put(JSONObject().put("text", textWithFile))
      }
      if (attachment?.base64Data != null && attachment.mimeType.startsWith("image/")) {
        parts.put(
          JSONObject().put(
            "inline_data",
            JSONObject().apply {
              put("mime_type", attachment.mimeType)
              put("data", attachment.base64Data)
            },
          )
        )
      }
      contents.put(
        JSONObject().apply {
          put("role", if (msg.role == "assistant") "model" else "user")
          put("parts", parts)
        }
      )
    }

    val payload =
      JSONObject().apply {
        put("contents", contents)
        if (systemPrompt.isNotBlank()) {
          put(
            "system_instruction",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))),
          )
        }
        put("generationConfig", JSONObject().put("temperature", temperature.toDouble()))
      }

    return Request.Builder()
      .url(fullUrl)
      .header("x-goog-api-key", apiKey.trim())
      .header("Content-Type", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
  }

  private fun buildClaudeRequest(
    endpoint: String,
    model: String,
    apiKey: String,
    systemPrompt: String,
    temperature: Float,
    history: List<ApiChatMessagePayload>,
  ): Request {
    val messagesArray = JSONArray()
    history.forEach { msg ->
      val attachment = msg.attachment
      if (attachment != null && attachment.base64Data != null && attachment.mimeType.startsWith("image/")) {
        val blocks =
          JSONArray().apply {
            put(
              JSONObject().apply {
                put("type", "image")
                put(
                  "source",
                  JSONObject().apply {
                    put("type", "base64")
                    put("media_type", attachment.mimeType)
                    put("data", attachment.base64Data)
                  },
                )
              }
            )
            put(
              JSONObject().apply {
                put("type", "text")
                put("text", msg.content.ifBlank { "Analyze this image." })
              }
            )
          }
        messagesArray.put(
          JSONObject().apply {
            put("role", if (msg.role == "assistant") "assistant" else "user")
            put("content", blocks)
          }
        )
      } else {
        val textWithFile =
          if (attachment?.textContent != null) {
            "${msg.content}\n\n[Attached File: ${attachment.name}]\n${attachment.textContent.take(16000)}"
          } else {
            msg.content
          }
        messagesArray.put(
          JSONObject().apply {
            put("role", if (msg.role == "assistant") "assistant" else "user")
            put("content", textWithFile)
          }
        )
      }
    }

    val payload =
      JSONObject().apply {
        put("model", model.trim())
        put("max_tokens", 2048)
        put("temperature", temperature.toDouble())
        if (systemPrompt.isNotBlank()) {
          put("system", systemPrompt)
        }
        put("messages", messagesArray)
      }

    return Request.Builder()
      .url(endpoint)
      .header("x-api-key", apiKey.trim())
      .header("anthropic-version", "2023-06-01")
      .header("Content-Type", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
  }

  private fun parseOpenAiStyleResponse(body: String): String {
    val root = JSONObject(body)
    val choices = root.optJSONArray("choices") ?: return ""
    if (choices.length() == 0) return ""
    val msg = choices.getJSONObject(0).optJSONObject("message") ?: return ""
    return msg.optString("content", "")
  }

  private fun parseGeminiResponse(body: String): String {
    val root = JSONObject(body)
    val candidates = root.optJSONArray("candidates") ?: return ""
    if (candidates.length() == 0) return ""
    val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
    val parts = content.optJSONArray("parts") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until parts.length()) {
      val partText = parts.getJSONObject(i).optString("text", "")
      if (partText.isNotEmpty()) sb.append(partText)
    }
    return sb.toString()
  }

  private fun parseClaudeResponse(body: String): String {
    val root = JSONObject(body)
    val content = root.optJSONArray("content") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until content.length()) {
      val block = content.getJSONObject(i)
      if (block.optString("type") == "text") {
        sb.append(block.optString("text", ""))
      }
    }
    return sb.toString()
  }

  private fun extractErrorMessage(body: String): String {
    return try {
      val root = JSONObject(body)
      val errorObj = root.optJSONObject("error")
      errorObj?.optString("message")?.takeIf { it.isNotBlank() }
        ?: root.optString("message").takeIf { it.isNotBlank() }
        ?: body.take(280)
    } catch (_: Exception) {
      body.take(280)
    }
  }
}
