package com.gomizplayer.iptv.ui.livereceiver

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.core.model.FavoriteItem
import com.gomizplayer.iptv.core.model.HistoryItem
import com.gomizplayer.iptv.core.net.HttpClient
import com.gomizplayer.iptv.ui.kit.SearchField
import com.gomizplayer.iptv.ui.player.PlayerBottomDock
import com.gomizplayer.iptv.ui.player.InfoPanel
import com.gomizplayer.iptv.ui.player.PlayerTopBar
import com.gomizplayer.iptv.ui.player.StatsPanel
import com.gomizplayer.iptv.ui.player.capturePlayerFrame
import com.gomizplayer.iptv.ui.theme.GomizColors
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import com.gomizplayer.iptv.ui.toast.Toasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LiveReceiverScreen(
    playlist: List<ChannelItem>,
    favoritesMode: Boolean,
    initialIndex: Int,
    onClose: () -> Unit,
    onOpenGuide: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current

    var currentIndex by remember(playlist) { mutableStateOf(initialIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))) }
    var drawerOpen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var dockFocused by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf("AUTO") }
    var aspectIndex by remember { mutableIntStateOf(0) }
    var speedIndex by remember { mutableIntStateOf(3) }
    var locked by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) } // 0 off 1 one 2 all
    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<java.io.File?>(null) }
    var recCount by remember { mutableIntStateOf(0) }
    var recStartedAt by remember { mutableLongStateOf(0L) }
    var recElapsed by remember { mutableLongStateOf(0L) }
    var statsOpen by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var sleepEndAt by remember { mutableStateOf(0L) }

    // Finalize any active recording on exit so no partial .ts is left dangling.
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) app.scope.launch { app.media.stopRecord() }
        }
    }

    if (playlist.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(if (favoritesMode) R.string.favorites_empty else R.string.empty_home_title),
                    color = Color.White,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.back), color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val current = playlist[currentIndex]

    var player by remember(current.key) { mutableStateOf<ExoPlayer?>(null) }
    var playerViewRef by remember(current.key) { mutableStateOf<PlayerView?>(null) }
    var latencySeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(current.key) {
        app.store.pushHistory(
            HistoryItem(
                key = current.key,
                name = current.name,
                group = current.group,
                kind = "live",
                url = current.url,
                logo = current.logo,
                tvgId = current.tvgId,
                mode = "live",
                playedAt = System.currentTimeMillis()
            )
        )
    }

    DisposableEffect(current.key) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(HttpClient.DEFAULT_UA)
            .setDefaultRequestProperties(current.headers)
        val exo = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        exo.setMediaItem(MediaItem.fromUri(Uri.parse(current.url)))
        exo.prepare()
        exo.playWhenReady = true
        player = exo
        onDispose {
            exo.release()
            player = null
        }
    }

    // Player listener: isPlaying + videoSize -> quality
    DisposableEffect(player) {
        val exo = player
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isP: Boolean) { isPlaying = isP }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                val h = videoSize.height
                quality = if (h > 0) "${h}p" else "AUTO"
            }
        }
        exo?.addListener(listener)
        onDispose { exo?.removeListener(listener) }
    }

    // Sleep timer countdown
    LaunchedEffect(sleepEndAt) {
        while (sleepEndAt > 0) {
            val remaining = sleepEndAt - System.currentTimeMillis()
            if (remaining <= 0) {
                player?.pause()
                sleepEndAt = 0L
                sleepMinutes = 0
                Toasts.show("Sleep timer ended")
                break
            }
            delay(5000)
        }
    }

    // Auto-dismiss panels opened by buttons (except REC overlay, which stays while recording)
    LaunchedEffect(infoOpen) {
        if (infoOpen) {
            delay(6000)
            infoOpen = false
        }
    }
    LaunchedEffect(statsOpen) {
        if (statsOpen) {
            delay(6000)
            statsOpen = false
        }
    }

    // Recording elapsed ticker
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recStartedAt = System.currentTimeMillis()
            while (isRecording) {
                recElapsed = System.currentTimeMillis() - recStartedAt
                delay(1000)
            }
        }
    }

    // Latency polling via live offset (distance from live edge, like Windows)
    LaunchedEffect(player) {
        while (true) {
            val exo = player
            if (exo != null) {
                val offset = exo.currentLiveOffset
                latencySeconds = when {
                    offset == androidx.media3.common.C.TIME_UNSET -> 0 // not reported by this stream
                    offset > 0 -> (offset / 1000).toInt().coerceIn(1, 3600)
                    else -> 0 // at live edge
                }
            }
            delay(1000)
        }
    }

    // Auto-hide the chrome after inactivity, but NEVER while D-pad focus is inside
    // the dock — otherwise the bar vanished mid-navigation.
    LaunchedEffect(controlsVisible, drawerOpen, statsOpen, infoOpen, dockFocused) {
        if (controlsVisible && !drawerOpen && !statsOpen && !infoOpen && !dockFocused) {
            delay(6000)
            controlsVisible = false
        }
    }

    BackHandler {
        if (drawerOpen) drawerOpen = false else if (!controlsVisible) controlsVisible = true else onClose()
    }

    val epgNow = remember(current.tvgId) { current.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).first?.title } }
    val epgNext = remember(current.tvgId) { current.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).second?.title } }
    val epgProgress = remember(current.tvgId, current.key) {
        current.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).first }?.let { p ->
            val s = p.start; val e = p.stop
            if (s != null && e != null && e > s) ((System.currentTimeMillis() - s).toFloat() / (e - s).toFloat()).coerceIn(0f, 1f) else null
        }
    }
    val epgRemaining = remember(current.key, current.tvgId) {
        current.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).first?.stop }?.let { stop ->
            val mins = ((stop - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
            if (mins > 0) "${mins / 60}h ${mins % 60}m" else "<1m"
        }
    }
    val epgNextTime = remember(current.tvgId) {
        current.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).second }?.start?.let {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
        }
    }

    val data by app.store.data.collectAsState()
    val favoriteSet = remember(data) {
        data.library[data.settings.profile]?.favorites.orEmpty().map { it.key }.toHashSet()
    }
    val drawerOffset = if (playlist.map { it.group.ifBlank { "Uncategorized" } }.toSet().size > 1) 574.dp else 374.dp

    val rootFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    // When the dock hides, its focusable buttons leave composition and focus would fall
    // back to the first focusable (the back arrow) — making OK exit the player by accident.
    // Reclaim focus to the root Box instead so OK just re-shows the dock.
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            kotlinx.coroutines.delay(50)
            runCatching { rootFocus.requestFocus() }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable(true)
            .onPreviewKeyEvent { event ->
                if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                val wasVisible = controlsVisible
                controlsVisible = true // any remote key re-shows the chrome
                when (event.key) {
                    androidx.compose.ui.input.key.Key.MediaPlayPause -> {
                        player?.let { it.playWhenReady = !it.playWhenReady; isPlaying = it.playWhenReady }
                        true
                    }
                    androidx.compose.ui.input.key.Key.MediaFastForward -> {
                        player?.let { exo -> exo.seekTo(exo.currentPosition + 10_000L) }
                        true
                    }
                    androidx.compose.ui.input.key.Key.MediaRewind -> {
                        player?.let { exo -> exo.seekTo((exo.currentPosition - 10_000L).coerceAtLeast(0)) }
                        true
                    }
                    // When chrome is hidden, left/right seek; when visible they move dock focus.
                    androidx.compose.ui.input.key.Key.DirectionLeft -> {
                        if (!wasVisible) {
                            player?.let { exo -> exo.seekTo((exo.currentPosition - 10_000L).coerceAtLeast(0)) }
                            true
                        } else false
                    }
                    androidx.compose.ui.input.key.Key.DirectionRight -> {
                        if (!wasVisible) {
                            player?.let { exo -> exo.seekTo(exo.currentPosition + 10_000L) }
                            true
                        } else false
                    }
                    else -> false
                }
            }
            .clickable { controlsVisible = !controlsVisible }
    ) {
        player?.let { exo ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exo
                        useController = false
                        keepScreenOn = true
                        setShowSubtitleButton(false)
                        playerViewRef = this
                    }
                },
                update = { view ->
                    view.resizeMode = when (aspectIndex) {
                        1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ---------- Drawer (Windows "All channels" drawer, slide-in + remote focus) ----------
        androidx.compose.animation.AnimatedVisibility(
            visible = drawerOpen,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + androidx.compose.animation.fadeOut()
        ) {
            Row(Modifier.fillMaxSize()) {
                ChannelDrawer(
                    items = playlist,
                    favoritesMode = favoritesMode,
                    currentKey = current.key,
                    recordingKey = if (isRecording) current.key else null,
                    onPlayItem = { item ->
                        val orig = playlist.indexOfFirst { it.key == item.key }
                        if (orig >= 0 && orig != currentIndex) currentIndex = orig
                        drawerOpen = false
                        controlsVisible = true
                    },
                    onClose = { drawerOpen = false },
                    modifier = Modifier.fillMaxHeight()
                )
                Box(Modifier.fillMaxHeight().weight(1f).clickable { drawerOpen = false })
            }
        }

        // ---------- Stats panel ----------
        if (statsOpen) {
            StatsPanel(
                title = current.name,
                group = current.group,
                epg = epgNow,
                aspect = when (aspectIndex) { 0 -> "Fit"; 1 -> "Zoom"; else -> "Fill" },
                speed = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)[speedIndex],
                latency = latencySeconds,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
            )
        }

        // ---------- Quick info panel ----------
        if (infoOpen) {
            InfoPanel(
                title = current.name,
                group = current.group,
                url = current.url,
                epg = epgNow,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
            )
        }

        // ---------- REC timer overlay (Windows-style) ----------
        if (isRecording) {
            val transition = rememberInfiniteTransition(label = "recPulse")
            val recDot by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "recDot"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCCFF3D5A))
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = recDot))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "REC",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = current.name,
                    color = Color(0xF2FFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatRecTime(recElapsed),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ---------- Top bar ----------
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopStart),
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut()
        ) {
            PlayerTopBar(
                isLive = true,
                latencySeconds = latencySeconds.takeIf { it >= 1 },
                onBack = { if (drawerOpen) drawerOpen = false else onClose() },
                modifier = Modifier.padding(start = if (drawerOpen) drawerOffset else 0.dp)
            )
        }

        // ---------- Bottom dock ----------
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomStart),
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
        ) {
            PlayerBottomDock(
            title = current.name,
            group = current.group,
            logo = current.logo.takeIf { it.isNotBlank() },
            quality = quality,
            isLive = true,
            epgNowTitle = epgNow,
            epgNowRemaining = epgRemaining,
            epgProgress = epgProgress,
            epgNextTitle = epgNext,
            epgNextTime = epgNextTime,
            seekNowMs = 0L,
            seekDurationMs = 0L,
            onSeekFraction = {},
            isPlaying = isPlaying,
            aspect = when (aspectIndex) { 0 -> "Fit"; 1 -> "Zoom"; else -> "Fill" },
            speed = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)[speedIndex],
            repeatMode = repeatMode,
            locked = locked,
            isFavorite = favoriteSet.contains(current.key),
            isRecording = isRecording,
            recCount = recCount,
            drawerOpen = drawerOpen,
            showPrevNext = true,
            controlsVisible = controlsVisible,
            onDockFocusChange = { dockFocused = it },
            onTogglePlay = { player?.let { it.playWhenReady = !it.playWhenReady } },
            onStop = { player?.pause() },
            onPrevChannel = { if (!locked) currentIndex = (currentIndex - 1 + playlist.size) % playlist.size },
            onNextChannel = { if (!locked) currentIndex = (currentIndex + 1) % playlist.size },
            onSeekBack = { if (!locked) player?.let { it.seekTo(maxOf(0, it.currentPosition - 10_000L)) } },
            onSeekForward = { if (!locked) player?.let { it.seekTo(it.currentPosition + 10_000L) } },
            onReload = {
                player?.let { it.seekToDefaultPosition(); it.prepare(); it.playWhenReady = true; Toasts.show("Reloaded") }
            },
            onToggleLock = { locked = !locked; Toasts.show(if (locked) "Controls locked" else "Controls unlocked") },
            onScreenshot = {
                val pv = playerViewRef
                if (pv != null) {
                    app.scope.launch {
                        val bitmap = kotlinx.coroutines.withTimeoutOrNull(2500) { capturePlayerFrame(pv) }
                        if (bitmap != null) {
                            withContext(Dispatchers.IO) { saveBitmapToPictures(context, bitmap, current.name) }
                            withContext(Dispatchers.Main) { Toasts.show("Screenshot saved") }
                        } else {
                            withContext(Dispatchers.Main) { Toasts.show("Screenshot not supported", isError = true) }
                        }
                    }
                } else {
                    Toasts.show("Screenshot not supported", isError = true)
                }
            },
            onSleepTimer = {
                sleepMinutes = when (sleepMinutes) { 0 -> 15; 15 -> 30; 30 -> 45; 45 -> 60; else -> 0 }
                sleepEndAt = if (sleepMinutes > 0) System.currentTimeMillis() + sleepMinutes * 60_000L else 0L
                Toasts.show(if (sleepMinutes == 0) "Sleep timer off" else "Sleep timer: $sleepMinutes min")
            },
            onToggleRepeat = {
                repeatMode = (repeatMode + 1) % 3
                player?.repeatMode = when (repeatMode) {
                    1 -> Player.REPEAT_MODE_ONE
                    2 -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                Toasts.show(when (repeatMode) { 1 -> "Repeat one"; 2 -> "Repeat all"; else -> "Repeat off" })
            },
            onCycleAspect = {
                aspectIndex = (aspectIndex + 1) % 3
                Toasts.show("Aspect: ${when (aspectIndex) { 0 -> "Fit"; 1 -> "Zoom"; else -> "Fill" }}")
            },
            onCycleSpeed = {
                speedIndex = (speedIndex + 1) % 7
                player?.setPlaybackSpeed(listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)[speedIndex])
            },
            onCycleAudio = {
                player?.let { Toasts.show(com.gomizplayer.iptv.ui.player.cycleAudioTrack(it)) }
                    ?: Toasts.show("Audio: not ready")
            },
            onToggleFavorite = {
                app.store.toggleFavorite(
                    FavoriteItem(
                        key = current.key,
                        name = current.name,
                        group = current.group,
                        kind = "live",
                        url = current.url,
                        logo = current.logo,
                        tvgId = current.tvgId
                    )
                )
            },
            onShowEpg = { onOpenGuide?.invoke() },
            onShowInfo = { infoOpen = !infoOpen; statsOpen = false },
            onToggleStats = { statsOpen = !statsOpen; infoOpen = false },
            onToggleDrawer = { drawerOpen = !drawerOpen },
            onGoLive = { player?.let { it.seekToDefaultPosition(); it.playWhenReady = true } },
            onRecord = {
                if (!isRecording) {
                    isRecording = true
                    recCount += 1
                    recStartedAt = System.currentTimeMillis()
                    recordingFile = app.media.startRecord(current.url, current.name, current.headers)
                    Toasts.show("Recording started")
                } else {
                    isRecording = false
                    app.scope.launch {
                        val mp4 = app.media.stopRecord()
                        withContext(Dispatchers.Main) {
                            Toasts.show(if (mp4 != null) "Recording saved: ${mp4.name}" else "Recording stopped")
                            recordingFile = null
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = if (drawerOpen) drawerOffset else 0.dp)
        )
        }
    }
}

private fun saveBitmapToPictures(context: Context, bitmap: android.graphics.Bitmap?, displayName: String): Boolean {
    if (bitmap == null) return false
    return runCatching {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "GomizPlayer_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        true
    }.getOrDefault(false)
}

private fun formatRecTime(ms: Long): String {
    val sec = ms / 1000
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
