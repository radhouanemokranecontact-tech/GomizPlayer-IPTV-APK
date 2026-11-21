package com.gomizplayer.iptv.ui.livereceiver

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.core.model.FavoriteItem
import com.gomizplayer.iptv.ui.kit.SearchField
import com.gomizplayer.iptv.ui.kit.tvFocusRing
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

private const val PAGE_SIZE = 100
private const val PREFS = "gomiz_drawer"

/**
 * Channel drawer (Windows "All channels" drawer, literal port):
 * slide-in from the left, group categories on the left (172dp) + channel list on the right,
 * pagination of 100 with "Load more", pinned "NOW PLAYING" current channel, EPG per row,
 * dedupe toggle, search, favorites heart per row, fully focusable for Android TV D-pad.
 */
@Composable
fun ChannelDrawer(
    items: List<ChannelItem>,
    favoritesMode: Boolean,
    currentKey: String,
    recordingKey: String?,
    onPlayItem: (ChannelItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val prefs = remember { context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE) }

    // ---- restored state (like Windows saveDrawerState / restoreDrawerState) ----
    var search by remember { mutableStateOf(prefs.getString("search", "") ?: "") }
    var groupFilter by remember { mutableStateOf(prefs.getString("group", null)) }
    var dedupe by remember { mutableStateOf(prefs.getBoolean("dedupe", false)) }
    var shown by remember { mutableStateOf(prefs.getInt("page", PAGE_SIZE).coerceAtLeast(PAGE_SIZE)) }

    val data by app.store.data.collectAsState()
    val favKeys = remember(data) {
        data.library[data.settings.profile]?.favorites.orEmpty().map { it.key }.toHashSet()
    }

    // ---- derived data (same logic as Windows renderDrawer) ----
    val base = remember(items, dedupe) {
        if (!dedupe) items
        else {
            val seen = HashSet<String>()
            items.filter { item ->
                val k = item.name.lowercase().trim()
                if (k.isBlank()) true else seen.add(k)
            }
        }
    }
    val counts = remember(base) {
        base.groupBy { it.group.ifBlank { "Uncategorized" } }
            .map { (name, list) -> name to list.size }
            .sortedByDescending { it.second }
    }
    val showGroups = counts.size > 1
    val effectiveGroup = groupFilter?.takeIf { g -> counts.any { it.first == g } }

    // Reset saved group if it no longer exists
    LaunchedEffect(counts) {
        if (groupFilter != null && counts.none { it.first == groupFilter }) groupFilter = null
    }

    val matched = remember(base, search) {
        if (search.isBlank()) base
        else base.filter { it.name.contains(search, ignoreCase = true) || it.group.contains(search, ignoreCase = true) }
    }
    val filtered = remember(matched, effectiveGroup) {
        if (effectiveGroup == null) matched
        else matched.filter { it.group.ifBlank { "Uncategorized" } == effectiveGroup }
    }
    val total = filtered.size
    val shownItems = remember(filtered, shown) { filtered.take(shown) }
    val remaining = total - shownItems.size

    // Move focus into the drawer as soon as it opens — onto the first channel row.
    val drawerFirstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        runCatching { drawerFirstFocus.requestFocus() }
    }

    // Reset page when filtering changes
    LaunchedEffect(search, effectiveGroup, dedupe) {
        shown = PAGE_SIZE
    }

    // Persist the drawer state
    LaunchedEffect(search, effectiveGroup, dedupe, shown) {
        prefs.edit()
            .putString("search", search)
            .putString("group", effectiveGroup)
            .putBoolean("dedupe", dedupe)
            .putInt("page", shown)
            .apply()
    }

    // ---- pinned current channel ("NOW PLAYING"), like Windows ----
    val currentItem = items.firstOrNull { it.key == currentKey }
    val showPinned = currentItem != null && search.isBlank() &&
        (effectiveGroup == null || currentItem.group.ifBlank { "Uncategorized" } == effectiveGroup) &&
        filtered.indexOfFirst { it.key == currentKey }.let { it < 0 || it >= shown }

    val title = stringResource(if (favoritesMode) R.string.nav_favorites else R.string.all_channels)
    val listState = rememberLazyListState()

    // Scroll to the current channel when opening
    LaunchedEffect(currentKey, shownItems) {
        val idx = shownItems.indexOfFirst { it.key == currentKey }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    val accent = colors.accent
    val drawerWidth = if (showGroups) 560.dp else 360.dp

    Row(
        modifier = modifier
            .width(drawerWidth)
            .background(colors.panel.copy(alpha = 0.97f))
            .border(1.dp, colors.line)
    ) {
        // ========= Category pane (172dp) — like Windows =========
        if (showGroups) {
            Column(
                modifier = Modifier
                    .width(172.dp)
                    .fillMaxHeight()
                    .border(1.dp, colors.line)
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.categories).uppercase(),
                    color = colors.dim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item(key = "__all__") {
                        GroupRow(
                            name = stringResource(if (favoritesMode) R.string.nav_favorites else R.string.all_channels),
                            count = base.size,
                            selected = effectiveGroup == null,
                            onClick = { groupFilter = null }
                        )
                    }
                    items(counts, key = { it.first }) { (name, count) ->
                        GroupRow(
                            name = name,
                            count = count,
                            selected = effectiveGroup == name,
                            onClick = { groupFilter = if (effectiveGroup == name) null else name }
                        )
                    }
                }
            }
        }

        // ========= Channel list pane =========
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            // ---- Header ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${shownItems.size} / $total",
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.panel2)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            SearchField(
                value = search,
                onValueChange = { value -> search = value },
                hint = stringResource(R.string.search_channels),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // ---- Pinned NOW PLAYING ----
            if (showPinned && currentItem != null) {
                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Box(
                            Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.now_playing).uppercase(),
                            color = colors.muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                    ChannelRow(
                        item = currentItem,
                        isCurrent = true,
                        isFavorite = favKeys.contains(currentItem.key),
                        isRecording = recordingKey == currentItem.key,
                        accent = accent,
                        colors = colors,
                        app = app,
                        onPlay = onPlayItem
                    )
                }
            }

            // ---- Channel list ----
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(shownItems, key = { it.key }) { item ->
                    ChannelRow(
                        item = item,
                        isCurrent = item.key == currentKey,
                        isFavorite = favKeys.contains(item.key),
                        isRecording = recordingKey == item.key,
                        focusRequester = if (item.key == shownItems.firstOrNull()?.key) drawerFirstFocus else null,
                        accent = accent,
                        colors = colors,
                        app = app,
                        onPlay = onPlayItem
                    )
                }

                if (shownItems.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_results),
                            color = colors.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // ---- Load more (pagination) ----
            if (remaining > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.panel2)
                        .border(1.dp, colors.line, RoundedCornerShape(9.dp))
                        .tvFocusRing()
                        .clickable { shown += PAGE_SIZE }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.load_more) + " (" + remaining + ")",
                        color = colors.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    name: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalGomizColors.current
    val accent = colors.accent
    val bg = animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(140),
        label = "groupBg"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg.value)
            .tvFocusRing()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(
            text = name,
            color = if (selected) colors.text else colors.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            color = if (selected) colors.text else colors.dim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChannelRow(
    item: ChannelItem,
    isCurrent: Boolean,
    isFavorite: Boolean,
    isRecording: Boolean,
    accent: Color,
    colors: com.gomizplayer.iptv.ui.theme.GomizColors,
    app: com.gomizplayer.iptv.GomizApp,
    onPlay: (ChannelItem) -> Unit,
    focusRequester: FocusRequester? = null
) {
    val logoShape = RoundedCornerShape(6.dp)
    val bg = animateColorAsState(
        targetValue = if (isCurrent) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(140),
        label = "rowBg"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(bg.value)
            .then(if (isRecording) Modifier.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(9.dp)) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusRing()
            .clickable { onPlay(item) }
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        if (item.logo.isNotBlank()) {
            AsyncImage(
                model = item.logo,
                contentDescription = item.name,
                modifier = Modifier.size(width = 48.dp, height = 34.dp).clip(logoShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Box(
                Modifier.size(width = 48.dp, height = 34.dp).clip(logoShape).background(colors.panel2),
                contentAlignment = Alignment.Center
            ) { Text(item.name.take(2).uppercase(), color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    color = colors.text,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isRecording) {
                    Text(
                        text = "REC",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Text(
                text = item.group.ifBlank { "Live" },
                color = colors.dim,
                fontSize = 9.sp,
                maxLines = 1
            )
            item.tvgId.takeIf { it.isNotBlank() }?.let { id ->
                val epgNow = remember(id) {
                    app.epg.nowNext(id).first?.title
                }
                epgNow?.let { now ->
                    Text(
                        text = stringResource(R.string.live_now_tag) + ": " + now,
                        color = colors.muted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .tvFocusRing()
                .clickable {
                    app.store.toggleFavorite(
                        FavoriteItem(
                            key = item.key,
                            name = item.name,
                            group = item.group,
                            kind = "live",
                            url = item.url,
                            logo = item.logo,
                            tvgId = item.tvgId
                        )
                    )
                }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_heart),
                contentDescription = null,
                tint = if (isFavorite) accent else colors.dim,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
