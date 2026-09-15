# ─────────────────────────────────────────────────────────────────
#  CineTheta ProGuard / R8 Rules
# ─────────────────────────────────────────────────────────────────

# !! CRITICAL: Shrink dead code but DO NOT rename/obfuscate classes.
# This is what fixes the Gson 'java.lang.Class cannot be cast to
# ParameterizedType' crash while still keeping the APK small.
-dontobfuscate

# !! ALSO CRITICAL: Disable R8 class merging/inlining optimizations.
# R8's optimizer (even without obfuscation) merges anonymous TypeToken
# subclasses and strips their generic supertype, breaking Gson at runtime.
-dontoptimize

# ── 1. Keep Attributes (CRITICAL: fixes ParameterizedType crash) ──
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ── 2. Keep ALL CineTheta App Classes (most reliable fix) ─────────
#    R8 was silently obfuscating model field names and generic types
#    in nested packages that weren't covered by partial rules.
-keep class com.cinetheta.** { *; }

# ── 3. Gson ───────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# ── 4. Retrofit 2 & Suspend Continuation (CRITICAL FIX) ────────────
-keep class kotlin.coroutines.Continuation { *; }
-keep interface retrofit2.Call { *; }
-keep class retrofit2.Response { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class * implements retrofit2.CallAdapter$Factory { *; }
-keep class * implements retrofit2.Converter$Factory { *; }

# ── 5. OkHttp ─────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── 6. Kotlinx Serialization ──────────────────────────────────────
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }

# ── 7. Coroutines ─────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── 8. Hilt / DI ──────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── 9. Coil (Image Loading) ───────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── 10. ExoPlayer / Media3 ────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── 11. Suppress common warnings ──────────────────────────────────
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-dontwarn org.conscrypt.**