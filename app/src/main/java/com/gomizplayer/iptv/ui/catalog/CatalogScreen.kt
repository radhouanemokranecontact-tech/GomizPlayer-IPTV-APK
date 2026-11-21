package com.gomizplayer.iptv.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.kit.EmptyState
import com.gomizplayer.iptv.ui.kit.GroupFilterRow
import com.gomizplayer.iptv.ui.kit.LiveChannelRow
import com.gomizplayer.iptv.ui.kit.PosterCard
import com.gomizplayer.iptv.ui.theme.Accent
import com.gomizplayer.iptv.ui.theme.OnSurfaceMuted

enum class CatalogKind { LIVE, MOVIES, SERIES }

@Composable
fun CatalogScreen(kind: CatalogKind, onPlay: (ChannelItem) -> Unit, onDetail: (ChannelItem) -> Unit = {}) {
    val context = LocalContext.current
    val app = gomiz(context)
    val state by app.library.state.collectAsState()
    val data by app.store.data.collectAsState()

    val all = remember(state.items) { state.items.filter { !it.hidden } }
    val filtered = remember(all, kind) {
        when (kind) {
            CatalogKind.LIVE -> all.filter { it.kind == Kind.LIVE || it.kind == Kind.UNKNOWN }
            CatalogKind.MOVIES -> all.filter { it.kind == Kind.MOVIE }
            CatalogKind.SERIES -> all.filter { it.kind == Kind.SERIES }
        }
    }

    var group by remember { mutableStateOf<String?>(null) }

    val groups = remember(filtered) { app.library.countsByGroupFor(filtered) }
    val visible = remember(filtered, group) {
        filtered.filter { group == null || it.group == group }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 12.dp)) {
        GroupFilterRow(
            groups = groups,
            selected = group,
            onSelect = { group = it },
            allLabel = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.filter_all)
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp, color = Accent)
                Spacer(Modifier.width(8.dp))
            }
            val countText = when (kind) {
                CatalogKind.LIVE -> androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.channels_count, visible.size)
                else -> androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.items_count, visible.size)
            }
            Text(countText, color = OnSurfaceMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))

        if (visible.isEmpty()) {
            if (state.loading) {
                // keep empty while loading
            } else {
                EmptyState(
                    title = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.empty_home_title),
                    body = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.empty_home_body)
                )
            }
            return@Column
        }

        when (kind) {
            CatalogKind.LIVE -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.key }) { item -> LiveChannelRow(item, onPlay) }
            }
            CatalogKind.MOVIES, CatalogKind.SERIES -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                // 6 posters per row on TV — 8 felt too dense.
                val columns = 6
                val cellWidth = (maxWidth.value / columns.toFloat()).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(visible, key = { it.key }) { item ->
                        PosterCard(item = item, onPlay = onPlay, onDetail = onDetail, modifier = Modifier.fillMaxWidth(), cardWidth = cellWidth)
                    }
                }
            }
        }
    }
}
