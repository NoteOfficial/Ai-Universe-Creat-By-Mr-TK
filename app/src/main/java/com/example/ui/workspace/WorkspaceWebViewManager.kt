package com.example.ui.workspace

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.network.OriginCheckStatus
import com.example.data.network.UrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WorkspaceRuntimeState(
  val isLoading: Boolean = true,
  val progress: Int = 0,
  val currentUrl: String = "",
  val pageTitle: String = "",
  val canGoBack: Boolean = false,
  val errorTitle: String? = null,
  val errorDetail: String? = null,
  val originStatus: OriginCheckStatus = OriginCheckStatus.APPROVED_OFFICIAL,
  val blockedHostWarning: String? = null,
  val lastRefreshedAt: Long = 0L,
)

class WorkspaceWebViewManager(private val appContext: Context) {
  private val webViews = mutableMapOf<String, WebView>()
  private val contextWrappers = mutableMapOf<String, MutableContextWrapper>()
  private val _states = MutableStateFlow<Map<String, WorkspaceRuntimeState>>(emptyMap())
  val states: StateFlow<Map<String, WorkspaceRuntimeState>> = _states.asStateFlow()

  var onFileChooserRequested: ((ValueCallback<Array<Uri>>?) -> Unit)? = null
  var onDownloadRequested: ((url: String, mimeType: String, filename: String) -> Unit)? = null
  var onUrlUpdated: ((workspaceId: String, newUrl: String) -> Unit)? = null

  @SuppressLint("SetJavaScriptEnabled")
  fun getOrCreateWebView(
    workspaceId: String,
    initialUrl: String,
    approvedOrigins: List<String>,
    dataSaver: Boolean,
    hostContext: Context = appContext,
  ): WebView? {
    val existing = webViews[workspaceId]
    if (existing != null) {
      contextWrappers[workspaceId]?.baseContext = hostContext
      runCatching { existing.settings.loadsImagesAutomatically = !dataSaver }
      return existing
    }

    val wrapper = MutableContextWrapper(hostContext)
    val webView =
      runCatching { WebView(wrapper) }
        .onFailure { err ->
          updateState(workspaceId) {
            it.copy(
              isLoading = false,
              currentUrl = initialUrl,
              errorTitle = "WebView Initialization Failed",
              errorDetail =
                err.localizedMessage
                  ?: "Android System WebView could not start. Tap Custom Tab to open in Chrome.",
            )
          }
        }
        .getOrNull() ?: return null

    contextWrappers[workspaceId] = wrapper
    configureWebView(webView, workspaceId, approvedOrigins, dataSaver)
    webViews[workspaceId] = webView

    _states.update { map ->
      map +
        (workspaceId to
          WorkspaceRuntimeState(
            isLoading = true,
            currentUrl = initialUrl,
            originStatus = UrlValidator.checkOrigin(initialUrl, approvedOrigins),
          ))
    }

    if (UrlValidator.isValidHttpsUrl(initialUrl)) {
      webView.loadUrl(initialUrl)
    } else {
      updateState(workspaceId) {
        it.copy(
          isLoading = false,
          errorTitle = "Invalid Website URL",
          errorDetail = "The configured URL ($initialUrl) is not a valid HTTPS website.",
          originStatus = OriginCheckStatus.INVALID_URL,
        )
      }
    }
    return webView
  }

  fun detachHostContext(workspaceId: String) {
    contextWrappers[workspaceId]?.baseContext = appContext
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun configureWebView(
    webView: WebView,
    workspaceId: String,
    approvedOrigins: List<String>,
    dataSaver: Boolean,
  ) {
    runCatching {
      CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
      }
    }

    webView.settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      useWideViewPort = true
      loadWithOverviewMode = true
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      allowFileAccess = false
      allowContentAccess = true
      mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
      loadsImagesAutomatically = !dataSaver
      mediaPlaybackRequiresUserGesture = false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isAlgorithmicDarkeningAllowed = true
      }
      userAgentString = userAgentString.replace("; wv", "")
    }

    webView.webChromeClient =
      object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
          updateState(workspaceId) {
            it.copy(
              progress = newProgress,
              isLoading = newProgress in 1..99,
            )
          }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
          if (!title.isNullOrBlank()) {
            updateState(workspaceId) { it.copy(pageTitle = title) }
          }
        }

        override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?,
        ): Boolean {
          val handler = onFileChooserRequested
          if (handler != null) {
            handler.invoke(filePathCallback)
            return true
          }
          filePathCallback?.onReceiveValue(null)
          return false
        }
      }

    webView.webViewClient =
      object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
          view: WebView?,
          request: WebResourceRequest?,
        ): Boolean {
          val url = request?.url?.toString() ?: return false
          if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return true // Block non-http custom schemes inside WebView
          }
          val status = UrlValidator.checkOrigin(url, approvedOrigins)
          updateState(workspaceId) {
            it.copy(
              originStatus = status,
              blockedHostWarning =
                if (status == OriginCheckStatus.UNVERIFIED_EXTERNAL) {
                  "Navigated to external host: ${UrlValidator.extractHost(url)}"
                } else {
                  null
                },
            )
          }
          return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
          val target = url ?: return
          val status = UrlValidator.checkOrigin(target, approvedOrigins)
          updateState(workspaceId) {
            it.copy(
              isLoading = true,
              currentUrl = target,
              errorTitle = null,
              errorDetail = null,
              canGoBack = view?.canGoBack() ?: false,
              originStatus = status,
            )
          }
          onUrlUpdated?.invoke(workspaceId, target)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          val finalUrl = url ?: view?.url.orEmpty()
          updateState(workspaceId) {
            it.copy(
              isLoading = false,
              progress = 100,
              currentUrl = finalUrl.ifBlank { it.currentUrl },
              canGoBack = view?.canGoBack() ?: false,
            )
          }
          if (finalUrl.isNotBlank()) {
            onUrlUpdated?.invoke(workspaceId, finalUrl)
          }
        }

        override fun onReceivedError(
          view: WebView?,
          request: WebResourceRequest?,
          error: WebResourceError?,
        ) {
          if (request?.isForMainFrame == true) {
            updateState(workspaceId) {
              it.copy(
                isLoading = false,
                errorTitle = "Website Unavailable",
                errorDetail =
                  error?.description?.toString()
                    ?: "Failed to load the official website. Check your connection or open in Chrome Custom Tab.",
              )
            }
          }
        }

        override fun onReceivedHttpError(
          view: WebView?,
          request: WebResourceRequest?,
          errorResponse: WebResourceResponse?,
        ) {
          val code = errorResponse?.statusCode ?: 0
          if (request?.isForMainFrame == true && code >= 500) {
            updateState(workspaceId) {
              it.copy(
                isLoading = false,
                errorTitle = "Server Error (HTTP $code)",
                errorDetail = "The AI provider server returned HTTP $code. Tap Retry or open in Chrome Custom Tab.",
              )
            }
          }
        }

        override fun onReceivedSslError(
          view: WebView?,
          handler: SslErrorHandler?,
          error: android.net.http.SslError?,
        ) {
          handler?.cancel()
          updateState(workspaceId) {
            it.copy(
              isLoading = false,
              errorTitle = "SSL Security Error",
              errorDetail = "Blocked an untrusted SSL certificate while connecting to ${error?.url ?: "site"}.",
            )
          }
        }
      }

    webView.setDownloadListener(
      DownloadListener { url, _, contentDisposition, mimetype, _ ->
        val guessedName =
          android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
        onDownloadRequested?.invoke(url, mimetype ?: "application/octet-stream", guessedName)
      }
    )
  }

  fun refreshOnly(workspaceId: String) {
    val current = _states.value[workspaceId]
    if (current?.isLoading == true) return // Prevent duplicate refresh operations
    val wv = webViews[workspaceId] ?: return
    updateState(workspaceId) {
      it.copy(
        isLoading = true,
        errorTitle = null,
        errorDetail = null,
        lastRefreshedAt = System.currentTimeMillis(),
      )
    }
    wv.reload()
  }

  fun goBack(workspaceId: String): Boolean {
    val wv = webViews[workspaceId] ?: return false
    return if (wv.canGoBack()) {
      wv.goBack()
      true
    } else {
      false
    }
  }

  fun loadUrl(workspaceId: String, url: String) {
    if (!UrlValidator.isValidHttpsUrl(url)) return
    webViews[workspaceId]?.loadUrl(url)
  }

  fun getCurrentUrl(workspaceId: String): String? =
    webViews[workspaceId]?.url ?: _states.value[workspaceId]?.currentUrl

  fun pauseWorkspace(workspaceId: String) {
    webViews[workspaceId]?.onPause()
  }

  fun resumeWorkspace(workspaceId: String) {
    webViews[workspaceId]?.onResume()
  }

  fun pauseInactiveWorkspaces(activeIds: Set<String>) {
    webViews.forEach { (id, wv) ->
      if (id in activeIds) wv.onResume() else wv.onPause()
    }
  }

  fun destroyWorkspace(workspaceId: String) {
    contextWrappers.remove(workspaceId)?.baseContext = appContext
    webViews.remove(workspaceId)?.let { wv ->
      runCatching {
        wv.stopLoading()
        wv.onPause()
        (wv.parent as? android.view.ViewGroup)?.removeView(wv)
        wv.removeAllViews()
        wv.destroy()
      }
    }
    _states.update { it - workspaceId }
  }

  fun clearAllCaches() {
    webViews.values.forEach { it.clearCache(true) }
  }

  fun updateDataSaver(dataSaver: Boolean) {
    webViews.values.forEach { it.settings.loadsImagesAutomatically = !dataSaver }
  }

  private fun updateState(
    workspaceId: String,
    transform: (WorkspaceRuntimeState) -> WorkspaceRuntimeState,
  ) {
    _states.update { map ->
      val current = map[workspaceId] ?: WorkspaceRuntimeState()
      map + (workspaceId to transform(current))
    }
  }
}
