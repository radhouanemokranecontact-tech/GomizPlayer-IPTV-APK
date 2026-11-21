package com.gomizplayer.iptv.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import coil3.compose.AsyncImage
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/** TV Guide: live channels with EPG Now/Next + progress. */
@Composable
fun GuideScreen(
    onPlay: (ChannelItem) -> Unit,
    onBack: () -> Unit,
    onTimeline: () -> Unit = {},
    focusKey: String? = null
) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val libraryState by app.library.state.collectAsState()

    var groupFilter by remember { mutableStateOf<String?>(null) }
    val channels = remember(libraryState.items) {
        libraryState.items
            .filter { !it.hidden && (it.kind == Kind.LIVE || it.kind == Kind.UNKNOWN) }
            .sortedWith(compareByDescending<ChannelItem> { it.logo.isNotBlank() }.thenBy { it.name })
    }
    val groups = remember(channels) {
        channels.groupBy { it.group.ifBlank { "Uncategorized" } }
            .map { (name, list) -> name to list.size }
            .sortedByDescending { it.second }
    }
    // Smart open: jump to the channel that is currently playing / was last watched.
    val focusItem = remember(channels, focusKey) {
        focusKey?.let { key -> channels.firstOrNull { it.key == key } }
    }
    LaunchedEffect(focusItem) {
        if (focusItem != null) {
            groupFilter = focusItem.group.ifBlank { "Uncategorized" }
        }
    }
    val visible = remember(channels, groupFilter) {
        channels.filter {
            groupFilter == null || it.group.ifBlank { "Uncategorized" } == groupFilter
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(focusItem, visible) {
        if (focusItem != null) {
            val idx = visible.indexOfFirst { it.key == focusItem.key }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("←  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.back)) }
            Spacer(Modifier.width(8.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.tv_guide),
                color = colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onTimeline) { Text("Timeline →", color = colors.accent, fontWeight = FontWeight.Bold) }
            Text(
                text = visible.size.toString() + " channels",
                color = colors.dim,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(10.dp))
        // Category filter — jump straight to a group instead of scrolling thousands of channels.
        com.gomizplayer.iptv.ui.kit.GroupFilterRow(
            groups = groups,
            selected = groupFilter,
            onSelect = { groupFilter = it },
            allLabel = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.filter_all)
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visible, key = { it.key }) { item ->
                val epg = remember(item.tvgId) {
                    if (item.tvgId.isNotBlank()) app.epg.nowNext(item.tvgId) else null to null
                }
                GuideRow(item, epg.first, epg.second, colors, onPlay)
            }
        }
    }
}

@Composable
private fun GuideRow(
    item: ChannelItem,
    now: com.gomizplayer.iptv.core.epg.Xmltv.Programme?,
    next: com.gomizplayer.iptv.core.epg.Xmltv.Programme?,
    colors: com.gomizplayer.iptv.ui.theme.GomizColors,
    onPlay: (ChannelItem) -> Unit
) {
    val progress = remember(now) {
        val p = now
        if (p != null && p.start != null && p.stop != null && p.stop > p.start) {
            ((System.currentTimeMillis() - p.start).toFloat() / (p.stop - p.start).toFloat()).coerceIn(0f, 1f)
        } else null
    }

    Card(
        onClick = { onPlay(item) },
        modifier = Modifier.fillMaxWidth().height(86.dp),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        colors = CardDefaults.colors(containerColor = colors.panel)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            if (item.logo.isNotBlank()) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(colors.panel3),
                    contentAlignment = Alignment.Center
                ) { Text(item.name.take(2).uppercase(), color = colors.muted, fontSize = 15.sp) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (now != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NOW",
                            color = colors.accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = now.title,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    progress?.let { frac ->
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x26FFFFFF))
                        ) {
                            Box(
                                Modifier.fillMaxWidth(frac).height(3.dp).background(colors.accentBrush)
                            )
                        }
                    }
                    if (next != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "NEXT · " + next.title,
                            color = colors.muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(text = item.group, color = colors.muted, fontSize = 11.sp, maxLines = 1)
                }
            }
            Text("▶", color = colors.accent, fontSize = 20.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
}
