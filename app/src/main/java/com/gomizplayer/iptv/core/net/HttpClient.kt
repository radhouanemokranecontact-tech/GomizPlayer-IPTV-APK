package com.gomizplayer.iptv.core.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object HttpClient {
    const val MAX_PLAYLIST_BYTES = 30L * 1024 * 1024
    const val MAX_EPG_BYTES = 60L * 1024 * 1024
    const val DEFAULT_UA = "VLC/3.0.21 LibVLC/3.0.21"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val hasUa = chain.request().header("User-Agent") != null
            val req = if (hasUa) chain.request() else chain.request().newBuilder().header("User-Agent", DEFAULT_UA).build()
            chain.proceed(req)
        }
        .build()

    /** Fetches a URL with an optional per-source User-Agent/Referer/Origin headers, capped at [maxBytes]. */
    fun fetchBytes(
        url: String,
        headers: Map<String, String> = emptyMap(),
        maxBytes: Long = MAX_PLAYLIST_BYTES
    ): ByteArray {
        if (url.startsWith("file:", ignoreCase = true)) {
            val bytes = java.io.File(java.net.URI(url)).readBytes()
            if (bytes.size > maxBytes) throw IllegalStateException("Response too large")
            return bytes
        }
        val req = Request.Builder()
            .url(url)
            .apply {
                if (headers.keys.none { it.equals("User-Agent", true) }) {
                    header("User-Agent", DEFAULT_UA)
                }
                headers.forEach { (k, v) -> header(k, v) }
            }
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            val body = res.body
            val contentLength = body.contentLength()
            if (contentLength > maxBytes) throw IllegalStateException("Response too large (${contentLength / 1048576} MB)")
            val bytes = body.bytes()
            if (bytes.size > maxBytes) throw IllegalStateException("Response too large")
            return bytes
        }
    }
}
