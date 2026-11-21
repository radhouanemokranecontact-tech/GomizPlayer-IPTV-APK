# GomizPlayer IPTV — Release Keystore Setup (future)

These steps create a signing key for the Google Play release. Do NOT commit the keystore or passwords.

## 1. Generate the keystore
```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" `
  -genkeypair -v `
  -keystore "C:\Users\Radhouanemokrane\AndroidStudioProjects\GomizPlayerIPTV\release.keystore" `
  -alias gomiz_release `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -dname "CN=GomizPlayer IPTV, OU=Android, O=GomizPlayer, L=Tunis, ST=Tunis, C=TN"
# enter store password + key password when prompted (use a strong password, save it securely)
```

## 2. Sign the release build
Add to `app/build.gradle.kts` (already wired via env/gradle props — see `signingConfigs.release`):
```
GOMIZ_KEYSTORE=<path to release.keystore>
GOMIZ_KEYSTORE_PASSWORD=<store password>
GOMIZ_KEY_ALIAS=gomiz_release
GOMIZ_KEY_PASSWORD=<key password>
```
Then:
```powershell
./gradlew bundleRelease   # produces app/build/outputs/bundle/release/app-release.aab
```

## 3. Gradle properties (local machine, git-ignored)
Add to `gradle.properties` (do NOT commit):
```
GOMIZ_KEYSTORE=C:\Users\Radhouanemokrane\AndroidStudioProjects\GomizPlayerIPTV\release.keystore
GOMIZ_KEYSTORE_PASSWORD=***
GOMIZ_KEY_ALIAS=gomiz_release
GOMIZ_KEY_PASSWORD=***
```
OR set environment variables before running Gradle (preferred for CI secrets).

## 4. Google Play Console
1. Create app `com.gomizplayer.iptv` (package com.gomizplayer.iptv)
2. Upload `app-release.aab` (Play App Signing recommended: Google holds the upload key)
3. Fill store listing (assets in `store/`: descriptions, privacy policy, icon, screenshots)
4. Create Internal Testing track with testers; promote to Production when stable
5. Add release notes (v1.0.3): Live TV/Movies/Series, M3U/Xtream, EPG timeline, downloads/recordings, app lock, backup/restore

## 5. Keep the key safe
- `.gitignore` already excludes `*.keystore`, `*.jks`, `*.p12`
- Back up the keystore + passwords in a secure vault; Google Play cannot recover a lost upload key
- Never print passwords into logs, chat, or issues
