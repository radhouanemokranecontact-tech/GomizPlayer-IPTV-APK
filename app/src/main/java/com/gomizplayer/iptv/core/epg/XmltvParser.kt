package com.gomizplayer.iptv.core.epg

import kotlinx.serialization.Serializable

/**
 * Tolerant XMLTV / EPG parser — Kotlin port of src/main/epg.js (Windows app).
 */
object Xmltv {

    @Serializable
    data class Programme(
        val channel: String,
        val start: Long?,
        val stop: Long?,
        val title: String,
        val desc: String,
        val category: String
    )

    private fun decodeEntities(s: String): String =
        s.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("&#(\\d+);")) { String(Character.toChars(it.groupValues[1].toInt())) }
            .replace(Regex("&#x([0-9a-fA-F]+);")) { String(Character.toChars(it.groupValues[1].toInt(16))) }
            .replace("&amp;", "&")

    private fun stripTags(s: String): String =
        decodeEntities(s).replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    fun parseXmltvTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val v = value.trim()
        val m = Regex("^(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})?\\s*([+-]\\d{4})?").find(v)
        if (m != null) {
            val gv = m.groupValues
            val se = gv[6].ifBlank { "0" }
            val tz = gv[7]
            // Lenient: invalid fields (e.g. hour 24) yield null, never throw.
            return runCatching {
                var ts = java.time.ZonedDateTime.of(
                    gv[1].toInt(), gv[2].toInt(), gv[3].toInt(), gv[4].toInt(), gv[5].toInt(), se.toInt(), 0,
                    java.time.ZoneOffset.UTC
                ).toInstant().toEpochMilli()
                if (tz.isNotBlank()) {
                    val sign = if (tz[0] == '-') -1 else 1
                    val hours = tz.substring(1, 3).toInt()
                    val mins = tz.substring(3, 5).toInt()
                    ts -= sign * (hours * 60L + mins) * 60_000L
                }
                ts
            }.getOrNull()
        }
        return try { java.time.Instant.parse(v).toEpochMilli() } catch (_: Exception) { null }
    }

    fun parseEpg(xml: String, maxProgrammes: Int = 200_000): List<Programme> {
        val programmes = mutableListOf<Programme>()
        val attrRe = Regex("([a-zA-Z]+)=\"([^\"]*)\"")
        val tagRe = Regex(
            "<(title|desc|category)[^>]*>([\\s\\S]*?)</\\1>",
            RegexOption.IGNORE_CASE
        )
        val blockRe = Regex(
            "<programme\\b([^>]*)>([\\s\\S]*?)</programme>",
            RegexOption.IGNORE_CASE
        )

        for (m in blockRe.findAll(xml)) {
            val attrs = m.groupValues[1]
            val body = m.groupValues[2]
            val attrMap = attrRe.findAll(attrs).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
            val start = parseXmltvTime(attrMap["start"])
            val stop = parseXmltvTime(attrMap["stop"])
            var title = ""
            var desc = ""
            var category = ""
            for (t in tagRe.findAll(body)) {
                when (t.groupValues[1].lowercase()) {
                    "title" -> title = stripTags(t.groupValues[2])
                    "desc" -> desc = stripTags(t.groupValues[2])
                    "category" -> category = stripTags(t.groupValues[2])
                }
            }
            programmes.add(
                Programme(
                    channel = decodeEntities(attrMap["channel"] ?: ""),
                    start = start,
                    stop = stop,
                    title = title,
                    desc = desc,
                    category = category
                )
            )
            if (programmes.size >= maxProgrammes) break
        }
        return programmes
    }
}
