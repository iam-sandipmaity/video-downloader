# ── Hilt / Dagger ─────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories { *; }

# Hilt generated classes
-keep class dagger.hilt.**_Impl { *; }
-keep class dagger.hilt.**_HiltModules** { *; }
-keep class **_HiltModules** { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-keep class **_Provide*Factory* { *; }
-keep class **_Factory* { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @javax.inject.Named class *
-keep public class * extends androidx.lifecycle.ViewModel

# ── Jetpack Compose ───────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keepnames class kotlinx.coroutines.internal.* { *; }
-dontwarn androidx.compose.**

# ── kotlinx.serialization ─────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.localdownloader.**$$serializer { *; }
-keepclassmembers class com.localdownloader.** {
    *** Companion;
}
-keepclasseswithmembers class com.localdownloader.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── yt-dlp-android / youtubeDL-android ───────────────────────────
-keep class com.yausername.** { *; }
-keep class org.libsdl.** { *; }
-keep class org.apache.** { *; }
-dontwarn com.yausername.**
-dontwarn org.libsdl.**

# ── FFmpeg ────────────────────────────────────────────────────────
-keep class com.localdownloader.jni.** { *; }
-keep class io.github.theyagas.** { *; }
-dontwarn io.github.theyagas.**

# ── WorkManager ───────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.InputMerger

# ── OkHttp ────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── General ──────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class com.localdownloader.domain.models.** { *; }
