package com.gomizplayer.iptv.core.classify

/**
 * Faithful Kotlin port of src/shared/classification.js (Windows app).
 * Token/word-boundary aware Live/VOD/Series classification.
 */

enum class Kind { LIVE, MOVIE, SERIES, UNKNOWN }

data class MediaMeta(
    val url: String = "",
    val group: String = "",
    val name: String = "",
    val tvgId: String = "",
    val tvgName: String = "",
    val categoryName: String = "",
    val parentCategoryName: String = "",
    val streamType: String = "",
    val contentType: String = "",
    val mediaType: String = "",
    val categoryType: String = "",
    val type: String = "",
    val typeName: String = "",
    val kind: String = ""
)

object Classification {

    private fun norm(v: String?): String = v?.trim() ?: ""
    private fun lower(v: String?): String = norm(v).lowercase()

    // ---- boundary-aware token matching ----
    private fun escapeRe(s: String): String =
        Regex.escape(s)

    private val asciiWord = Regex("^[a-z0-9]+$")

    fun hasToken(text: String?, token: String?): Boolean {
        if (text.isNullOrBlank() || token.isNullOrBlank()) return false
        val t = token.lowercase()
        val src = text.lowercase()
        return if (asciiWord.matches(t)) {
            Regex("(^|[^a-z0-9])" + escapeRe(t) + "([^a-z0-9]|$)").containsMatchIn(src)
        } else {
            Regex("(^|[\\s|\\-_/\\[\\]():,;.])" + escapeRe(t) + "([\\s|\\-_/\\[\\]():,;.]|$)")
                .containsMatchIn(src)
        }
    }

    private fun hasAnyToken(text: String, tokens: List<String>): Boolean =
        tokens.any { hasToken(text, it) }

    private fun matchesAny(text: String, patterns: List<Regex>): Boolean =
        patterns.any { it.containsMatchIn(text) }

    // ---- URL / path helpers ----
    private fun getPathAndQuery(url: String): String = lower(url).substringBefore('#')

    private fun pathHasSegment(u: String, seg: String): Boolean {
        val s = "/" + seg.lowercase() + "/"
        val s2 = "/" + seg.lowercase() + "?"
        val s3 = "/" + seg.lowercase()
        return u.contains(s) || u.contains(s2) || u.endsWith(s3) || u.contains(s3 + "&") || u.contains(s3 + "#")
    }

    // Bounded, thread-safe cache — playlists contain thousands of distinct URLs and
    // classification can run from parallel IO coroutines.
    private const val QUERY_CACHE_MAX = 1024
    private val queryParamCache = object : LinkedHashMap<String, Map<String, String>>(QUERY_CACHE_MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>?): Boolean =
            size > QUERY_CACHE_MAX
    }

    @Synchronized private fun queryParams(u: String): Map<String, String> =
        queryParamCache.getOrPut(u) {
            val idx = u.indexOf('?')
            if (idx < 0) return@getOrPut emptyMap()
            val q = u.substring(idx + 1)
            val out = mutableMapOf<String, String>()
            for (pair in q.split('&')) {
                val eq = pair.indexOf('=')
                if (eq > 0) {
                    val k = pair.substring(0, eq).lowercase()
                    val v = try {
                        java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                    } catch (_: Exception) { pair.substring(eq + 1) }
                    out[k] = v.lowercase()
                }
            }
            out
        }

    private fun getQueryParam(u: String, name: String): String = queryParams(u)[name.lowercase()] ?: ""

    fun getUrlExtension(url: String): String {
        val path = getPathAndQuery(url).substringBefore('?')
        val m = Regex("\\.([a-z0-9]{1,8})$").find(path) ?: return ""
        return m.groupValues[1].lowercase()
    }

    private val VOD_FILE_EXT = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ogv", "3gp")
    private val STREAM_EXT = setOf("m3u8", "ts", "m3u", "mpd")

    // ---- explicit stream type detection ----
    fun explicitTypeFromString(v: String?): Kind? {
        val k = lower(v)
        if (k.isEmpty()) return null
        if (k == "series" || k == "serie" || k == "show" || k == "shows" || k == "tvshow") return Kind.SERIES
        if (k == "movie" || k == "movies" || k == "vod" || k == "film" || k == "films") return Kind.MOVIE
        if (k == "live" || k == "livetv" || k == "live_tv" || k == "channel" || k == "channels") return Kind.LIVE
        return null
    }

    fun explicitTypeFromUrl(url: String?): Kind? {
        val u = getPathAndQuery(url ?: "")
        if (u.isEmpty()) return null
        when (getQueryParam(u, "stream_type")) {
            "series" -> return Kind.SERIES
            "movie", "vod" -> return Kind.MOVIE
            "live" -> return Kind.LIVE
        }
        if (pathHasSegment(u, "series")) return Kind.SERIES
        if (pathHasSegment(u, "movie") || pathHasSegment(u, "vod")) return Kind.MOVIE
        if (pathHasSegment(u, "live")) return Kind.LIVE
        return null
    }

    fun explicitTypeFromFields(item: MediaMeta): Kind? {
        val candidates = listOf(
            item.streamType, item.contentType, item.mediaType, item.categoryType, item.type
        )
        for (c in candidates) explicitTypeFromString(c)?.let { return it }
        when (lower(item.typeName)) {
            "series" -> return Kind.SERIES
            "movie", "vod" -> return Kind.MOVIE
            "live" -> return Kind.LIVE
        }
        return null
    }

    // ---- text/category classification ----
    val SERIES_TOKENS = listOf(
        "series", "serie", "série", "séries", "shows", "show", "tvshow", "sitcom", "drama",
        "مسلسل", "مسلسلات", "موسم", "حلقة", "حلقات", "دراما"
    )
    val SERIES_PATTERNS = listOf(
        Regex("\\bs\\d{1,2}\\s*[._ -]?\\s*e\\d{1,3}\\b", RegexOption.IGNORE_CASE),
        Regex("\\bseason\\s*\\d{1,3}\\b", RegexOption.IGNORE_CASE),
        Regex("\\bepisode\\s*\\d{1,4}\\b", RegexOption.IGNORE_CASE),
        Regex("\\bep\\.?\\s*\\d{1,4}\\b", RegexOption.IGNORE_CASE),
        Regex("\\btv\\s*shows?\\b", RegexOption.IGNORE_CASE)
    )
    val MOVIE_TOKENS = listOf(
        "movie", "movies", "film", "films", "cinema", "cinéma", "vod",
        "فيلم", "أفلام", "افلام"
    )
    val LIVE_TOKENS = listOf(
        "live", "tv", "channel", "channels", "iptv", "broadcast",
        "قناة", "قنوات", "مباشر", "بث",
        "direct", "chaine", "chaîne", "chaines", "chaînes", "en direct"
    )
    val LIVE_CONTEXT_TOKENS = listOf(
        "live", "tv", "channel", "channels", "iptv", "news", "sport", "sports",
        "قناة", "قنوات", "مباشر", "بث", "اخبار", "أخبار", "رياضة", "رياضية"
    )

    fun classifyText(text: String?, avoidAmbiguousMovie: Boolean = false): Kind {
        val s = norm(text)
        if (s.isEmpty()) return Kind.UNKNOWN
        val isSeries = hasAnyToken(s, SERIES_TOKENS) || matchesAny(s, SERIES_PATTERNS)
        val isLive = hasAnyToken(s, LIVE_TOKENS)
        val isMovie = hasAnyToken(s, MOVIE_TOKENS)

        if (isSeries && !isLive) return Kind.SERIES
        if (isSeries && isLive) {
            val strongSeries = matchesAny(s, SERIES_PATTERNS) ||
                hasAnyToken(s, listOf("مسلسل", "مسلسلات", "series", "série", "séries", "show", "shows", "tvshow", "دراما", "drama", "sitcom"))
            val explicitLive = hasAnyToken(s, listOf("live", "channel", "channels", "iptv", "broadcast", "قناة", "قنوات", "مباشر", "بث", "direct", "chaine", "chaîne", "chaines", "chaînes", "en direct"))
            if (strongSeries && !explicitLive) return Kind.SERIES
            if (strongSeries && explicitLive) return Kind.LIVE
            if (explicitLive) return Kind.LIVE
            return Kind.SERIES
        }

        if (isMovie && !isLive) return Kind.MOVIE
        if (isMovie && isLive) {
            if (avoidAmbiguousMovie) return Kind.LIVE
            if (hasAnyToken(s, LIVE_CONTEXT_TOKENS)) return Kind.LIVE
            return Kind.MOVIE
        }

        if (isLive) return Kind.LIVE
        return Kind.UNKNOWN
    }

    private fun isDigits(s: String) = s.all { it.isDigit() }

    fun classifyMedia(item: MediaMeta): Kind {
        // 1) Explicit Xtream stream type and URL paths
        explicitTypeFromUrl(item.url)?.let { return it }

        // .m3u8 is a live playlist (IPTV "Movies"/"Series" groups are 24/7 live channels,
        // not VOD). Only explicit VOD metadata above may override — group/title text must not.
        if (isHlsPlaylistUrl(item.url)) {
            explicitTypeFromFields(item)?.let { return it }
            return Kind.LIVE
        }

        // 2) Explicit IPTV group-title metadata
        val byGroup = classifyText(item.group, avoidAmbiguousMovie = true)
        if (byGroup != Kind.UNKNOWN) return byGroup

        // 3) Category names (skip pure-numeric ids)
        val catParts = buildList {
            listOf(item.categoryName, item.parentCategoryName).forEach {
                val s = norm(it)
                if (s.isNotEmpty() && !isDigits(s)) add(s)
            }
        }
        if (catParts.isNotEmpty()) {
            val byCat = classifyText(catParts.joinToString(" "), avoidAmbiguousMovie = true)
            if (byCat != Kind.UNKNOWN) return byCat
        }

        // 4) tvg-name and item metadata
        val nameParts = listOf(item.tvgName, item.name).filter { it.isNotBlank() }
        if (nameParts.isNotEmpty()) {
            val byName = classifyText(nameParts.joinToString(" "), avoidAmbiguousMovie = false)
            if (byName != Kind.UNKNOWN) return byName
        }

        // 5) Explicit content type stored in the source data
        explicitTypeFromFields(item)?.let { return it }

        normalizeKind(item.kind).takeIf { it != Kind.UNKNOWN }?.let { return it }

        // 6) Extension fallback (never Live solely because of .m3u8/.ts)
        val byExt = classifyByExtension(item.url)
        if (byExt != Kind.UNKNOWN) return byExt

        return Kind.UNKNOWN
    }

    fun classifyByExtension(url: String?): Kind {
        val ext = getUrlExtension(url ?: "")
        if (ext.isEmpty()) return Kind.UNKNOWN
        if (ext in VOD_FILE_EXT) return Kind.MOVIE
        return Kind.UNKNOWN
    }

    fun normalizeKind(k: String?): Kind {
        return when (lower(k)) {
            "live", "livetv" -> Kind.LIVE
            "series", "serie" -> Kind.SERIES
            "movie", "movies", "vod", "film" -> Kind.MOVIE
            else -> Kind.UNKNOWN
        }
    }

    fun isVodFileUrl(url: String?): Boolean = getUrlExtension(url ?: "") in VOD_FILE_EXT

    fun isStreamUrl(url: String?): Boolean = getUrlExtension(url ?: "") in STREAM_EXT

    /** .m3u8 is a live playlist, not a VOD file — IPTV "Movies" groups are 24/7 channels. */
    fun isHlsPlaylistUrl(url: String?): Boolean = getUrlExtension(url ?: "") == "m3u8"
}
