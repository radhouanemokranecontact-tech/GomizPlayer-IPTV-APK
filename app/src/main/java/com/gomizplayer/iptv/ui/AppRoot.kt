package com.gomizplayer.iptv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.catalog.CatalogKind
import com.gomizplayer.iptv.ui.catalog.CatalogScreen
import com.gomizplayer.iptv.ui.favorites.FavoritesScreen
import com.gomizplayer.iptv.ui.guide.GuideScreen
import com.gomizplayer.iptv.ui.home.HomeScreen
import com.gomizplayer.iptv.ui.modals.DetailModal
import com.gomizplayer.iptv.ui.livereceiver.LiveReceiverScreen
import com.gomizplayer.iptv.ui.downloads.DownloadsScreen
import com.gomizplayer.iptv.ui.downloads.RecordingsScreen
import com.gomizplayer.iptv.ui.player.PlayerHost
import com.gomizplayer.iptv.ui.settings.SettingsScreen
import com.gomizplayer.iptv.ui.settings.SourcesPanel
import com.gomizplayer.iptv.ui.shell.AppShell
import com.gomizplayer.iptv.ui.toast.Toasts
import com.gomizplayer.iptv.ui.theme.GomizTheme
import com.gomizplayer.iptv.ui.theme.parseAccent
import kotlinx.coroutines.launch

private fun mediaFileItem(file: java.io.File, group: String) = ChannelItem(
    key = file.absolutePath,
    name = file.nameWithoutExtension,
    group = group,
    logo = "",
    tvgId = "",
    tvgName = file.nameWithoutExtension,
    url = file.toURI().toString(),
    kind = Kind.MOVIE,
    headers = emptyMap(),
    hidden = false
)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = gomiz(context)
    val data by app.store.data.collectAsState()

    val darkTheme = !data.settings.theme.equals("light", ignoreCase = true)
    val accent = parseAccent(data.settings.accent)
    val accent2 = parseAccent(data.settings.accent2)

    var route by remember { mutableStateOf("home") }
    var playing by remember { mutableStateOf<ChannelItem?>(null) }
    var receiverMode by remember { mutableStateOf<String?>(null) } // "live" | "favorites"
    var guideReturn by remember { mutableStateOf<String?>(null) } // "receiver:live" | "receiver:favorites" | "player" - where the TV Guide was opened from
    var detailItem by remember { mutableStateOf<ChannelItem?>(null) }
    val sourcesFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(route) {
        if (route == "sources") {
            kotlinx.coroutines.delay(250)
            runCatching { sourcesFocus.requestFocus() }
        }
    }
    var unlocked by remember { mutableStateOf(!data.settings.appLockEnabled || !com.gomizplayer.iptv.core.secrets.PinStore.hasPin(context)) }

    val libraryState by app.library.state.collectAsState()

    val livePlaylist = remember(libraryState.items) {
        libraryState.items.filter { !it.hidden && (it.kind == Kind.LIVE || it.kind == Kind.UNKNOWN) && it.url.isNotBlank() }
    }
    val favoritesLive = remember(data) {
        data.library[data.settings.profile]?.favorites.orEmpty()
            .filter { it.kind == "live" }
            .map {
                ChannelItem(
                    key = it.key,
                    name = it.name,
                    group = it.group,
                    logo = it.logo,
                    tvgId = it.tvgId,
                    tvgName = it.name,
                    url = it.url,
                    kind = Kind.LIVE,
                    headers = emptyMap(),
                    hidden = false
                )
            }
    }
    val lastLiveKey = remember(data, livePlaylist) {
        val lib = data.library[data.settings.profile]
        lib?.liveHistory?.firstOrNull { h -> livePlaylist.any { it.key == h.key } }?.key
            ?: lib?.history?.firstOrNull { h -> h.mode == "live" && livePlaylist.any { it.key == h.key } }?.key
    }
    val receiverPlaylist = if (receiverMode == "favorites") favoritesLive else livePlaylist
    val receiverInitialIndex = remember(receiverPlaylist, lastLiveKey) {
        val idx = receiverPlaylist.indexOfFirst { it.key == lastLiveKey }
        if (idx >= 0) idx else 0
    }

    fun leaveGuide() {
        val ret = guideReturn
        guideReturn = null
        when (ret) {
            "receiver:live" -> { receiverMode = "live"; route = "home" }
            "receiver:favorites" -> { receiverMode = "favorites"; route = "home" }
            else -> { route = "home" }
        }
    }

    GomizTheme(darkTheme = darkTheme, accent = accent, accent2 = accent2) {
        if (!unlocked) {
            com.gomizplayer.iptv.ui.lock.LockScreen(onUnlocked = { unlocked = true })
            return@GomizTheme
        }
        AppShell(
            route = receiverMode ?: route,
            onRoute = { r ->
                if (r == "live" || r == "favorites") {
                    receiverMode = r
                    route = r
                } else {
                    route = r
                    receiverMode = null
                }
            },
            onGuide = { guideReturn = null; route = "guide" }
        ) {
            when (route) {
                "home" -> HomeScreen(
                    onPlay = { playing = it },
                    onOpenSettings = { route = "settings" },
                    onNavigate = { route = it },
                    onDetail = { detailItem = it }
                )
                "live" -> androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
                "favorites" -> androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
                "movies" -> CatalogScreen(CatalogKind.MOVIES, onPlay = { playing = it }, onDetail = { detailItem = it })
                "series" -> CatalogScreen(CatalogKind.SERIES, onPlay = { playing = it }, onDetail = { detailItem = it })
                "myvod" -> FavoritesScreen(onPlay = { playing = it }, initialTab = 1, onDetail = { detailItem = it })
                "downloads" -> com.gomizplayer.iptv.ui.downloads.DownloadsScreen(onOpenSources = { route = "sources" }, onPlay = { file -> playing = mediaFileItem(file, "Downloads") })
                "recordings" -> com.gomizplayer.iptv.ui.downloads.RecordingsScreen(onOpenSources = { route = "sources" }, onPlay = { file -> playing = mediaFileItem(file, "Recordings") })
                "sources" -> SourcesPanel(app = app, data = data, onExit = { route = "home" }, entryFocus = sourcesFocus)
                "settings" -> SettingsScreen()
                "guide" -> GuideScreen(
                    onPlay = { playing = it },
                    onBack = { leaveGuide() },
                    onTimeline = { route = "guide_timeline" },
                    focusKey = lastLiveKey
                )
                "guide_timeline" -> com.gomizplayer.iptv.ui.guide.GuideTimelineScreen(onPlay = { playing = it }, onBack = { route = "guide" })
            }
        }

        detailItem?.let { item ->
            DetailModal(
                item = item,
                allItems = libraryState.items.filter { !it.hidden },
                onPlay = { playing = it },
                onDownload = { downloadItem ->
                    Toasts.show("Downloading ${downloadItem.name}...")
                    app.scope.launch {
                        val result = app.media.download(downloadItem.url, downloadItem.name, downloadItem.headers)
                        if (result.isSuccess) {
                            Toasts.show("Downloaded: ${result.getOrNull()?.name}")
                        } else {
                            Toasts.show("Download failed: ${result.exceptionOrNull()?.message}", isError = true)
                        }
                    }
                },
                onClose = { detailItem = null }
            )
        }

        receiverMode?.let { mode ->
            LiveReceiverScreen(
                playlist = receiverPlaylist,
                favoritesMode = mode == "favorites",
                initialIndex = receiverInitialIndex,
                onClose = {
                    receiverMode = null
                    route = "home"
                },
                onOpenGuide = {
                    guideReturn = "receiver:" + mode
                    route = "guide"
                }
            )
        }

        playing?.let { item ->
            PlayerHost(
                item = item,
                onClose = { playing = null },
                onOpenGuide = { guideReturn = "player"; route = "guide" }
            )
        }
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
            com.gomizplayer.iptv.ui.toast.ToastHost(
                modifier = androidx.compose.ui.Modifier.align(Alignment.TopEnd)
            )
        }

        BackHandler {
            when {
                route == "guide" -> leaveGuide()
                receiverMode != null -> receiverMode = null
                playing != null -> playing = null
                route != "home" -> route = "home"
            }
        }
    }
}
