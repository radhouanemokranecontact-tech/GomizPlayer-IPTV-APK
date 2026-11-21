package com.gomizplayer.iptv.data

import android.content.Context
import com.gomizplayer.iptv.core.epg.Xmltv
import com.gomizplayer.iptv.core.model.EpgSource
import com.gomizplayer.iptv.core.net.HttpClient
import com.gomizplayer.iptv.core.store.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.GZIPInputStream

@Serializable
private data class EpgCache(
    val id: String,
    val fetchedAt: Long,
    val programmes: List<Xmltv.Programme>
)

data class EpgState(
    val loading: Boolean = false,
    val error: String? = null,
    val counts: Map<String, Int> = emptyMap(),
    val lastRefresh: Long = 0L,
    val loaded: Boolean = false
)

/**
 * XMLTV EPG repository — Android counterpart of Windows refreshEpg/epg:channels.
 * Programmes are time-windowed (now-2h .. now+24h) before caching to bound memory.
 */
class EpgRepository(context: Context, private val store: Store) {
    private val cacheDir = File(context.cacheDir, "epg").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state = MutableStateFlow(EpgState())
    val state: StateFlow<EpgState> = _state.asStateFlow()

    /** channelId -> programmes (sorted by start), kept in memory for the 26h window. */
    private var index: Map<String, List<Xmltv.Programme>> = emptyMap()

    private fun cacheFile(id: String) = File(cacheDir, "epg-$id.json")

    /** Keep only programmes overlapping [now-2h, now+24h] and sort by start. */
    private fun window(programmes: List<Xmltv.Programme>): List<Xmltv.Programme> {
        val now = System.currentTimeMillis()
        val from = now - 2 * 3600_000L
        val to = now + 24 * 3600_000L
        return programmes.asSequence()
            .filter { p -> val s = p.start; val e = p.stop; s != null && e != null && e > from && s < to }
            .sortedWith(compareBy({ it.start }, { it.channel }))
            .toList()
    }

    private fun decodeMaybeGzip(url: String, bytes: ByteArray): String {
        val isGzip = (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) ||
            url.endsWith(".gz", ignoreCase = true)
        val plain = if (isGzip) {
            GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
        } else bytes
        return plain.toString(Charsets.UTF_8)
    }

    suspend fun refresh(entry: EpgSource): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bytes = HttpClient.fetchBytes(entry.url, emptyMap(), HttpClient.MAX_EPG_BYTES)
            val xml = decodeMaybeGzip(entry.url, bytes)
            val programmes = window(Xmltv.parseEpg(xml))
            writeCache(entry.id, programmes)
            rebuildIndex()
            _state.value = _state.value.copy(
                counts = _state.value.counts + (entry.id to programmes.size),
                lastRefresh = System.currentTimeMillis(),
                error = null
            )
            Result.success(programmes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(loading = true, error = null)
        var firstError: String? = null
        for (entry in store.data.value.settings.epgUrls) {
            refresh(entry).onFailure { e -> firstError = firstError ?: (e.message ?: "EPG refresh failed") }
        }
        _state.value = _state.value.copy(loading = false, error = firstError, loaded = true)
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        try { cacheFile(id).delete() } catch (_: Exception) {}
        _state.value = _state.value.copy(counts = _state.value.counts - id)
        rebuildIndex()
    }

    private suspend fun rebuildIndex() = withContext(Dispatchers.IO) {
        val newIndex = mutableMapOf<String, MutableList<Xmltv.Programme>>()
        for (entry in store.data.value.settings.epgUrls) {
            val programmes = readCache(entry.id) ?: continue
            for (p in programmes) {
                if (p.channel.isBlank()) continue
                newIndex.getOrPut(p.channel) { mutableListOf() }.add(p)
            }
        }
        newIndex.values.forEach { it.sortBy { p -> p.start } }
        index = newIndex
    }

    /** Load all configured EPG caches into memory (fast, no network). */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        rebuildIndex()
        val counts = mutableMapOf<String, Int>()
        for (entry in store.data.value.settings.epgUrls) {
            readCache(entry.id)?.let { counts[entry.id] = it.size }
        }
        _state.value = _state.value.copy(counts = counts, loaded = true)
    }

    private fun readCache(id: String): List<Xmltv.Programme>? = try {
        val f = cacheFile(id)
        if (f.exists()) json.decodeFromString(EpgCache.serializer(), f.readText(Charsets.UTF_8)).programmes else null
    } catch (_: Exception) { null }

    private fun writeCache(id: String, programmes: List<Xmltv.Programme>) {
        try {
            val tmp = File(cacheDir, "epg-$id.json.tmp")
            tmp.writeText(
                json.encodeToString(EpgCache(id, System.currentTimeMillis(), programmes)),
                Charsets.UTF_8
            )
            val f = cacheFile(id)
            if (!tmp.renameTo(f)) { tmp.copyTo(f, overwrite = true); tmp.delete() }
        } catch (_: Exception) { /* best-effort */ }
    }

    /** (now, next) programmes for a channel tvg-id. */
    fun programmesBetween(channelId: String, start: Long, end: Long): List<Xmltv.Programme> =
        (index[channelId] ?: emptyList()).filter { p ->
            val s = p.start ?: 0L
            val e = p.stop ?: (s + 30 * 60 * 1000L)
            s < end && e > start
        }

fun nowNext(channelId: String): Pair<Xmltv.Programme?, Xmltv.Programme?> {
        val list = index[channelId] ?: return null to null
        val now = System.currentTimeMillis()
        var nowP: Xmltv.Programme? = null
        var nextP: Xmltv.Programme? = null
        for (p in list) {
            val s = p.start ?: continue
            val e = p.stop ?: continue
            if (s <= now && now < e) { nowP = p; continue }
            if (s >= now && nextP == null) { nextP = p; break }
        }
        return nowP to nextP
    }

    /** All programmes for the given channel ids (for list/grid EPG hints). */
    fun scheduleFor(channelIds: Collection<String>): Map<String, List<Xmltv.Programme>> =
        channelIds.mapNotNull { id -> index[id]?.let { id to it } }.toMap()
}
