package com.gomizplayer.iptv.core.store

import android.content.Context
import com.gomizplayer.iptv.core.model.AppData
import com.gomizplayer.iptv.core.model.AppSettings
import com.gomizplayer.iptv.core.model.FavoriteItem
import com.gomizplayer.iptv.core.model.HistoryItem
import com.gomizplayer.iptv.core.model.ProfileLibrary
import com.gomizplayer.iptv.core.model.SourceEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-backed JSON store with atomic writes — Android counterpart of the Windows
 * gomizplayer.json store (src/main/store.js). Same data shape to ease future
 * Windows <-> Android backup/restore.
 */
class Store(context: Context) {
    private val dir: File = context.filesDir
    private val file: File = File(dir, "gomizplayer.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    fun snapshot(): String = json.encodeToString(AppData.serializer(), _data.value)

    /** Replace all app data with the contents of a gomizplayer.json backup. */
    fun restoreFromJson(text: String): Result<Unit> = try {
        val decoded = json.decodeFromString(AppData.serializer(), text)
        _data.value = decoded
        persist()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun load(): AppData {
        return try {
            if (file.exists()) {
                json.decodeFromString(AppData.serializer(), file.readText(Charsets.UTF_8))
            } else {
                AppData()
            }
        } catch (_: Exception) {
            try {
                file.copyTo(File(dir, "gomizplayer.corrupt.json"), overwrite = true)
            } catch (_: Exception) { /* ignore */ }
            AppData()
        }
    }

    @Synchronized
    private fun persist() {
        val tmp = File(dir, "gomizplayer.json.tmp")
        try {
            tmp.writeText(json.encodeToString(AppData.serializer(), _data.value), Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        } catch (_: Exception) { /* keep running; data stays in memory */ }
    }

    private fun update(block: (AppData) -> AppData) {
        _data.update(block)
        persist()
    }

    fun patchSettings(patch: AppSettings) {
        update { it.copy(settings = patch) }
    }

    // ---------- profiles ----------

    fun currentProfile(): String = _data.value.settings.profile.ifBlank { "default" }

    fun profiles(): List<String> = _data.value.library.keys.toList().ifEmpty { listOf("default") }

    fun createProfile(name: String): String {
        val id = name.trim().take(40)
        require(id.isNotEmpty()) { "Invalid profile name" }
        update { d ->
            if (d.library.containsKey(id)) return@update d
            d.copy(library = d.library + (id to ProfileLibrary()))
        }
        return id
    }

    fun deleteProfile(name: String): String {
        val id = name.trim()
        require(id != "default") { "Cannot delete default profile" }
        update { d ->
            val lib = d.library.toMutableMap().apply { remove(id) }
            val settings = if (d.settings.profile == id) d.settings.copy(profile = "default") else d.settings
            d.copy(library = lib, settings = settings)
        }
        return "default"
    }

    fun switchProfile(name: String) {
        val id = name.trim().ifBlank { "default" }
        update { d ->
            val lib = if (d.library.containsKey(id)) d.library else d.library + (id to ProfileLibrary())
            d.copy(library = lib, settings = d.settings.copy(profile = id))
        }
    }

    private fun updateLibrary(profile: String, block: (ProfileLibrary) -> ProfileLibrary) {
        update { d ->
            val id = profile.ifBlank { "default" }
            val lib = d.library[id] ?: ProfileLibrary()
            d.copy(library = d.library + (id to block(lib)))
        }
    }

    fun currentLibrary(): ProfileLibrary =
        _data.value.library[currentProfile()] ?: ProfileLibrary()

    // ---------- favorites ----------

    fun isFavorite(key: String): Boolean =
        currentLibrary().favorites.any { it.key == key }

    fun toggleFavorite(item: FavoriteItem): Boolean {
        var added = false
        updateLibrary(currentProfile()) { lib ->
            val exists = lib.favorites.any { it.key == item.key }
            added = !exists
            if (exists) {
                lib.copy(favorites = lib.favorites.filterNot { it.key == item.key })
            } else {
                lib.copy(favorites = (listOf(item.copy(addedAt = System.currentTimeMillis())) + lib.favorites).take(2000))
            }
        }
        return added
    }

    // ---------- history ----------

    fun pushHistory(item: HistoryItem) {
        updateLibrary(currentProfile()) { lib ->
            if (item.mode == "live") {
                val filtered = lib.liveHistory.filterNot { it.key == item.key && it.mode == item.mode }
                lib.copy(liveHistory = (listOf(item.copy(playedAt = System.currentTimeMillis())) + filtered).take(100))
            } else {
                val filtered = lib.history.filterNot { it.key == item.key }
                lib.copy(history = (listOf(item.copy(playedAt = System.currentTimeMillis())) + filtered).take(1000))
            }
        }
    }

    fun patchHistoryPosition(key: String, positionMs: Long, durationMs: Long) {
        updateLibrary(currentProfile()) { lib ->
            lib.copy(
                history = lib.history.map {
                    if (it.key == key) it.copy(positionMs = positionMs, durationMs = durationMs) else it
                }
            )
        }
    }

    fun historyPosition(key: String): Pair<Long, Long>? {
        val h = currentLibrary().history.firstOrNull { it.key == key } ?: return null
        return h.positionMs to h.durationMs
    }

    fun clearHistory() = updateLibrary(currentProfile()) { it.copy(history = emptyList(), liveHistory = emptyList()) }

    fun clearSearches() = updateLibrary(currentProfile()) { it.copy(searches = emptyList()) }

    fun clearAllLibrary() = updateLibrary(currentProfile()) { it.copy(favorites = emptyList(), history = emptyList(), liveHistory = emptyList(), searches = emptyList()) }

    fun resetSettings() = update { it.copy(settings = AppSettings()) }

    fun clearFavorites() = updateLibrary(currentProfile()) { it.copy(favorites = emptyList()) }

    // ---------- search ----------

    fun addSearch(term: String) {
        val t = term.trim()
        if (t.isEmpty()) return
        updateLibrary(currentProfile()) { lib ->
            val filtered = lib.searches.filterNot { it.equals(t, ignoreCase = true) }
            lib.copy(searches = (listOf(t) + filtered).take(30))
        }
    }

    // ---------- sources ----------

    fun upsertSource(source: SourceEntry) {
        update { d ->
            val idx = d.sources.indexOfFirst { it.id == source.id }
            val list = d.sources.toMutableList()
            if (idx >= 0) list[idx] = source else list.add(source)
            d.copy(sources = list)
        }
    }

    fun removeSource(id: String) {
        update { d -> d.copy(sources = d.sources.filterNot { it.id == id }) }
    }
}
