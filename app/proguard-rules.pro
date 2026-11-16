# AI UNIVERSE R8 Obfuscation & Anti-Tamper Rules

-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o'
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip debug/verbose logs in release builds to impede reverse engineering
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Room entities, DAOs, database & Moshi models
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Preserve Kotlin coroutines & serialization metadata needed at runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Preserve EncryptedSharedPreferences / Tink keysets
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Integrity guard must remain intact
-keep class com.example.security.AppIntegrityGuard { *; }
