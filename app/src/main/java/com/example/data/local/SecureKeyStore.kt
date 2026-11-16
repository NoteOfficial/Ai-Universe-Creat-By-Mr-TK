package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {
  private val prefs: SharedPreferences =
    try {
      val masterKey =
        MasterKey.Builder(context.applicationContext)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build()
      EncryptedSharedPreferences.create(
        context.applicationContext,
        "ai_universe_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )
    } catch (_: Exception) {
      context.applicationContext.getSharedPreferences(
        "ai_universe_secure_fallback",
        Context.MODE_PRIVATE,
      )
    }

  fun saveApiKey(providerId: String, apiKey: String) {
    prefs.edit().putString("api_key_$providerId", apiKey).apply()
  }

  fun getApiKey(providerId: String): String {
    return prefs.getString("api_key_$providerId", "") ?: ""
  }

  fun deleteApiKey(providerId: String) {
    prefs.edit().remove("api_key_$providerId").apply()
  }

  fun savePin(pin: String) {
    prefs.edit().putString("app_lock_pin", pin).apply()
  }

  fun getPin(): String {
    return prefs.getString("app_lock_pin", "") ?: ""
  }

  fun clearPin() {
    prefs.edit().remove("app_lock_pin").apply()
  }

  fun hasPin(): Boolean = getPin().length == 4
}
