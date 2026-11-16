package com.example.ui.workspace

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabHelper {
  fun openOfficialWebsite(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
      val customTabsIntent =
        CustomTabsIntent.Builder()
          .setShowTitle(true)
          .setUrlBarHidingEnabled(false)
          .build()
      customTabsIntent.launchUrl(context, uri)
    } catch (_: Exception) {
      runCatching {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      }
    }
  }
}
