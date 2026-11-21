# GomizPlayer ProGuard rules
-keep class com.gomizplayer.iptv.core.model.** { *; }
-keep class com.gomizplayer.iptv.core.m3u.** { *; }
-keep class com.gomizplayer.iptv.core.epg.** { *; }
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.gomizplayer.iptv.**$$serializer { *; }
-keepclassmembers class com.gomizplayer.iptv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Media3
-keep class androidx.media3.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
