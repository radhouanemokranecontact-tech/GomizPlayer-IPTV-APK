package com.gomizplayer.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.ui.kit.tvFocusRing
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import kotlin.math.max

private fun fmtTime(ms: Long): String {
    val t = max(0, ms) / 1000
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

/** ---------- Top bar: back + LIVE pill + latency + right widget strip ---------- */
@Composable
fun PlayerTopBar(
    isLive: Boolean,
    latencySeconds: Int?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = gomiz(context)
    val data by app.store.data.collectAsState()
    val colors = LocalGomizColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x99090C12))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                .clickable(onClick = onBack)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // LIVE pill + latency (live only)
        if (isLive) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFFF3D5A))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Color.White))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.live_pill),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
            latencySeconds?.let { lat ->
                if (lat >= 1) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0x66090C12))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.latency_behind, lat),
                            color = Color(0xCCFFFFFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Widget strip: clock | weather | prayer
        if (data.settings.widgetClock || data.settings.widgetWeather || data.settings.widgetPrayer) {
            val widgetState by app.widget.state.collectAsState()
            val nowMs = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
            val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val mm = cal.get(java.util.Calendar.MINUTE)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x66090C12))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (data.settings.widgetClock) {
                    WidgetChip(icon = R.drawable.ic_clock, text = String.format("%02d:%02d", hh, mm))
                }
                if (data.settings.widgetWeather) {
                    val tempC = widgetState.tempC
                    if (tempC != null) {
                        WidgetChip(
                            icon = R.drawable.ic_weather,
                            text = Math.round(tempC).toString() + "°" + (if (data.settings.tempUnit == "F") "F" else "C")
                        )
                    }
                }
                if (data.settings.widgetPrayer && widgetState.prayers.isNotEmpty()) {
                    val nowMinutes = hh * 60 + mm
                    val next = app.widget.nextPrayer(nowMinutes)
                    if (next != null) {
                        WidgetChip(
                            icon = R.drawable.ic_prayer,
                            text = (next.first.replaceFirstChar { it.uppercase() }) + " " + next.second
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetChip(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color(0xE6FFFFFF),
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = Color(0xE6FFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** ---------- Bottom dock ---------- */
@Composable
fun PlayerBottomDock(
    // identity
    title: String,
    group: String,
    logo: String?,
    quality: String,
    // epg / seek
    isLive: Boolean,
    epgNowTitle: String?,
    epgNowRemaining: String?,
    epgProgress: Float?,
    epgNextTitle: String?,
    epgNextTime: String?,
    seekNowMs: Long,
    seekDurationMs: Long,
    onSeekFraction: (Float) -> Unit,
    // state
    isPlaying: Boolean,
    aspect: String,
    speed: Float,
    repeatMode: Int,
    locked: Boolean,
    isFavorite: Boolean,
    isRecording: Boolean,
    recCount: Int,
    drawerOpen: Boolean,
    showPrevNext: Boolean,
    controlsVisible: Boolean = true,
    onDockFocusChange: ((Boolean) -> Unit)? = null,
    // actions
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onReload: () -> Unit,
    onToggleLock: () -> Unit,
    onScreenshot: () -> Unit,
    onSleepTimer: () -> Unit,
    onToggleRepeat: () -> Unit,
    onCycleAspect: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowEpg: () -> Unit,
    onShowInfo: () -> Unit,
    onToggleStats: () -> Unit,
    onToggleDrawer: () -> Unit,
    onGoLive: () -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    // "Toutes les chaînes" (all channels) is the most used entry point on a TV remote —
    // give it the default focus when the dock shows, instead of play/pause.
    val allChannelsFocus = remember { FocusRequester() }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            runCatching { allChannelsFocus.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xC2080A0F))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
            .onFocusChanged { onDockFocusChange?.invoke(it.hasFocus) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ---- Identity row: logo + title + quality chip + sub ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!logo.isNullOrBlank()) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0x59000000))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = coil3.compose.rememberAsyncImagePainter(logo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Brush.linearGradient(listOf(colors.accent.copy(alpha = 0.42f), Color(0x26FF7A45)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.trim().take(1).uppercase(),
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quality,
                            color = Color(0xE6FFFFFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = buildString {
                        append(group)
                        epgNowTitle?.let { append(" · ").append(it) }
                    },
                    color = Color(0x8AFFFFFF),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- EPG strip (live) or seek row (VOD) ----
        if (isLive) {
            if (epgNowTitle != null) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.live_now_tag),
                            color = colors.accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = epgNowTitle,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        epgNowRemaining?.let {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = it,
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    epgProgress?.let { frac ->
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x26FFFFFF))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(frac)
                                    .height(3.dp)
                                    .background(Brush.horizontalGradient(listOf(colors.accent, colors.accent2)))
                            )
                        }
                    }
                    epgNextTitle?.let { next ->
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.live_next_tag),
                                color = Color(0x59FFFFFF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = next,
                                color = Color(0x8AFFFFFF),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            epgNextTime?.let {
                                Text(
                                    text = it,
                                    color = Color(0x66FFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                // ---- Fallback live strip (no EPG guide available) ----
                // Always show NOW / NEXT + a time-position line so the dock stays informative.
                val nowMs = System.currentTimeMillis()
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
                val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val mm = cal.get(java.util.Calendar.MINUTE)
                val timeLabel = String.format(java.util.Locale.getDefault(), "%02d:%02d", hh, mm)
                val minuteFrac = mm / 60f
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.live_now_tag),
                            color = colors.accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.epg_fallback_now),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = timeLabel,
                            color = Color(0xCCFFFFFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x26FFFFFF))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(minuteFrac)
                                .height(3.dp)
                                .background(Brush.horizontalGradient(listOf(colors.accent, colors.accent2)))
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.live_next_tag),
                            color = Color(0x59FFFFFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.epg_fallback_next),
                            color = Color(0x8AFFFFFF),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            // ---- Seek row (VOD) ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fmtTime(seekNowMs),
                    color = Color(0xCCFFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(10.dp))
                SeekBar(
                    fraction = if (seekDurationMs > 0) (seekNowMs.toFloat() / seekDurationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                    onSeekFraction = onSeekFraction,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = fmtTime(seekDurationMs),
                    color = Color(0xCCFFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ---- Control row: drawer pill + transport + tools ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showPrevNext) {
                // "All channels" pill (drawer toggle) — accent when open, visible focus ring when D-pad focused
                var pillFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (drawerOpen) colors.accent.copy(alpha = 0.85f) else if (pillFocused) colors.accent.copy(alpha = 0.35f) else Color(0x1AFFFFFF))
                        .border(1.dp, if (drawerOpen || pillFocused) colors.accent else Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                        .focusRequester(allChannelsFocus)
                        .onFocusChanged { pillFocused = it.isFocused }
                        .clickable(onClick = onToggleDrawer)
                        .focusable()
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.all_channels),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(10.dp))
            }

            // Transport
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                DockIconButton(R.drawable.ic_back10, "−10s", onSeekBack)
                if (showPrevNext) DockIconButton(R.drawable.ic_prev, "Prev", onPrevChannel)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.accentBrush)
                        .clickable(onClick = onTogglePlay)
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isPlaying) 26.dp else 24.dp)
                    )
                }
                DockIconButton(R.drawable.ic_stop, "Stop", onStop)
                if (showPrevNext) DockIconButton(R.drawable.ic_next, "Next", onNextChannel)
                DockIconButton(R.drawable.ic_forward10, "+10s", onSeekForward)
                if (isLive) DockIconButton(R.drawable.ic_golive, "Go live", onGoLive)
                // Record button with badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRecording) colors.accent.copy(alpha = 0.28f) else Color(0x1AFFFFFF))
                        .clickable(onClick = onRecord)
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_record),
                        contentDescription = null,
                        tint = if (isRecording) colors.accent else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    if (recCount > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(15.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(recCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Tools (scrollable if space is tight)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                item { DockIconButton(R.drawable.ic_reload, "Reload", onReload) }
                item { DockIconButton(R.drawable.ic_headphones, "Audio track", onCycleAudio) }
                item { DockIconButton(if (locked) R.drawable.ic_lock else R.drawable.ic_unlock, "Lock", onToggleLock, active = locked) }
                item { DockIconButton(R.drawable.ic_screenshot, "Screenshot", onScreenshot) }
                item { DockIconButton(R.drawable.ic_timer, "Sleep timer", onSleepTimer) }
                item { DockIconButton(if (repeatMode == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat, "Repeat", onToggleRepeat, active = repeatMode > 0) }
                item { DockTextButton(aspect, "Aspect ratio", onCycleAspect) }
                item { DockTextButton(((speed * 100).toInt() / 100f).toString().trimEnd('0', '.') + "×", "Speed", onCycleSpeed) }
                item { DockIconButton(R.drawable.ic_heart, "Favorite", onToggleFavorite, active = isFavorite) }
                item { DockIconButton(R.drawable.ic_guide, "EPG guide", onShowEpg) }
                item { DockIconButton(R.drawable.ic_info, "Quick info", onShowInfo) }
                item { DockIconButton(R.drawable.ic_stats, "Stats", onToggleStats) }
            }
        }
    }
}

@Composable
private fun DockIconButton(icon: Int, label: String, onClick: () -> Unit, active: Boolean = false) {
    val colors = LocalGomizColors.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) colors.accent.copy(alpha = 0.3f) else Color(0x1AFFFFFF))
            .tvFocusRing(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (active) Color.White else Color(0xE6FFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DockTextButton(text: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .tvFocusRing(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SeekBar(
    fraction: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    var widthPx by remember { androidx.compose.runtime.mutableStateOf(0f) }
    Box(
        modifier = modifier
            .height(22.dp)
            .onGloballyPositioned { widthPx = it.size.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeekFraction((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onSeekFraction((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDrag = { change, _ -> onSeekFraction((change.position.x / size.width).coerceIn(0f, 1f)) }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x26FFFFFF))
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.accentBrush)
        )
        // knob
        val knobOffset = (widthPx * fraction - 8f).coerceAtLeast(0f)
        Box(
            Modifier
                .padding(start = with(androidx.compose.ui.platform.LocalDensity.current) { knobOffset.toDp() })
                .size(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White)
        )
    }
}
