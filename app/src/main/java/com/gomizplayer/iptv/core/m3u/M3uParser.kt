package com.gomizplayer.iptv.core.m3u

import com.gomizplayer.iptv.core.classify.Classification
import com.gomizplayer.iptv.core.classify.MediaMeta
import com.gomizplayer.iptv.core.classify.Kind
import java.security.MessageDigest
import java.net.URL
import java.net.URLEncoder

object M3u {

    private val SKIP_HINT = Regex("(adult|xxx|18\\+|لكبار|اباحي|إباحي)", RegexOption.IGNORE_CASE)

    fun keyFor(url: String): String = sha1(url)

    /** Stable key for a playlist entry: URL + group + name. */
    fun itemKey(url: String, group: String?, name: String?): String =
        sha1(url + '\u0000' + (group ?: "") + '\u0000' + (name ?: ""))

    private fun sha1(input: String): String =
        MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun looksLikePlaylist(text: String): Boolean =
        Regex("^\\uFEFF?\\s*#EXTM3U", RegexOption.IGNORE_CASE).containsMatchIn(text.take(4096)) ||
            Regex("#EXTINF:", RegexOption.IGNORE_CASE).containsMatchIn(text.take(4096))

    fun isHlsPlaylist(text: String): Boolean =
        Regex("#EXT-X-", RegexOption.IGNORE_CASE).containsMatchIn(text.take(20000))

    fun xtreamAccountUrl(url: String?, username: String?, password: String?): String {
        var base = url?.trim().orEmpty()
        if (base.isEmpty()) return base
        if (!Regex("^[a-z]+://", RegexOption.IGNORE_CASE).containsMatchIn(base)) base = "http://$base"
        if (username.isNullOrEmpty() && password.isNullOrEmpty()) return base
        return try {
            val u = URL(base)
            var path = u.path.trimEnd('/')
            if (!Regex("/player_api\\.php$", RegexOption.IGNORE_CASE).containsMatchIn(path)) {
                path += "/player_api.php"
            }
            val q = "username=${enc(username ?: "")}&password=${enc(password ?: "")}"
            "${u.protocol}://${u.host}${if (u.port != -1) ":${u.port}" else ""}$path?$q"
        } catch (_: Exception) {
            base
        }
    }

    fun xtreamUrl(url: String?, username: String?, password: String?): String {
        var base = url?.trim().orEmpty()
        if (base.isEmpty()) return base
        if (!Regex("^[a-z]+://", RegexOption.IGNORE_CASE).containsMatchIn(base)) base = "http://$base"
        if (username.isNullOrEmpty() && password.isNullOrEmpty()) return base
        return try {
            val u = URL(base)
            var path = u.path.trimEnd('/')
            if (!Regex("/get\\.php$", RegexOption.IGNORE_CASE).containsMatchIn(path)) {
                path += "/get.php"
            }
            val q = "username=${enc(username ?: "")}&password=${enc(password ?: "")}&type=m3u_plus&output=ts"
            "${u.protocol}://${u.host}${if (u.port != -1) ":${u.port}" else ""}$path?$q"
        } catch (_: Exception) {
            base
        }
    }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    data class ChannelItem(
        val key: String,
        val name: String,
        val group: String,
        val logo: String,
        val tvgId: String,
        val tvgName: String,
        val url: String,
        val kind: Kind,
        val headers: Map<String, String>,
        val hidden: Boolean
    )

    private fun parseAttributes(line: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val re = Regex("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")
        for (m in re.findAll(line)) {
            out[m.groupValues[1].lowercase()] = m.groupValues[2]
        }
        return out
    }

    fun parseM3U(text: String, baseUri: String? = null): List<ChannelItem> {
        val items = mutableListOf<ChannelItem>()
        var extinfGroup = ""
        var extinfTvgId = ""
        var extinfTvgName = ""
        var extgrpGroup = ""
        var pendingName = ""
        var pendingLogo = ""
        var pendingHeaders = mutableMapOf<String, String>()

        fun push(rawUrl: String) {
            var url = rawUrl.trim()
            if (url.isEmpty() || url.startsWith("#")) return
            url = try {
                if (!baseUri.isNullOrEmpty()) URL(URL(baseUri), url).toString() else url
            } catch (_: Exception) { url }

            val name = pendingName.ifBlank { url }
            val group = extinfGroup.ifBlank { extgrpGroup.ifBlank { "Uncategorized" } }
            val tvgId = extinfTvgId
            val tvgName = extinfTvgName
            val kind = Classification.classifyMedia(
                MediaMeta(url = url, group = group, name = name, tvgId = tvgId, tvgName = tvgName)
            )
            items.add(
                ChannelItem(
                    key = itemKey(url, group, name),
                    name = name,
                    group = group,
                    logo = pendingLogo,
                    tvgId = tvgId,
                    tvgName = tvgName,
                    url = url,
                    kind = kind,
                    headers = pendingHeaders.toMap(),
                    hidden = SKIP_HINT.containsMatchIn(group) || SKIP_HINT.containsMatchIn(name)
                )
            )
            extinfGroup = ""; pendingName = ""; pendingLogo = ""
            extinfTvgId = ""; extinfTvgName = ""
            pendingHeaders = mutableMapOf()
        }

        for (raw in text.lineSequence()) {
            var line = raw.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val afterColon = line.substring(8)
                val commaIdx = afterColon.indexOf(',')
                val attrsSection = if (commaIdx >= 0) afterColon.substring(0, commaIdx) else afterColon
                val title = if (commaIdx >= 0) afterColon.substring(commaIdx + 1).trim() else ""
                val attrs = parseAttributes(attrsSection)
                pendingName = title.ifBlank { attrs["tvg-name"] ?: attrs["tvg-id"] ?: "Channel" }
                extinfGroup = attrs["group-title"] ?: ""
                pendingLogo = attrs["tvg-logo"] ?: ""
                extinfTvgId = attrs["tvg-id"] ?: ""
                extinfTvgName = attrs["tvg-name"] ?: ""
                continue
            }

            if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                extgrpGroup = line.substring(8).trim()
                continue
            }

            if (line.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                val kv = line.substring(11)
                val eq = kv.indexOf('=')
                if (eq > 0) {
                    val k = kv.substring(0, eq).lowercase()
                    val v = kv.substring(eq + 1)
                    when (k) {
                        "http-user-agent" -> pendingHeaders["User-Agent"] = v
                        "http-referrer", "http-referer" -> pendingHeaders["Referer"] = v
                        "http-origin" -> pendingHeaders["Origin"] = v
                    }
                }
                continue
            }

            if (line.startsWith("#")) continue
            push(line)
        }

        return items
    }
}
