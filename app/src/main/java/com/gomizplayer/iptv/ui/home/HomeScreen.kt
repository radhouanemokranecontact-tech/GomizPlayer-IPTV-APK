package com.gomizplayer.iptv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.kit.ContinueCard
import com.gomizplayer.iptv.ui.kit.EmptyState
import com.gomizplayer.iptv.ui.kit.SectionRow
import com.gomizplayer.iptv.ui.kit.HintBanner
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onPlay: (ChannelItem) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onDetail: (ChannelItem) -> Unit = {}
) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val state by app.library.state.collectAsState()
    val data by app.store.data.collectAsState()
    val s = data.settings

    val density = if (s.density == "spacious") "large" else s.density
    val densityCardWidth = when (density) {
        "compact" -> 96.dp
        "large" -> 160.dp
        else -> 124.dp
    }
    val sectionOrder = s.homeSectionOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }.let { saved ->
        val ids = listOf("hero", "live", "movies", "series")
        (saved.filter { ids.contains(it) } + ids.filter { !saved.contains(it) }).distinct()
    }.ifEmpty { listOf("hero", "live", "movies", "series") }
    val hiddenSections = s.hiddenSections.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    val all = remember(state.items) { state.items.filter { !it.hidden } }
    val movies = remember(all) { all.filter { it.kind == Kind.MOVIE } }
    val series = remember(all) { all.filter { it.kind == Kind.SERIES } }
    val live = remember(all) { all.filter { it.kind == Kind.LIVE || it.kind == Kind.UNKNOWN } }
    val featured = remember(movies, series, live) {
        val vod = movies + series
        val withArt = vod.filter { it.logo.isNotBlank() }.ifEmpty { vod }.take(5)
        // Fallback: when no VOD source is loaded, feature top live channels so the
        // Hero carousel stays useful instead of disappearing entirely.
        if (withArt.isNotEmpty()) withArt
        else live.filter { it.logo.isNotBlank() }.ifEmpty { live }.take(5)
    }
    val continueWatching = remember(data) {
        data.library[data.settings.profile]?.history.orEmpty()
            .filter { it.durationMs > 0 && it.positionMs > 10_000 && it.positionMs < it.durationMs * 0.95 }
            .take(12)
    }

    LazyColumn(Modifier.fillMaxSize()) {
        // Widgets now live in the top shell bar (no duplicate here).

        // Hero carousel pinned right under widgets — stable position, no layout jump
        // while sources load (featured falls back to live channels when VOD is absent).
        if (featured.isNotEmpty()) {
            item(key = "hero") { HeroCarousel(featured, onPlay) }
        }

        state.error?.let { err -> item { HintBanner(err) } }

        if (state.loading) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.loading_sources), color = colors.muted, fontSize = 12.sp)
                }
            }
        }

        // Continue Watching
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.continue_watching),
                    count = continueWatching.size,
                    onViewAll = null
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    items(continueWatching, key = { it.key }) { h ->
                        ContinueCard(
                            item = ChannelItem(
                                key = h.key,
                                name = h.name,
                                group = h.group,
                                logo = h.logo,
                                tvgId = h.tvgId,
                                tvgName = h.name,
                                url = h.url,
                                kind = when (h.kind) {
                                    "series" -> Kind.SERIES
                                    "movie" -> Kind.MOVIE
                                    else -> Kind.UNKNOWN
                                },
                                headers = emptyMap(),
                                hidden = false
                            ),
                            progress = if (h.durationMs > 0) h.positionMs.toFloat() / h.durationMs.toFloat() else 0f,
                            onPlay = onPlay
                        )
                    }
                }
            }
        }

        if (all.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = stringResource(R.string.empty_home_title),
                    body = stringResource(R.string.empty_home_body),
                    modifier = Modifier.height(360.dp)
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = onOpenSettings) { Text(stringResource(R.string.empty_open_settings)) }
                }
            }
        }

        sectionOrder.forEach { id ->
            if (id in hiddenSections) return@forEach
            when (id) {
                "live" -> if (s.showLiveOnHome && live.isNotEmpty()) item(key = "live") {
                    SectionHeader(
                        title = stringResource(R.string.section_live_tv),
                        count = live.size,
                        onViewAll = { onNavigate("live") }
                    )
                    Spacer(Modifier.height(4.dp))
                    SectionRow(
                        title = "",
                        items = live,
                        onPlay = onPlay,
                        onDetail = onDetail,
                        rowLimit = s.rowLimit,
                        cardWidth = densityCardWidth
                    )
                }
                "movies" -> if (s.showMoviesOnHome && movies.isNotEmpty()) item(key = "movies") {
                    SectionHeader(
                        title = stringResource(R.string.section_movies),
                        count = movies.size,
                        onViewAll = { onNavigate("movies") }
                    )
                    Spacer(Modifier.height(4.dp))
                    SectionRow(
                        title = "",
                        items = movies,
                        onPlay = onPlay,
                        onDetail = onDetail,
                        rowLimit = s.rowLimit,
                        cardWidth = densityCardWidth
                    )
                }
                "series" -> if (s.showSeriesOnHome && series.isNotEmpty()) item(key = "series") {
                    SectionHeader(
                        title = stringResource(R.string.section_series),
                        count = series.size,
                        onViewAll = { onNavigate("series") }
                    )
                    Spacer(Modifier.height(4.dp))
                    SectionRow(
                        title = "",
                        items = series,
                        onPlay = onPlay,
                        onDetail = onDetail,
                        rowLimit = s.rowLimit,
                        cardWidth = densityCardWidth
                    )
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
internal fun SectionHeader(title: String, count: Int, onViewAll: (() -> Unit)?) {
    val colors = LocalGomizColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp)
    ) {
        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(colors.accentBrush))
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = colors.text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = count.toString(),
            color = colors.dim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (onViewAll != null) {
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Text("›", color = colors.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeroCarousel(featured: List<ChannelItem>, onPlay: (ChannelItem) -> Unit) {
    val colors = LocalGomizColors.current
    var index by remember { mutableStateOf(0) }
    val current = featured[index % featured.size]

    LaunchedEffect(featured.size) {
        while (true) {
            delay(6000)
            index = (index + 1) % featured.size
        }
    }

    Box(Modifier.fillMaxWidth().height(320.dp)) {
        if (current.logo.isNotBlank()) {
            AsyncImage(
                model = current.logo,
                contentDescription = current.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.85f
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x66FF3D5A), Color(0x00FF3D5A)),
                        center = Offset(0.8f, 0.15f),
                        radius = 0.9f
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x4D5A6CFF), Color(0x005A6CFF)),
                        center = Offset(0.15f, 1.0f),
                        radius = 0.8f
                    )
                )
            )
            Box(Modifier.fillMaxSize().background(colors.heroFallbackEnd.copy(alpha = 0.35f)))
        }
        // Legibility gradients (RTL-friendly)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color(0xE60A0C11), Color(0x800A0C11), Color(0x140A0C11))
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC080A0F)))
            )
        )

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 26.dp, end = 240.dp)
        ) {
            // Kicker pill
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent.copy(alpha = 0.2f))
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_featured).uppercase(),
                    color = Color(0xFFFFD2D9),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = current.name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = current.group,
                color = Color(0xB3FFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                // Play button (Windows style)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentBrush)
                        .clickable { onPlay(current) }
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.play),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Add to My List (ghost)
                val app = gomiz(LocalContext.current)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x26FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .clickable {
                            app.store.toggleFavorite(
                                com.gomizplayer.iptv.core.model.FavoriteItem(
                                    key = current.key,
                                    name = current.name,
                                    group = current.group,
                                    kind = current.kind.name.lowercase(),
                                    url = current.url,
                                    logo = current.logo,
                                    tvgId = current.tvgId
                                )
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_heart),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_to_list),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Page dots — wide bars like Windows
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            featured.indices.forEach { i ->
                val isOn = i == index % featured.size
                Box(
                    Modifier
                        .size(if (isOn) 40.dp else 26.dp, 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOn) colors.accent else Color(0x40FFFFFF))
                )
            }
        }
    }
}
