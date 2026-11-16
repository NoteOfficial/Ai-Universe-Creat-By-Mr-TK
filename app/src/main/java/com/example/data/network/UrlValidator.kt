package com.example.data.network

import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class AvailabilityResult(
  val isReachable: Boolean,
  val statusCode: Int?,
  val latencyMs: Long,
  val message: String,
)

enum class OriginCheckStatus {
  APPROVED_OFFICIAL,
  APPROVED_AUTH_FLOW,
  UNVERIFIED_EXTERNAL,
  INVALID_URL,
}

object UrlValidator {
  private val trustedAuthHosts =
    setOf(
      "accounts.google.com",
      "github.com",
      "appleid.apple.com",
      "login.microsoftonline.com",
      "login.live.com",
      "auth0.com",
      "discord.com",
      "challenges.cloudflare.com",
    )

  fun isValidHttpsUrl(rawUrl: String): Boolean {
    return try {
      val uri = URI(rawUrl.trim())
      val scheme = uri.scheme?.lowercase()
      val host = uri.host?.lowercase()
      (scheme == "https" || scheme == "http") && !host.isNullOrBlank() && host.contains(".")
    } catch (_: Exception) {
      false
    }
  }

  fun normalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      trimmed
    } else {
      "https://$trimmed"
    }
  }

  fun extractHost(url: String): String {
    return try {
      URI(url.trim()).host?.lowercase()?.removePrefix("www.") ?: ""
    } catch (_: Exception) {
      ""
    }
  }

  fun checkOrigin(url: String, approvedOrigins: List<String>): OriginCheckStatus {
    if (!isValidHttpsUrl(url)) return OriginCheckStatus.INVALID_URL
    val host = extractHost(url)
    if (host.isBlank()) return OriginCheckStatus.INVALID_URL

    val normalizedApproved = approvedOrigins.map { it.lowercase().removePrefix("www.") }
    if (
      normalizedApproved.any { approved ->
        host == approved || host.endsWith(".$approved")
      }
    ) {
      return OriginCheckStatus.APPROVED_OFFICIAL
    }

    if (
      trustedAuthHosts.any { authHost ->
        host == authHost || host.endsWith(".$authHost")
      }
    ) {
      return OriginCheckStatus.APPROVED_AUTH_FLOW
    }

    return OriginCheckStatus.UNVERIFIED_EXTERNAL
  }
}

class UrlAvailabilityChecker {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .followRedirects(true)
      .build()

  suspend fun check(url: String): AvailabilityResult =
    withContext(Dispatchers.IO) {
      if (!UrlValidator.isValidHttpsUrl(url)) {
        return@withContext AvailabilityResult(
          isReachable = false,
          statusCode = null,
          latencyMs = 0L,
          message = "Invalid URL format",
        )
      }
      val start = System.currentTimeMillis()
      try {
        val request =
          Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
          val elapsed = System.currentTimeMillis() - start
          val code = response.code
          val reachable = code in 200..499 // 403/401 still proves host is live & responding
          AvailabilityResult(
            isReachable = reachable,
            statusCode = code,
            latencyMs = elapsed,
            message =
              when {
                code in 200..299 -> "Online (HTTP $code • ${elapsed}ms)"
                code == 403 || code == 401 -> "Online • Bot/Auth Protection (HTTP $code • ${elapsed}ms)"
                else -> "Responded with HTTP $code (${elapsed}ms)"
              },
          )
        }
      } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - start
        AvailabilityResult(
          isReachable = false,
          statusCode = null,
          latencyMs = elapsed,
          message = e.localizedMessage ?: "Connection failed",
        )
      }
    }
}
