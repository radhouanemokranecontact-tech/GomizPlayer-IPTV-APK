# Gomiz Player IPTV — Android

[English](#english) | [العربية](#العربية)

📘 **الدليل التقني الشامل:** [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) — يشرح التطبيق كما يُستخدم فعلياً، مع كل الميزات والتفاصيل التقنية والحدود المعروفة.
📘 **Full technical guide:** [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) — what the app really does, internals and known limits.

## English

Gomiz Player IPTV is a native Android TV / Google TV and Android phone IPTV player built with **Kotlin**, **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)** and **FFmpegKit**.

### Features
- M3U / M3U8 playlists and Xtream Codes sources
- Live TV with channel categories, favorites and a full "All channels" drawer
- Electronic Program Guide (EPG / XMLTV) with Now/Next
- Movies, Series, Downloads and Recordings
- HLS recording and local VOD downloads
- Centralized TV focus ring and full D-pad remote navigation
- Dynamic weather, clock and prayer-time widgets
- English and Arabic interface (RTL aware)

### Version
Current version: **1.0.4**

### Build
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

> `local.properties` is intentionally git-ignored — it contains your local Android SDK path.

---

## العربية

**Gomiz Player IPTV** هو مشغّل IPTV أصلي لأجهزة **Android TV / Google TV** والهواتف، مبني بلغة **Kotlin** و**Jetpack Compose** ومحرك **AndroidX Media3 (ExoPlayer)** مع **FFmpegKit**.

### المميزات
- قوائم M3U / M3U8 واشتراكات Xtream Codes
- البث المباشر مع تصنيفات القنوات والمفضلة ودرج «كل القنوات»
- دليل البرامج الإلكتروني (EPG / XMLTV) مع «الآن / التالي»
- أفلام ومسلسلات وتنزيلات وتسجيلات
- تسجيل HLS وتنزيل محلي للفيديو
- حلقة تركيز موحّدة وتنقل كامل بريموت D-pad
- أدوات ديناميكية للطقس والساعة وأوقات الصلاة
- واجهة إنجليزية وعربية (دعم RTL)

### الإصدار
الإصدار الحالي: **1.0.4**

### البناء
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
يُنشأ ملف APK في `app/build/outputs/apk/debug/app-debug.apk`.

> ملف `local.properties` مستبعد عمداً من Git لأنه يحتوي مسار Android SDK الخاص بجهازك.
