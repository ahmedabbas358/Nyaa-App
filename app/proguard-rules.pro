# ==============================================================================
# AniFlow Production R8 & ProGuard Rules
# Enforces STEP 13 Section 70 (R8 / Minification) and Section 71 (Serialization)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Android & Kotlin Core
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# ------------------------------------------------------------------------------
# 2. Room Database Rules
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration

# ------------------------------------------------------------------------------
# 3. Kotlinx Serialization
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,allowobfuscation,allowshrinking class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static *** Companion;
}

# ------------------------------------------------------------------------------
# 4. Domain Value Classes and Aggregate Models
# ------------------------------------------------------------------------------
-keep class com.aniflow.domain.identity.** { *; }
-keep class com.aniflow.domain.valueobject.** { *; }
-keep class com.aniflow.domain.model.** { *; }
-keep class com.aniflow.domain.controlplane.models.** { *; }

# ------------------------------------------------------------------------------
# 5. OkHttp Rules
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ------------------------------------------------------------------------------
# 6. Coroutines
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
