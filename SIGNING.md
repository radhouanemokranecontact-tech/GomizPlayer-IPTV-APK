# توقيع الإصدار / Release Signing

[English](#english) | [العربية](#العربية)

## العربية

### الحالة الحالية
تم توليد مفتاح توقيع إنتاجي (Self-signed) بتاريخ 2026-11-21:
- **الاسم**: `gomiz-release.jks` — محفوظ في `C:\Users\Radhouanemokrane\Documents\GomizSigning`
- **Alias**: `gomiz`
- **الخوارزمية**: RSA 2048, SHA-384, صلاحية 10000 يوم
- **SHA-256 للشهادة**: `14f0766ccbc5e2fef79ec8ed6a354e35d0d46f5568f8218110f7a77931de4738`

### ⚠️ مهم جداً — احفظ المفتاح
1. **انسخ مجلد `GomizSigning` كاملاً** إلى مكان آمن (قرص خارجي + سحابة مشفّرة).
2. **لا تحذفه أبداً** — فقدانه يعني inability لتوقيع أي تحديث مستقبلي بنفس الهوية، وGoogle Play لن يقبل تحديثاتك.
3. كلمة المرور موجودة في `keystore-password.txt` داخل نفس المجلد.

### البناء محلياً
```powershell
$dir = "C:\Users\Radhouanemokrane\Documents\GomizSigning"
$pw  = Get-Content "$dir\keystore-password.txt" -Raw
$env:GOMIZ_KEYSTORE = "$dir\gomiz-release.jks"
$env:GOMIZ_KEYSTORE_PASSWORD = $pw
$env:GOMIZ_KEY_ALIAS = "gomiz"
$env:GOMIZ_KEY_PASSWORD = $pw
.\gradlew.bat assembleRelease
```
الناتج: `app/build/outputs/apk/release/app-release.apk`

### GitHub Actions (للتوقيع في السحابة)
أضف هذه Secrets في المستودع ← Settings → Secrets and variables → Actions:

| Secret | القيمة |
|---|---|
| `GOMIZ_KEYSTORE_BASE64` | المحتوى المشفّر base64 لملف `gomiz-release.jks` |
| `GOMIZ_KEYSTORE_PASSWORD` | محتوى `keystore-password.txt` |
| `GOMIZ_KEY_ALIAS` | `gomiz` |
| `GOMIZ_KEY_PASSWORD` | نفس كلمة المرور |

لتوليد الـ base64:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$dir\gomiz-release.jks"))
```

---

## توقيع تحديثات PC (Ed25519)

تم توليد زوج مفاتيح Ed25519 لتوقيع ملف `latest.yml` ( manifested التحديث):
- **المفتاح الخاص**: `C:\Users\Radhouanemokrane\Documents\GomizSigning\update-private-key.pem` ⚠️ **سري — لا تشاركه أبداً**
- **المفتاح العام** (مُضمَّن في التطبيق): `update-public-key.pem`
- **keyId**: `2f5e8004ace29b1bc28f460c1c14b6b7d4a294ddf27eb79b0777da9317d33ddb`

### لماذا سكربت مملوك بدل electron-builder 27؟
electron-builder 27 (الذي يوقّع الـ manifest تلقائياً) ما زال **alpha** — ترقيته تُخاطر باستقرار التطبيق. طبّقنا بديلاً بنفس الحماية:
1. `scripts/sign-update-manifest.js` يوقّع `RELEASE_MANIFEST.yml` Ed25519.
2. التطبيق يتحقق من التوقيع (`src/main/manifest-verify.js`) **قبل أي تنزيل** — يرفض manifest غير موقّع أو مُعبَّث.
3. المفتاح العام مُضمَّن في الكود — لا حاجة لخادم.

### عند كل إصدار جديد
```powershell
Copy-Item dist\latest.yml RELEASE_MANIFEST.yml -Force
node scripts\sign-update-manifest.js RELEASE_MANIFEST.yml "C:\Users\Radhouanemokrane\Documents\GomizSigning\update-private-key.pem"
# ثم ارفع RELEASE_MANIFEST.yml إلى جذر المستودع + latest.yml إلى الـ Release
```

---

## English

### Current state
A self-signed production signing key was generated on 2026-11-21:
- **File**: `gomiz-release.jks` — stored in `C:\Users\Radhouanemokrane\Documents\GomizSigning`
- **Alias**: `gomiz`
- **Algorithm**: RSA 2048, SHA-384, validity 10000 days
- **Certificate SHA-256**: `14f0766ccbc5e2fef79ec8ed6a354e35d0d46f5568f8218110f7a77931de4738`

### ⚠️ Back up the key
1. **Copy the whole `GomizSigning` folder** to a safe place (external drive + encrypted cloud).
2. **Never delete it** — losing it means you cannot sign future updates with the same identity, and Google Play will reject them.
3. The password is in `keystore-password.txt` in the same folder.

### GitHub Actions
Add these repository Secrets: `GOMIZ_KEYSTORE_BASE64`, `GOMIZ_KEYSTORE_PASSWORD`, `GOMIZ_KEY_ALIAS`, `GOMIZ_KEY_PASSWORD`.
