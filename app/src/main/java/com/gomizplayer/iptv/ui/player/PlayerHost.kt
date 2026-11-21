package com.gomizplayer.iptv.ui.player

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
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
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.core.model.FavoriteItem
import com.gomizplayer.iptv.core.model.HistoryItem
import com.gomizplayer.iptv.core.net.HttpClient
import com.gomizplayer.iptv.ui.toast.Toasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerHost(item: ChannelItem, onClose: () -> Unit, onOpenGuide: (() -> Unit)? = null) {
    val context = LocalContext.current
    val app = gomiz(context)

    var isPlaying by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf("AUTO") }
    var aspectIndex by remember { mutableIntStateOf(0) }
    var speedIndex by remember { mutableIntStateOf(3) }
    var locked by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var recCount by remember { mutableIntStateOf(0) }
    var statsOpen by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var sleepEndAt by remember { mutableStateOf(0L) }

    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    var player by remember(item.key) { mutableStateOf<ExoPlayer?>(null) }
    var playerViewRef by remember(item.key) { mutableStateOf<PlayerView?>(null) }

    val data by app.store.data.collectAsState()
    val isFavorite = data.library[data.settings.profile]?.favorites?.any { it.key == item.key } == true

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

    LaunchedEffect(item.key) {
        app.store.pushHistory(
            HistoryItem(
                key = item.key,
                name = item.name,
                group = item.group,
                kind = item.kind.name.lowercase(),
                url = item.url,
                logo = item.logo,
                tvgId = item.tvgId,
                mode = if (item.kind == Kind.LIVE) "live" else "vod",
                playedAt = System.currentTimeMillis()
            )
        )
        // resume
        app.store.historyPosition(item.key)?.let { (pos, dur) ->
            if (pos in 5_000L..(dur * 0.95).toLong().coerceAtLeast(10_000L)) {
                // will seek after prepare
            }
        }
    }

    DisposableEffect(item.key) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(HttpClient.DEFAULT_UA)
            .setDefaultRequestProperties(item.headers)
        // DefaultDataSource handles http/https (via httpFactory, with UA + headers)
        // AND file:// / content:// / asset:// so recordings & downloads can play.
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
        val exo = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        val resumeMs = if (item.kind != Kind.LIVE) {
            app.store.historyPosition(item.key)?.first ?: 0L
        } else 0L
        exo.setMediaItem(MediaItem.fromUri(Uri.parse(item.url)), resumeMs)
        exo.prepare()
        exo.playWhenReady = true
        player = exo
        onDispose {
            val dur = exo.duration.takeIf { it > 0 } ?: 0L
            app.store.patchHistoryPosition(item.key, exo.currentPosition, dur)
            exo.release()
            player = null
        }
    }

    DisposableEffect(player) {
        val exo = player
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isP: Boolean) { isPlaying = isP }
            override fun onVideoSizeChanged(v: androidx.media3.common.VideoSize) {
                val h = v.height; quality = if (h > 0) "${h}p" else "AUTO"
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    durationMs = exo?.duration?.takeIf { it > 0 } ?: durationMs
                }
            }
        }
        exo?.addListener(listener)
        onDispose { exo?.removeListener(listener) }
    }

    LaunchedEffect(player) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            if (exo.isPlaying) {
                positionMs = exo.currentPosition.coerceAtLeast(0)
                val d = exo.duration
                if (d > 0) durationMs = d
            }
            app.store.patchHistoryPosition(item.key, exo.currentPosition, exo.duration.takeIf { it > 0 } ?: durationMs)
            delay(5000)
        }
    }

    LaunchedEffect(sleepEndAt) {
        while (sleepEndAt > 0) {
            if (sleepEndAt - System.currentTimeMillis() <= 0) {
                player?.pause()
                sleepEndAt = 0L
                sleepMinutes = 0
                Toasts.show("Sleep timer ended")
                break
            }
            delay(5000)
        }
    }

    BackHandler { onClose() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                when (event.key) {
                    androidx.compose.ui.input.key.Key.MediaPlayPause -> {
                        player?.let { it.playWhenReady = !it.playWhenReady; isPlaying = it.playWhenReady }
                        true
                    }
                    androidx.compose.ui.input.key.Key.MediaFastForward -> {
                        player?.let { it.seekTo((it.currentPosition + 10_000L).coerceAtMost(it.duration.takeIf { d -> d > 0 } ?: Long.MAX_VALUE)) }
                        true
                    }
                    androidx.compose.ui.input.key.Key.MediaRewind -> {
                        player?.let { it.seekTo((it.currentPosition - 10_000L).coerceAtLeast(0)) }
                        true
                    }
                    else -> false
                }
            }
    ) {
        player?.let { exo ->
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = exo; useController = false; keepScreenOn = true; playerViewRef = this } },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (statsOpen) {
            StatsPanel(
                title = item.name,
                group = item.group,
                epg = item.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).first?.title },
                aspect = when (aspectIndex) { 0 -> "Fit"; 1 -> "Zoom"; else -> "Fill" },
                speed = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)[speedIndex],
                latency = 0,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
            )
        }

        if (infoOpen) {
            InfoPanel(
                title = item.name,
                group = item.group,
                url = item.url,
                epg = item.tvgId.takeIf { it.isNotBlank() }?.let { app.epg.nowNext(it).first?.title },
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
            )
        }

        if (isRecording) {
            val transition = rememberInfiniteTransition(label = "recPulse")
            val recDot by transition.animateFloat(
                initialValue = 1f, targetValue = 0.25f,
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
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = recDot)))
                Spacer(Modifier.width(10.dp))
                Text("REC", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Text(item.name, color = Color(0xF2FFFFFF), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                Text(formatRecTime(durationMs - positionMs), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        PlayerTopBar(
            isLive = false,
            latencySeconds = null,
            onBack = onClose
        )

        PlayerBottomDock(
            title = item.name,
            group = item.group,
            logo = item.logo.takeIf { it.isNotBlank() },
            quality = quality,
            isLive = false,
            epgNowTitle = null,
            epgNowRemaining = null,
            epgProgress = null,
            epgNextTitle = null,
            epgNextTime = null,
            seekNowMs = positionMs,
            seekDurationMs = durationMs,
            onSeekFraction = { f ->
                val exo = player
                if (exo != null && durationMs > 0) {
                    exo.seekTo((f * durationMs).toLong())
                }
            },
            isPlaying = isPlaying,
            aspect = when (aspectIndex) { 0 -> "Fit"; 1 -> "Zoom"; else -> "Fill" },
            speed = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)[speedIndex],
            repeatMode = repeatMode,
            locked = locked,
            isFavorite = isFavorite,
            isRecording = isRecording,
            recCount = recCount,
            drawerOpen = false,
            showPrevNext = false,
            onTogglePlay = { player?.let { it.playWhenReady = !it.playWhenReady } },
            onStop = { player?.let { it.pause(); it.seekTo(0) } },
            onPrevChannel = {},
            onNextChannel = {},
            onSeekBack = { if (!locked) player?.let { it.seekTo(maxOf(0, it.currentPosition - 10_000L)) } },
            onSeekForward = { if (!locked) player?.let { it.seekTo(it.currentPosition + 10_000L) } },
            onReload = {
                player?.let { it.seekToDefaultPosition(); it.prepare(); it.playWhenReady = true; Toasts.show("Reloaded") }
            },
            onToggleLock = { locked = !locked; Toasts.show(if (locked) "Controls locked" else "Controls unlocked") },
            onScreenshot = {
                val pv = playerViewRef
                if (pv != null) {
                    // Off the main thread — runBlocking here would freeze the UI / risk ANR.
                    app.scope.launch {
                        val bitmap = kotlinx.coroutines.withTimeoutOrNull(2500) { capturePlayerFrame(pv) }
                        if (bitmap != null) {
                            withContext(Dispatchers.IO) { saveBitmapToPictures(context, bitmap, item.name) }
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
                player?.let { Toasts.show(cycleAudioTrack(it)) } ?: Toasts.show("Audio: not ready")
            },
            onToggleFavorite = {
                app.store.toggleFavorite(
                    FavoriteItem(
                        key = item.key,
                        name = item.name,
                        group = item.group,
                        kind = item.kind.name.lowercase(),
                        url = item.url,
                        logo = item.logo,
                        tvgId = item.tvgId
                    )
                )
            },
            onShowEpg = { onOpenGuide?.invoke() },
            onShowInfo = { infoOpen = !infoOpen; statsOpen = false },
            onToggleStats = { statsOpen = !statsOpen; infoOpen = false },
            onToggleDrawer = {},
            onGoLive = {},
            onRecord = {
                // VOD has no live stream to record — "REC" saves a local copy instead.
                isRecording = true
                recCount += 1
                Toasts.show("Saving ${item.name}…")
                app.scope.launch {
                    val result = app.media.download(item.url, item.name, item.headers)
                    withContext(Dispatchers.Main) {
                        isRecording = false
                        Toasts.show(
                            if (result.isSuccess) "Saved: ${result.getOrNull()?.name}"
                            else "Save failed: ${result.exceptionOrNull()?.message}",
                            isError = result.isFailure
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private fun formatRecTime(ms: Long): String {
    val sec = ms / 1000
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
