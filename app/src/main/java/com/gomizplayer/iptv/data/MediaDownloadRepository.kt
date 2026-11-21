package com.gomizplayer.iptv.data

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Records live streams and downloads (VOD) over HTTP(S) using OkHttp (TLS works everywhere
 * the app can play channels), writing raw MPEG-TS segments, then remuxing locally to MP4
 * with ffmpeg (no network needed). No native TLS required in the bundled ffmpeg build.
 */
class MediaDownloadRepository(private val context: Context) {

    companion object {
        /** Live HLS playlists have no ENDLIST; cap downloads so they always finish. */
        private const val MAX_LIVE_DOWNLOAD_MS = 120_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val dir = File(context.filesDir, "media").apply { mkdirs() }

    fun recordingsDir() = File(dir, "recordings").apply { mkdirs() }
    fun downloadsDir() = File(dir, "downloads").apply { mkdirs() }

    @Volatile private var recording = false
    private var lastTs: File? = null
    private var recordJob: Job? = null

    // ---------------- public API ----------------

    /** Start an asynchronous recording. Returns the intermediate .ts file handle. */
    fun startRecord(url: String, channelName: String, headers: Map<String, String> = emptyMap()): File {
        recording = true
        val safe = channelName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40)
        val ts = File(recordingsDir(), "$safe-${System.currentTimeMillis()}.ts")
        lastTs = ts
        recordJob = scope.launch {
            try {
                if (isHls(url)) recordHls(url, ts, headers, stopWhenDone = false)
                else copyProgressive(url, ts, headers, stopWhenDone = false)
            } catch (_: Exception) { /* stream ended / network error */ }
        }
        return ts
    }

    /** Stop the active recording; remux to mp4 and return the final file (or the .ts on remux failure). */
    suspend fun stopRecord(): File? {
        recording = false
        recordJob?.join()
        val ts = lastTs ?: return null
        lastTs = null
        val mp4 = File(ts.parentFile, ts.name.removeSuffix(".ts") + ".mp4")
        val remixed = withContext(Dispatchers.IO) {
            FFmpegKit.execute("-i \"${ts.absolutePath}\" -c copy -movflags +faststart -y \"${mp4.absolutePath}\"")
        }
        return if (ReturnCode.isSuccess(remixed.returnCode)) {
            runCatching { ts.delete() }
            mp4
        } else {
            ts
        }
    }

    /** Download a VOD URL (M3U/m3u8 or progressive mp4/ts) to an mp4 file. Blocking; returns Result. */
    suspend fun download(url: String, title: String, headers: Map<String, String> = emptyMap()): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val safe = title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40)
                if (isHls(url)) {
                    val ts = File(downloadsDir(), "$safe-${System.currentTimeMillis()}.ts.part")
                    recordHls(url, ts, headers, stopWhenDone = true)
                    val mp4 = File(ts.parentFile, ts.name.removeSuffix(".ts.part") + ".mp4")
                    val remixed = FFmpegKit.execute("-i \"${ts.absolutePath}\" -c copy -movflags +faststart -y \"${mp4.absolutePath}\"")
                    if (ReturnCode.isSuccess(remixed.returnCode)) {
                        ts.delete()
                        Result.success(mp4)
                    } else {
                        val renamed = File(ts.parentFile, ts.name.removeSuffix(".part"))
                        runCatching { ts.renameTo(renamed) }
                        Result.success(renamed)
                    }
                } else {
                    val out = File(downloadsDir(), "$safe-${System.currentTimeMillis()}.mp4")
                    copyProgressive(url, out, headers, stopWhenDone = true)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun listRecordings(): List<File> = recordingsDir().listFiles()?.filter { it.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun listDownloads(): List<File> = downloadsDir().listFiles()?.filter { it.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList()

    // ---------------- internals ----------------

    private fun isHls(url: String): Boolean = url.contains("m3u8", ignoreCase = true)

    private fun defaultHeaders(extra: Map<String, String>): Map<String, String> =
        mapOf("User-Agent" to com.gomizplayer.iptv.core.net.HttpClient.DEFAULT_UA) + extra

    /** Stream a progressive (non-HLS) response to a file. */
    private suspend fun copyProgressive(url: String, out: File, headers: Map<String, String>, stopWhenDone: Boolean) {
        val request = Request.Builder()
            .url(url)
            .headers(defaultHeaders(headers).toHeaders())
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            resp.body?.byteStream()?.use { input ->
                out.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (recording || stopWhenDone) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                    }
                    output.flush()
                }
            }
        }
    }

    /** Download HLS (master or media playlist) segments and write them concatenated to `tsOut`. */
    private suspend fun recordHls(url: String, tsOut: File, headers: Map<String, String>, stopWhenDone: Boolean) {
        // Resolve master -> variant media playlist (OkHttp follows redirects; keep the FINAL url as base)
        var mediaUrl = url
        val head = fetch(url, headers) ?: return
        var headTxt = head.first
        mediaUrl = head.second
        if (headTxt.contains("#EXT-X-STREAM-INF")) {
            val variant = pickVariant(headTxt, mediaUrl) ?: return
            val v = fetch(variant, headers) ?: return
            mediaUrl = v.second
            headTxt = v.first
        }

        // Live channels (no #EXT-X-ENDLIST) stream forever — cap download mode so the
        // file is finalized and remuxed instead of growing until storage is full.
        val deadlineMs = if (stopWhenDone) System.currentTimeMillis() + MAX_LIVE_DOWNLOAD_MS else Long.MAX_VALUE
        val downloaded = LinkedHashSet<String>()
        var idleRounds = 0
        tsOut.outputStream().use { out ->
            while (true) {
                if (!recording && !stopWhenDone) break
                if (System.currentTimeMillis() >= deadlineMs) break
                val playlistResp = fetch(mediaUrl, headers)
                val playlist = playlistResp?.first
                if (playlist != null) {
                    val segments = parseSegments(playlist, mediaUrl)
                    var wrote = 0
                    for (seg in segments) {
                        if (!recording && !stopWhenDone) break
                        if (downloaded.contains(seg)) continue
                        val bytes = fetchBytes(seg, headers) ?: continue
                        out.write(bytes)
                        out.flush()
                        downloaded.add(seg)
                        wrote++
                    }
                    if (stopWhenDone && playlist.contains("#EXT-X-ENDLIST") && wrote == 0) {
                        // finished a finite VOD playlist
                        break
                    }
                    if (wrote == 0) {
                        idleRounds++
                        if (idleRounds > 12 && !stopWhenDone) {
                            // stalled; keep waiting but cap idle time
                            delay(2000)
                        }
                    } else {
                        idleRounds = 0
                    }
                } else {
                    delay(1500)
                }
                delay(1000)
            }
        }
    }

    private suspend fun fetch(url: String, headers: Map<String, String>): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .headers(defaultHeaders(headers).toHeaders())
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) null else (resp.body?.string() ?: "") to resp.request.url.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchBytes(url: String, headers: Map<String, String>): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .headers(defaultHeaders(headers).toHeaders())
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.bytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pickVariant(master: String, baseUrl: String): String? {
        // Pick the LOWEST bandwidth variant so live recording can keep up even on slow networks
        var best: Pair<Long, String>? = null
        val lines = master.lines()
        for (i in lines.indices) {
            if (lines[i].startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                val bw = Regex("BANDWIDTH=(\\d+)").find(lines[i])?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
                val next = lines.getOrNull(i + 1)?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("#") }
                if (next != null) {
                    val abs = resolve(baseUrl, next)
                    if (best == null || bw < best!!.first) best = bw to abs
                }
            }
        }
        return best?.second
    }

    private fun parseSegments(playlist: String, baseUrl: String): List<String> {
        val out = mutableListOf<String>()
        for (raw in playlist.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            out += resolve(baseUrl, line)
        }
        return out
    }

    private fun resolve(base: String, relative: String): String = try {
        java.net.URL(base).let { baseUrl ->
            java.net.URL(baseUrl, relative).toString()
        }
    } catch (_: Exception) {
        relative
    }
}
