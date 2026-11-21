# سياسة الخصوصية — Gomiz Player IPTV (Android)

[English](#english)

آخر تحديث: 2026-11-21

يوضح هذا المستند كيفية تعامل Gomiz Player IPTV مع البيانات.

## العربية

### 1) البيانات التي تبقى على جهازك
يخزّن التطبيق مصادرك وقوائمك والمفضلة والإعدادات وأوقات التشغيل في ملف محلي (`gomizplayer.json`) داخل مساحة بيانات التطبيق. مع نسخ احتياطية دورية في نفس المجلد. **لا تُرسل هذه البيانات لأي خادم.**

### 2) الخدمات الخارجية

**تحديد الموقع الجغرافي (ipwho.is):**
- عند عدم إدخال مدينة يدوياً، يرسل التطبيق طلباً إلى `https://ipwho.is/` عند الإقلاع لتحديد موقعك التقريبي (المدينة/الدولة) من عنوان IP، لعرض الطقس وأوقات الصلاة في الأدوات الرئيسية.
- يُرسل **عنوان IP فقط** تقنياً — لا اسمك ولا بيانات حسابك ولا قوائمك. لا نخزّن أي شيء على خوادمنا (لا يوجد خادم لدينا لهذا الغرض).
- **لتجنّب هذا الطلب:** افتح الإعدادات ← أدخل اسم المدينة يدوياً؛ عندها لن يُستدعى ipwho.is إطلاقاً.

**الطقس:** `api.open-meteo.com` (درجة الحرارة، الحالة، الرياح) — مجاني بلا مفتاح API.
**أوقات الصلاة:** `api.aladhan.com` (حسب الإحداثيات والطريقة التي تختارها).
**الترجمة الجغرافية:** `geocoding-api.open-meteo.com` عند إدخال اسم مدينة.
**التحديثات:** `github.com` للتحقق من الإصدارات الجديدة (موقّعة رقمياً).

تنطبق سياسات خصوصية هذه الخدمات على الطلبات المرسلة إليها.

### 3) ما لا نجمعه
- لا نجمع كلمات المرور أو بيانات حساباتك (تُشفّر محلياً عبر Android Keystore).
- لا نجمع قوائم التشغيل أو المحتوى أو وسائطك الشخصية.
- لا نستخدم تحليلات أو تقارير أعطال تلقائية تُرسل للخادم.

### 4) التحديثات والتحقق
كل تحديث يُ تحقَّق من توقيعه الرقمي (Ed25519) قبل التنزيل. لا يمكن تثبيت ملف modification غير موقّع.

### 5) التحكم
يمكنك حذف كل البيانات المحلية من الإعدادات ← التخزين ← مسح البيانات.

### 6) التواصل
https://github.com/radhouanemokranecontact-tech/GomizPlayer-IPTV-APK

---

## English

Gomiz Player IPTV stores your sources, playlists, favorites, settings and watch history **locally only** (`gomizplayer.json` in the app's private storage, with rolling backups). Nothing is uploaded.

**Geolocation (ipwho.is):** If no city is configured, the app calls `https://ipwho.is/` on startup to approximate your city/country from your IP address, for the weather and prayer-time widgets. Only your IP is technically transmitted — no name, account or playlist data. To opt out, enter a city manually in Settings.

**Weather:** `api.open-meteo.com` · **Prayer times:** `api.aladhan.com` · **Geocoding:** `geocoding-api.open-meteo.com` · **Updates:** `github.com` (signed manifests verified before download).

No analytics or automatic crash reports are sent. Credentials are encrypted locally via Android Keystore. Updates are Ed25519-verified before any download.
