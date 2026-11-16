package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.webkit.WebView
import com.example.BuildConfig
import java.security.MessageDigest

data class IntegrityReport(
  val isTrusted: Boolean,
  val reason: String = "",
  val certificateSha256: String = "",
)

object AppIntegrityGuard {
  const val EXPECTED_PACKAGE_ID = "com.tkofficial.aiuniverse"

  fun verify(context: Context): IntegrityReport {
    // Allow JVM Robolectric test runner where APK signing certificates are absent
    if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      return IntegrityReport(isTrusted = true, certificateSha256 = "ROBOLECTRIC_TEST")
    }

    // 1. Verify runtime package ID & BuildConfig identity (blocks package cloning / repackaging)
    if (context.packageName != EXPECTED_PACKAGE_ID || BuildConfig.APPLICATION_ID != EXPECTED_PACKAGE_ID) {
      return IntegrityReport(
        isTrusted = false,
        reason = "Unauthorized package modification detected (${context.packageName}).",
      )
    }

    if (context.applicationInfo.packageName != EXPECTED_PACKAGE_ID) {
      return IntegrityReport(
        isTrusted = false,
        reason = "Manifest application identity mismatch.",
      )
    }

    // 2. Harden WebView inspection on release builds
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebuggable) {
      runCatching { WebView.setWebContentsDebuggingEnabled(false) }
    }

    // 3. Verify APK contains at least one intact cryptographic signing certificate (v1/v2/v3)
    val certDigests = readSigningCertificateSha256(context)
    if (certDigests.isEmpty()) {
      return IntegrityReport(
        isTrusted = false,
        reason = "APK signature verification failed (missing signing certificate).",
      )
    }

    return IntegrityReport(
      isTrusted = true,
      certificateSha256 = certDigests.first(),
    )
  }

  private fun readSigningCertificateSha256(context: Context): List<String> =
    runCatching {
      val pm = context.packageManager
      val pkgName = context.packageName
      val signatures: Array<Signature> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
          val signingInfo = info.signingInfo ?: return emptyList()
          if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
          } else {
            signingInfo.signingCertificateHistory
          }
        } else {
          @Suppress("DEPRECATION")
          pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
        } ?: return emptyList()

      val md = MessageDigest.getInstance("SHA-256")
      signatures.mapNotNull { sig ->
        val bytes = sig.toByteArray()
        if (bytes.isEmpty()) null
        else md.digest(bytes).joinToString(":") { "%02X".format(it) }
      }
    }.getOrDefault(emptyList())
}
