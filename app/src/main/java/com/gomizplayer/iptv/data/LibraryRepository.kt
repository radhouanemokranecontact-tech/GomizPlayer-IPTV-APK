package com.gomizplayer.iptv.data

import android.content.Context
import com.gomizplayer.iptv.core.classify.Classification
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.core.model.SourceEntry
import com.gomizplayer.iptv.core.net.HttpClient
import com.gomizplayer.iptv.core.secrets.SecretBox
import com.gomizplayer.iptv.core.store.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class CachedItem(
    val key: String,
    val name: String,
    val group: String,
    val logo: String,
    val tvgId: String,
    val tvgName: String,
    val url: String,
    val kind: String,
    val hidden: Boolean
)

@Serializable
private data class SourceCache(
    val id: String,
    val fetchedAt: Long,
    val items: List<CachedItem>
)

data class LibraryState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<ChannelItem> = emptyList(),
    val fetchedAt: Long = 0L
)

/**
 * Loads/parses/caches M3U playlists (incl. Xtream get.php endpoints) and exposes
 * the merged item catalog. Android counterpart of Windows readSourceText/loadSource.
 */
class LibraryRepository(context: Context, private val store: Store) {
    private val cacheDir = File(context.cacheDir, "sources").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val perSource = mutableMapOf<String, List<ChannelItem>>()

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    // ---------- cache ----------

    private fun cacheFile(id: String) = File(cacheDir, "source-$id.json")

    private fun toCached(items: List<ChannelItem>): List<CachedItem> = items.map {
        CachedItem(it.key, it.name, it.group, it.logo, it.tvgId, it.tvgName, it.url, it.kind.name, it.hidden)
    }

    private fun fromCached(items: List<CachedItem>): List<ChannelItem> = items.map {
        val stored = runCatching { Kind.valueOf(it.kind) }.getOrDefault(Kind.UNKNOWN)
        // Correct old caches: .m3u8 entries are live channels (IPTV "Movies" groups are
        // 24/7 streams), never VOD unless an explicit type was stored.
        val kind = if (Classification.isHlsPlaylistUrl(it.url) && stored != Kind.LIVE) Kind.LIVE else stored
        ChannelItem(
            key = it.key,
            name = it.name,
            group = it.group,
            logo = it.logo,
            tvgId = it.tvgId,
            tvgName = it.tvgName,
            url = it.url,
            kind = kind,
            headers = emptyMap(),
            hidden = it.hidden
        )
    }

    private fun readCacheFor(id: String): List<ChannelItem>? = try {
        val f = cacheFile(id)
        if (f.exists()) {
            json.decodeFromString(SourceCache.serializer(), f.readText(Charsets.UTF_8)).items.let(::fromCached)
        } else null
    } catch (_: Exception) { null }

    private fun writeCacheFor(id: String, items: List<ChannelItem>) {
        try {
            val tmp = File(cacheDir, "source-$id.json.tmp")
            tmp.writeText(
                json.encodeToString(SourceCache.serializer(), SourceCache(id, System.currentTimeMillis(), toCached(items))),
                Charsets.UTF_8
            )
            val f = cacheFile(id)
            if (!tmp.renameTo(f)) { tmp.copyTo(f, overwrite = true); tmp.delete() }
        } catch (_: Exception) { /* cache is best-effort */ }
    }

    private fun rebuildState(loading: Boolean = false, error: String? = null) {
        val fresh = synchronized(perSource) { perSource.values.flatten() }
        // Never drop an already-loaded catalog when a network refresh fails.
        val items = fresh.ifEmpty { _state.value.items }
        _state.value = LibraryState(
            loading = loading,
            error = error,
            items = items,
            fetchedAt = System.currentTimeMillis()
        )
    }

    /** Populate from on-disk caches (fast, no network). */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        synchronized(perSource) {
            perSource.clear()
            for (source in store.data.value.sources) {
                readCacheFor(source.id)?.let { perSource[source.id] = it }
            }
        }
        rebuildState()
    }

    /** Drop a source from memory and its cache file. */
    fun removeSource(id: String) {
        try { cacheFile(id).delete() } catch (_: Exception) {}
        synchronized(perSource) { perSource.remove(id) }
        rebuildState()
    }

    // ---------- loading ----------

    private fun readSourceText(source: SourceEntry): String {
        val address = if (source.kind == "xtream") {
            val password = SecretBox.decrypt(source.passwordEnc)
                ?: throw IllegalStateException("Could not decrypt source credentials")
            M3u.xtreamUrl(source.url, source.username, password)
        } else source.url
        val bytes = HttpClient.fetchBytes(address, source.headers, HttpClient.MAX_PLAYLIST_BYTES)
        return bytes.toString(Charsets.UTF_8)
    }

    /** Fetch + parse one source; updates its cache and in-memory catalog. */
    suspend fun loadSource(source: SourceEntry): Result<List<ChannelItem>> = withContext(Dispatchers.IO) {
        try {
            val text = readSourceText(source)
            val items: List<ChannelItem> = when {
                M3u.looksLikePlaylist(text) -> M3u.parseM3U(text, baseUri = source.url)
                M3u.isHlsPlaylist(text) -> listOf(
                    ChannelItem(
                        key = M3u.keyFor(source.url),
                        name = source.name.ifBlank { "Stream" },
                        group = "Streams",
                        logo = "",
                        tvgId = "",
                        tvgName = source.name,
                        url = if (source.kind == "xtream") {
                            val password = SecretBox.decrypt(source.passwordEnc)
                                ?: throw IllegalStateException("Could not decrypt source credentials")
                            M3u.xtreamUrl(source.url, source.username, password)
                        } else source.url,
                        kind = Kind.LIVE,
                        headers = source.headers,
                        hidden = false
                    )
                )
                else -> throw IllegalStateException("Not a valid M3U playlist")
            }
            writeCacheFor(source.id, items)
            synchronized(perSource) { perSource[source.id] = items }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Refresh every saved source; update counts in the store. */
    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(loading = true, error = null)
        val sources = store.data.value.sources
        if (sources.isEmpty()) {
            synchronized(perSource) { perSource.clear() }
            rebuildState()
            return@withContext
        }
        var firstError: String? = null
        for (source in sources) {
            val result = loadSource(source)
            result.onSuccess { items ->
                store.upsertSource(source.copy(updatedAt = System.currentTimeMillis(), count = items.size))
            }.onFailure { e ->
                firstError = firstError ?: (e.message ?: "Source load failed")
                // fall back to cache for this source so the UI stays usable
                val cached = readCacheFor(source.id)
                synchronized(perSource) {
                    if (cached != null) perSource[source.id] = cached
                }
            }
        }
        rebuildState(loading = false, error = firstError)
    }

    /** Refresh a single source (after add/edit). */
    suspend fun refreshOne(source: SourceEntry): Result<Int> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(loading = true, error = null)
        val result = loadSource(source)
        result.onSuccess { items ->
            store.upsertSource(source.copy(updatedAt = System.currentTimeMillis(), count = items.size))
        }
        rebuildState(loading = false, error = result.exceptionOrNull()?.message)
        result.map { it.size }
    }

    fun items(): List<ChannelItem> = _state.value.items.filter { !it.hidden }

    fun countsByGroupFor(items: List<ChannelItem>): List<Pair<String, Int>> =
        items.asSequence()
            .groupBy { it.group.ifBlank { "Uncategorized" } }
            .map { (g, list) -> g to list.size }
            .sortedByDescending { it.second }

    fun countsByGroup(kind: Kind): List<Pair<String, Int>> =
        countsByGroupFor(_state.value.items.filter { !it.hidden && it.kind == kind })
}
