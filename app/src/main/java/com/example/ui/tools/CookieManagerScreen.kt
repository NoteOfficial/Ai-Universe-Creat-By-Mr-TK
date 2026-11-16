package com.example.ui.tools

import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.network.UrlValidator
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

data class ParsedCookieItem(
  val name: String,
  val value: String,
  val domain: String? = null,
  val path: String = "/",
  val secure: Boolean = true,
  val httpOnly: Boolean = false,
) {
  fun toSetCookieHeader(): String =
    buildString {
      append("$name=$value; Path=$path")
      if (!domain.isNullOrBlank()) append("; Domain=$domain")
      if (secure) append("; Secure")
      if (httpOnly) append("; HttpOnly")
    }
}

object CookieParser {
  fun parseRawHeader(rawCookieHeader: String?): List<ParsedCookieItem> {
    if (rawCookieHeader.isNullOrBlank()) return emptyList()
    return rawCookieHeader
      .split(";")
      .mapNotNull { part ->
        val trimmed = part.trim()
        val eq = trimmed.indexOf('=')
        if (eq <= 0) return@mapNotNull null
        val name = trimmed.substring(0, eq).trim()
        val value = trimmed.substring(eq + 1).trim()
        if (name.isBlank()) null else ParsedCookieItem(name = name, value = value)
      }
  }

  fun parseImportText(input: String): List<ParsedCookieItem> {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return emptyList()

    // 1. JSON array or object (Cookie-Editor / EditThisCookie / Chrome DevTools JSON)
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
      runCatching {
        val result = mutableListOf<ParsedCookieItem>()
        if (trimmed.startsWith("[")) {
          val arr = JSONArray(trimmed)
          for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            parseJsonCookieObject(obj)?.let { result.add(it) }
          }
        } else {
          val obj = JSONObject(trimmed)
          val single = parseJsonCookieObject(obj)
          if (single != null) {
            result.add(single)
          } else {
            obj.keys().forEach { key ->
              val v = obj.opt(key)?.toString() ?: ""
              if (key.isNotBlank()) result.add(ParsedCookieItem(name = key.trim(), value = v))
            }
          }
        }
        if (result.isNotEmpty()) return result
      }
    }

    val collected = mutableListOf<ParsedCookieItem>()
    val attributeKeys = setOf("path", "domain", "expires", "max-age", "samesite", "priority")

    trimmed.lines().forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isBlank() || (line.startsWith("#") && !line.startsWith("#HttpOnly_"))) return@forEach

      // 2. Netscape cookies.txt tab-separated line (7 fields)
      val netscapeCols = line.split('\t')
      if (netscapeCols.size >= 7) {
        val rawDomain = netscapeCols[0].removePrefix("#HttpOnly_").trim()
        val path = netscapeCols[2].ifBlank { "/" }
        val secure = netscapeCols[3].equals("TRUE", ignoreCase = true)
        val name = netscapeCols[5].trim()
        val value = netscapeCols[6].trim()
        if (name.isNotBlank()) {
          collected.add(
            ParsedCookieItem(
              name = name,
              value = value,
              domain = rawDomain.ifBlank { null },
              path = path,
              secure = secure,
              httpOnly = line.startsWith("#HttpOnly_"),
            )
          )
        }
        return@forEach
      }

      // 3. Single Set-Cookie line OR raw `k=v; k2=v2` header
      val cleaned = line.removePrefix("Set-Cookie:").removePrefix("set-cookie:").removePrefix("Cookie:").removePrefix("cookie:").trim()
      val segments = cleaned.split(";").map { it.trim() }.filter { it.isNotBlank() }
      val eqSegments = segments.filter { it.contains("=") }

      // Determine whether this is one `Set-Cookie` header (1 key=val + Path=/; Domain=...) vs multiple `k=v; k2=v2` pairs
      val nonAttrPairs =
        eqSegments.filter { seg ->
          val k = seg.substringBefore("=").trim().lowercase()
          k !in attributeKeys
        }
      if (nonAttrPairs.size <= 1) {
        val first = nonAttrPairs.firstOrNull() ?: return@forEach
        val name = first.substringBefore("=").trim()
        val value = first.substringAfter("=").trim()
        if (name.isNotBlank()) {
          var domain: String? = null
          var path = "/"
          segments.drop(1).forEach { attr ->
            val ak = attr.substringBefore("=").trim().lowercase()
            val av = attr.substringAfter("=", "").trim()
            if (ak == "domain" && av.isNotBlank()) domain = av
            if (ak == "path" && av.isNotBlank()) path = av
          }
          val lowerAttrs = segments.map { it.lowercase() }
          collected.add(
            ParsedCookieItem(
              name = name,
              value = value,
              domain = domain,
              path = path,
              secure = "secure" in lowerAttrs || lowerAttrs.none { it == "http" },
              httpOnly = "httponly" in lowerAttrs,
            )
          )
        }
      } else {
        nonAttrPairs.forEach { pair ->
          val eq = pair.indexOf('=')
          if (eq > 0) {
            val name = pair.substring(0, eq).trim()
            val value = pair.substring(eq + 1).trim()
            if (name.isNotBlank()) {
              collected.add(ParsedCookieItem(name = name, value = value))
            }
          }
        }
      }
    }
    return collected
  }

  private fun parseJsonCookieObject(obj: JSONObject): ParsedCookieItem? {
    val name = obj.optString("name", "").trim()
    if (name.isBlank()) return null
    val value = obj.optString("value", "")
    val domain = obj.optString("domain", "").trim().ifBlank { null }
    val path = obj.optString("path", "/").trim().ifBlank { "/" }
    val secure = if (obj.has("secure")) obj.optBoolean("secure", true) else true
    val httpOnly = obj.optBoolean("httpOnly", false)
    return ParsedCookieItem(name, value, domain, path, secure, httpOnly)
  }

  fun toJsonExport(items: List<ParsedCookieItem>, url: String): String {
    val host = UrlValidator.extractHost(url)
    val arr = JSONArray()
    items.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("name", item.name)
          put("value", item.value)
          put("domain", item.domain ?: host)
          put("path", item.path)
          put("secure", item.secure)
          put("httpOnly", item.httpOnly)
        }
      )
    }
    return arr.toString(2)
  }
}

@Composable
fun CookieManagerScreen(
  initialUrl: String,
  onCopyText: (label: String, text: String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current

  val presetUrls =
    remember {
      listOf(
        "ChatGPT" to "https://chatgpt.com/",
        "Gemini" to "https://gemini.google.com/",
        "Claude" to "https://claude.ai/",
        "DeepSeek" to "https://chat.deepseek.com/",
        "Arena AI" to "https://lmarena.ai/",
        "v0.dev" to "https://v0.dev/",
        "Google Auth" to "https://accounts.google.com/",
      )
    }

  var targetUrl by remember(initialUrl) {
    mutableStateOf(
      if (initialUrl.isNotBlank()) UrlValidator.normalizeUrl(initialUrl)
      else "https://chatgpt.com/"
    )
  }
  var importDraft by remember { mutableStateOf("") }
  var refreshTick by remember { mutableIntStateOf(0) }

  val activeCookies =
    remember(targetUrl, refreshTick) {
      val normalized = UrlValidator.normalizeUrl(targetUrl)
      val raw = runCatching { CookieManager.getInstance().getCookie(normalized) }.getOrNull()
      CookieParser.parseRawHeader(raw)
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SpringIconButton(onClick = onBack, modifier = Modifier.testTag("cookie_back_button")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = neu.primaryText)
      }
      Text(
        text = strings.cookies.manageCookiesTitle,
        style = MaterialTheme.typography.titleLarge,
        color = neu.primaryText,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      SpringIconButton(onClick = { refreshTick++ }) {
        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = neu.accent)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Domain / URL Picker
      item {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              presetUrls.forEach { (label, url) ->
                val selected = UrlValidator.extractHost(targetUrl) == UrlValidator.extractHost(url)
                SpringFilterChip(
                  selected = selected,
                  onClick = {
                    targetUrl = url
                    refreshTick++
                  },
                  label = { Text(label) },
                )
              }
            }
            OutlinedTextField(
              value = targetUrl,
              onValueChange = { targetUrl = it },
              label = { Text(strings.cookies.cookieDomainHint) },
              singleLine = true,
              trailingIcon = {
                IconButton(onClick = {
                  targetUrl = UrlValidator.normalizeUrl(targetUrl)
                  refreshTick++
                }) {
                  Icon(Icons.Rounded.Search, contentDescription = null)
                }
              },
              modifier = Modifier.fillMaxWidth().testTag("cookie_url_input"),
            )
          }
        }
      }

      // Import / Add Cookies
      item {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.Cookie, contentDescription = null, tint = neu.accent)
              Text(
                text = strings.cookies.cookieImportLabel,
                style = MaterialTheme.typography.titleSmall,
                color = neu.primaryText,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedTextField(
              value = importDraft,
              onValueChange = { importDraft = it },
              placeholder = { Text(strings.cookies.cookieImportHint, style = MaterialTheme.typography.bodySmall) },
              minLines = 3,
              maxLines = 6,
              modifier = Modifier.fillMaxWidth().testTag("cookie_import_input"),
            )
            SpringButton(
              onClick = {
                val parsed = CookieParser.parseImportText(importDraft)
                if (parsed.isEmpty()) {
                  Toast.makeText(context, strings.cookies.cookieInvalidInput, Toast.LENGTH_SHORT).show()
                } else {
                  val normalized = UrlValidator.normalizeUrl(targetUrl)
                  runCatching {
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    parsed.forEach { cookie ->
                      cm.setCookie(normalized, cookie.toSetCookieHeader())
                    }
                    cm.flush()
                  }.onSuccess {
                    importDraft = ""
                    refreshTick++
                    Toast.makeText(
                      context,
                      String.format(strings.cookies.cookieAppliedFmt, parsed.size),
                      Toast.LENGTH_SHORT,
                    ).show()
                  }.onFailure {
                    Toast.makeText(context, strings.cookies.cookieInvalidInput, Toast.LENGTH_SHORT).show()
                  }
                }
              },
              modifier = Modifier.fillMaxWidth().testTag("cookie_apply_button"),
            ) {
              Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.cookies.cookieApplyBtn)
            }
          }
        }
      }

      // Active Cookies Header & Export / Clear Actions
      item {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = String.format(strings.cookies.cookieActiveCountFmt, activeCookies.size),
              style = MaterialTheme.typography.titleSmall,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SpringOutlinedButton(
                onClick = {
                  val rawHeader = activeCookies.joinToString("; ") { "${it.name}=${it.value}" }
                  if (rawHeader.isNotBlank()) {
                    onCopyText("Cookies (${UrlValidator.extractHost(targetUrl)})", rawHeader)
                    Toast.makeText(context, strings.copyAgain, Toast.LENGTH_SHORT).show()
                  }
                },
                enabled = activeCookies.isNotEmpty(),
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.cookies.cookieCopyRawBtn, maxLines = 1)
              }
              SpringOutlinedButton(
                onClick = {
                  val json = CookieParser.toJsonExport(activeCookies, targetUrl)
                  onCopyText("Cookies JSON (${UrlValidator.extractHost(targetUrl)})", json)
                  Toast.makeText(context, strings.copyAgain, Toast.LENGTH_SHORT).show()
                },
                enabled = activeCookies.isNotEmpty(),
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Rounded.DataObject, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.cookies.cookieCopyJsonBtn, maxLines = 1)
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SpringOutlinedButton(
                onClick = {
                  val normalized = UrlValidator.normalizeUrl(targetUrl)
                  runCatching {
                    val cm = CookieManager.getInstance()
                    activeCookies.forEach { cookie ->
                      cm.setCookie(normalized, "${cookie.name}=; Max-Age=0; Path=/")
                    }
                    cm.flush()
                  }
                  refreshTick++
                  Toast.makeText(context, strings.cookies.cookieDomainClearedToast, Toast.LENGTH_SHORT).show()
                },
                enabled = activeCookies.isNotEmpty(),
                modifier = Modifier.weight(1f),
              ) {
                Text(strings.cookies.cookieClearDomainBtn, color = WarningAmber, maxLines = 1)
              }
              SpringOutlinedButton(
                onClick = {
                  runCatching {
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                  }
                  refreshTick++
                  Toast.makeText(context, strings.cookiesClearedToast, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
              ) {
                Text(strings.clearAll, color = ErrorRed, maxLines = 1)
              }
            }
          }
        }
      }

      if (activeCookies.isEmpty()) {
        item {
          NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
            Text(
              text = strings.cookies.noCookiesForDomain,
              style = MaterialTheme.typography.bodyMedium,
              color = neu.secondaryText,
              modifier = Modifier.padding(8.dp),
            )
          }
        }
      } else {
        items(activeCookies, key = { it.name }) { item ->
          NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false, cornerRadius = 16.dp) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.titleSmall,
                  color = neu.accent,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = item.value,
                  style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                  color = neu.secondaryText,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              IconButton(
                onClick = {
                  val normalized = UrlValidator.normalizeUrl(targetUrl)
                  runCatching {
                    val cm = CookieManager.getInstance()
                    cm.setCookie(normalized, "${item.name}=; Max-Age=0; Path=/")
                    cm.flush()
                  }
                  refreshTick++
                }
              ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = ErrorRed)
              }
            }
          }
        }
      }
    }
  }
}
