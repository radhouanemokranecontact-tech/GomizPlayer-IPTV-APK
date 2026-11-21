package com.gomizplayer.iptv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItem(
    val key: String,
    val name: String = "",
    val group: String = "",
    val kind: String = "",
    val url: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val addedAt: Long = 0L
)

@Serializable
data class HistoryItem(
    val key: String,
    val name: String = "",
    val group: String = "",
    val kind: String = "",
    val url: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val mode: String = "vod", // "live" | "vod"
    val playedAt: Long = 0L,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

@Serializable
data class ProfileLibrary(
    val favorites: List<FavoriteItem> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val liveHistory: List<HistoryItem> = emptyList(),
    val searches: List<String> = emptyList()
)

@Serializable
data class EpgSource(
    val id: String,
    val url: String,
    val name: String = ""
)

@Serializable
data class SourceEntry(
    val id: String,
    val name: String,
    val url: String,
    val kind: String = "m3u", // "m3u" | "xtream"
    val username: String = "",
    val passwordEnc: String = "", // AES/GCM via Android Keystore, base64(iv||ct)
    val headers: Map<String, String> = emptyMap(),
    val addedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val count: Int = 0
)

@Serializable
data class AppSettings(
    val language: String = "en",
    val theme: String = "dark", // dark | light
    val accent: String = "#ff3d5a",
    val accent2: String = "#ff7a45",
    val density: String = "comfortable", // compact | comfortable | spacious
    val rowLimit: Int = 16,
    val showMoviesOnHome: Boolean = true,
    val showSeriesOnHome: Boolean = true,
    val showLiveOnHome: Boolean = true,
    val autoNext: Boolean = true,
    val aspect: String = "contain", // contain | cover | fill | 16:9 | 4:3 | stretch
    val seekStep: Int = 10,
    val epgUrls: List<EpgSource> = emptyList(),
    val profile: String = "default",
    val telemetryEnabled: Boolean = false,
    val widgetClock: Boolean = true,
    val widgetWeather: Boolean = true,
    val widgetPrayer: Boolean = true,
    val weatherCity: String = "",
    val weatherLat: Double = 0.0,
    val weatherLon: Double = 0.0,
    val weatherCountry: String = "",
    val tempUnit: String = "C",
    val timeFormat: String = "24",
    val prayerMethod: Int = 2,
    val prayerSchool: Int = 0,
    val subtitleSize: Int = 16,
    val subtitleColor: String = "#FFFFFF",
    val subtitleBg: String = "#00000080",
    val subtitlePosition: String = "bottom",
    val subtitleDelayMs: Int = 0,
    val subtitleBold: Boolean = false,
    val subtitleScale: Int = 100,
    val subtitleOpacity: Int = 55,
    val subtitleBottom: Int = 8,
    val subtitleDelay: Float = 0f,
    val hardwareDecoding: Boolean = true,
    val uiScale: Int = 100,
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val volume: Int = 100,
    val rate: Float = 1f,
    val quality: Int = 0,
    val preferredAudioLang: String = "",
    val preferredSubtitleLang: String = "",
    val retryCount: Int = 2,
    val networkTimeout: Int = 30,
    val proxyMode: String = "system",
    val proxyUrl: String = "",
    val clearSourceCacheOnLaunch: Boolean = false,
    val clearWatchHistoryOnLaunch: Boolean = false,
    val clearNetworkCacheOnLaunch: Boolean = false,
    val telemetryEndpoint: String = "",
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val consentShown: Boolean = false,
    val telemetryInstallId: String = "",
    val telemetryLastPing: Long = 0L,
    val downloadDirUri: String = "",
    val recordingDirUri: String = "",
    val userAgent: String = "GomizPlayer/1.0.2 (Android)",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val homeSectionOrder: String = "hero,live,movies,series",
    val hiddenSections: String = ""
)

@Serializable
data class AppData(
    val settings: AppSettings = AppSettings(),
    val sources: List<SourceEntry> = emptyList(),
    val library: Map<String, ProfileLibrary> = mapOf("default" to ProfileLibrary())
)
