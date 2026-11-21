# إشعارات الطرف الثالث / Third-Party Notices

[العربية](#العربية) | [English](#english)

## العربية

يتضمّن هذا التطبيق المكتبات المفتوحة المصدر التالية. تُوزَّع وفق تراخيصها الأصلية:

### FFmpegKit (fork مُصان) — `dev.ffmpegkit-maintained:ffmpeg:8.1.9`
- **الرخصة:** GNU Lesser General Public License v3.0 (LGPL-3.0)
- **المصدر:** https://github.com/ffmpegkit-maintained/ffmpeg
- **الاستخدام في التطبيق:** remux محلي فقط (TS → MP4 بـ `-c copy`) لتسجيلات البث والتنزيلات. لا يُستخدم للشبكة (الشبكة عبر OkHttp/Media3).
- **نص الرخصة:** مُرفق داخل ملف AAR باسم `license.txt` (مع تراخيص المكوّنات: `license_dav1d.txt`, `license_libaom.txt`, `license_libvpx.txt`, `license_opus.txt`, `license_libvorbis.txt`, `license_libogg.txt`, `license_speex.txt`, `license_cpu_features.txt`).
- **الحقوق:** © FFmpeg contributors · © Jokobee (ffmpegkit-maintained fork).

### مكتبات أخرى
تعتمد التطبيق أيضاً على مكتبات AndroidX و Media3 و Kotlinx و Coil و OkHttp و Kotlin Serialization — جميعها مرخّصة بموجب Apache License 2.0 أو ما يعادله، والمذكورة في ملفات POM الخاصة بكل تبعية.

---

## English

This app includes the following open-source libraries, distributed under their original licenses:

### FFmpegKit (community-maintained fork) — `dev.ffmpegkit-maintained:ffmpeg:8.1.9`
- **License:** GNU Lesser General Public License v3.0 (LGPL-3.0)
- **Source:** https://github.com/ffmpegkit-maintained/ffmpeg
- **Use in this app:** local remux only (TS → MP4 via `-c copy`) for stream recordings and downloads. Never used for network I/O (network goes through OkHttp/Media3).
- **License text:** bundled inside the AAR as `license.txt` (plus component licenses: dav1d, libaom, libvpx, Opus, Vorbis, Ogg, Speex, cpu_features).
- **Copyright:** © FFmpeg contributors · © Jokobee (maintained fork).

### Other libraries
AndroidX, Media3, Kotlinx, Coil, OkHttp and Kotlin Serialization are used under Apache 2.0 or equivalent licenses, as stated in each dependency's POM.
