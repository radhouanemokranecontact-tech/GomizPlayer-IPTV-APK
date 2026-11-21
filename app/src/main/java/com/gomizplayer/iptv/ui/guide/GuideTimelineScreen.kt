package com.gomizplayer.iptv.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.epg.Xmltv
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Horizontal EPG timeline view: channels on the left, programs as time-proportional
 * blocks scrolling horizontally. Click a program to play the channel.
 */
@Composable
fun GuideTimelineScreen(onPlay: (ChannelItem) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current

    val libraryState by app.library.state.collectAsState()
    val channels = remember(libraryState.items) {
        libraryState.items
            .filter { !it.hidden && (it.kind == Kind.LIVE || it.kind == Kind.UNKNOWN) }
            .sortedBy { it.name }
            .take(40)
    }

    // Time window: now -1h .. now +3h
    val now = System.currentTimeMillis()
    val windowStart = now - 60 * 60 * 1000L
    val windowEnd = now + 3 * 60 * 60 * 1000L
    val windowMs = (windowEnd - windowStart).toFloat()
    val pxPerMs = 0.00012f // ~432 px per hour

    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(8.dp))
            Text("TV Guide — Timeline", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("${channels.size} channels", color = colors.dim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        // Time axis
        Row(Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Spacer(Modifier.width(120.dp)) // channel column width
            Box(Modifier.width((windowMs * pxPerMs).dp).height(24.dp)) {
                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                var t = windowStart
                while (t <= windowEnd) {
                    val x = ((t - windowStart) * pxPerMs).dp
                    Text(
                        text = fmt.format(Date(t)),
                        color = colors.muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = x)
                    )
                    t += 30 * 60 * 1000L
                }
            }
        }

        // Channel rows
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(channels, key = { it.key }) { ch ->
                val programmes = remember(ch.tvgId) {
                    if (ch.tvgId.isNotBlank()) {
                        app.epg.programmesBetween(ch.tvgId, windowStart, windowEnd)
                    } else emptyList()
                }
                TimelineRow(ch, programmes, windowStart, windowEnd, windowMs, pxPerMs, now, colors, onPlay)
            }
        }
    }
}

@Composable
private fun TimelineRow(
    ch: ChannelItem,
    programmes: List<Xmltv.Programme>,
    windowStart: Long,
    windowEnd: Long,
    windowMs: Float,
    pxPerMs: Float,
    now: Long,
    colors: com.gomizplayer.iptv.ui.theme.GomizColors,
    onPlay: (ChannelItem) -> Unit
) {
    Row(Modifier.fillMaxWidth().height(44.dp)) {
        // Channel label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(120.dp).fillMaxHeight().padding(end = 6.dp)
        ) {
            if (ch.logo.isNotBlank()) {
                AsyncImage(
                    model = ch.logo,
                    contentDescription = ch.name,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(colors.panel2),
                    contentAlignment = Alignment.Center
                ) { Text(ch.name.take(2).uppercase(), color = colors.muted, fontSize = 9.sp) }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = ch.name,
                color = colors.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Program blocks
        Box(Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState())) {
            Row(Modifier.height(44.dp)) {
                programmes.forEach { p ->
                    val start = (p.start ?: windowStart).coerceAtLeast(windowStart)
                    val stop = (p.stop ?: (start + 30 * 60 * 1000L)).coerceAtMost(windowEnd).coerceAtLeast(start + 1000L)
                    val widthDp = ((stop - start) * pxPerMs).dp.coerceAtLeast(20.dp)
                    val offsetDp = ((start - windowStart) * pxPerMs).dp
                    val isNow = p.start != null && p.stop != null && now in p.start..p.stop
                    Box(
                        modifier = Modifier
                            .padding(start = offsetDp)
                            .width(widthDp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isNow) colors.accent.copy(alpha = 0.35f) else colors.panel)
                            .border(1.dp, if (isNow) colors.accent else colors.line, RoundedCornerShape(6.dp))
                            .clickable { onPlay(ch) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = p.title,
                            color = colors.text,
                            fontSize = 10.sp,
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (programmes.isEmpty()) {
                    Box(
                        Modifier.fillMaxHeight().width(200.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("No EPG data", color = colors.dim, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            // Now indicator
            val nowX = ((now - windowStart) * pxPerMs).dp
            Box(
                Modifier
                    .padding(start = nowX)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.accent)
            )
        }
    }
}
