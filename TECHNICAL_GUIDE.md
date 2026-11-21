# Gomiz Player IPTV (Android) — الدليل التقني الشامل

[English](#english) | [العربية](#العربية)

> مبني على فحص الكود الفعلي والنسخة 1.0.4 — وليس على وثائق تخطيطية.
> ما هو مكتوب هنا مُنجز ومُختبَر؛ وما ليس مذكوراً غير موجود.

## العربية

### 1) ما هو هذا التطبيق؟
مشغّل IPTV أصلي لـ **Android TV / Google TV** والهواتف (Android 7+ / API 24)، بواجهة **Jetpack Compose** ومحرك **AndroidX Media3 (ExoPlayer)**. يعمل بالريموت (D-pad) بواسطة نظام تركيز موحّد. الإصدار الحالي: **1.0.4** (`versionCode 104`).

### 2) ماذا يفعل بالضبط؟ (من تجربة الاستخدام)

**القائمة الرئيسية (Home):**
- شريط أدوات علوي: بحث، دليل البرامج (TV Guide)، زر «Add source».
- ثلاثة مربعات (widgets) ديناميكية:
  - **ساعة تناظرية حيّة** تتحرك عقاربها كل ثانية، بخلفية تتغيّر مع الوقت (فجر/نهار/غروب/ليل).
  - **طقس** بحالة الطقس الحقيقية (شمس/قمر/غيوم/مطر/ثلج/ضباب/برق) وخلفية تتبدّل مع الحالة.
  - **أوقات الصلاة** بالعدّ التنازلي، بخلفية تتحوّل لذهبية عند قرب الصلاة.
- كل مربع فيه زر تحديث صغير يُحدّث البيانات والموقع الجغرافي فوراً.
- أقسام: «مباشر الآن»، أفلام، مسلسلات، «أكمل المشاهدة»، «أُضيف حديثاً».

**Live TV (البث المباشر):**
- يفتح مباشرة على آخر قناة مشاهدة — لا توجد صفحة منفصلة.
- درج القنوات (Channel Drawer): كل القنوات مصنّفة بمجموعات، بحث، ومفضلة ♥.
- عند فشل بث: نافذة «تعذّر تشغيل هذا البث» مع **Retry** و**القناة التالية** و**Close** → تعود لقائمة القنوات.

**الدليل (EPG):**
- يعرض «الآن / التالي» لكل قناة، مع جدول زمني وتنقّل بالريموت، وتركيز تلقائي على القناة الحالية.

**الأفلام والمسلسلات:**
- تصفّح حسب المجموعات، تفاصيل العمل (DetailModal)، مفضلة، بحث، سجل مشاهدة.

**المفضلة / قوائم المستخدم (Profiles):**
- ملفات شخصية متعددة (`default` + ما تنشئه) — كل واحد له مفضلتهم وسجلهم.
- «My List TV» و «My Movies & Series».

**التنزيلات والتسجيلات:**
- تسجيل البث المباشر (HLS) بحد أقصى 120 ثانية للبث الحيّ، ثم remux محلي إلى MP4 عبر FFmpegKit (LGPL-3.0 — انظر `THIRD_PARTY_NOTICES.md`).
- تنزيل VOD إلى MP4، وإدارة الملفات (حذف/فتح مجلد).

**الإعدادات (10 تبويبات):**
`General` · `Home` · `Player` · `Subtitles` · `Storage` · `Widgets` · `Cleanup` · `System` · `Sources` · `About`
- **اللغات:** EN / FR / AR (كامل 137 سلسلة لكل لغة ✅ ودعم RTL).
- **المصادر:** M3U/M3U8 · Xtream Codes · رابط مفرد + رؤوس HTTP واسم مستخدم/كلمة مرور (مُشفّرة عبر Android Keystore في `SecretBox`).
- **قفل التطبيق:** PinStore + `appLockEnabled`.
- **النسخ الاحتياطي:** نسخ تلقائي كل 30 دقيقة (3 نسخ دوّارة) + استعادة تلقائية عند تلف الملف + أزرار «Back up now / Export JSON / Restore».

### 3) التفاصيل التقنية (للمطوّرين/الوكلاء)

| العنصر | الحقيقة في الكود |
|---|---|
| الحزمة | `com.gomizplayer.iptv` |
| المشغّل | Media3 / ExoPlayer + `DefaultDataSource` |
| التخزين | ملف JSON (`gomizplayer.json`) في `filesDir` + نسخ في `filesDir/backups` |
| الأسرار | `SecretBox` + `PinStore` (Keystore) |
| التصنيف | `Classification.kt` مع query cache محدود وقاعدة HLS→LIVE |
| EPG | `XmltvParser` + `EpgRepository` + cache |
| الطقس/الصلاة | Open-Meteo · Aladhan · ipwho.is (انظر `PRIVACY.md`) |
| FFmpeg | `dev.ffmpegkit-maintained:ffmpeg:8.1.9` — LGPL-3.0، remux فقط |
| الأمان | `network_security_config.xml`: HTTPS إجباري على النطاقات الحساسة، cleartext مسموح لبث IPTV |
| الشبكة | OkHttp مع TLS + Network Security Config |
| التوقيع | keystore إنتاجي (RSA 2048) — انظر `SIGNING.md` |
| البناء | `./gradlew assembleDebug` · `assembleRelease` · `testDebugUnitTest` |
| الاختبارات | **36 اختبار وحدة** ناجح + CI على GitHub Actions |

### 4) حدود معروفة (صدقاً)
- لا Catch-up كامل، لا Multiview، لا Stalker، لا Chromecast/DLNA (مؤجّلة — انظر PC/ROADMAP.md).
- لم يُختبر على جهاز Google TV فيزيائي — تحقق المحاكي فقط.
- التحقق من التوقيع الإنتاجي تم بـ `apksigner`، والنشر التجاري لم يحدث بعد.

---

## English

Gomiz Player IPTV is a native **Android TV / Google TV** and phone IPTV player (Android 7+, API 24) built with Jetpack Compose and AndroidX Media3 (ExoPlayer). Version **1.0.4**.

**What it actually does:** Home with a live analog clock, weather and prayer-time widgets (all with dynamic backgrounds and per-card refresh); Live TV opening straight into the last channel with a full categorized channel drawer; EPG Now/Next; movies, series, favorites, multi-profile libraries; HLS recording (120s live cap) and VOD download with local MP4 remux; 10 settings tabs; EN/FR/AR (137 strings each, RTL ready); M3U/Xtream/single sources; app lock; automatic rolling backups with auto-recovery.

**Under the hood:** JSON store + backups · Android Keystore secrets · bounded URL classification cache · XMLTV EPG parser · Open-Meteo/Aladhan/ipwho.is (see PRIVACY.md) · FFmpegKit LGPL-3.0 (see THIRD_PARTY_NOTICES.md) · Network Security Config forcing HTTPS on sensitive domains · production-signed release (see SIGNING.md) · 36 passing unit tests + CI.

**Known limits:** no full Catch-up/Multiview/Stalker/Cast; emulator-validated only; no commercial release yet.
