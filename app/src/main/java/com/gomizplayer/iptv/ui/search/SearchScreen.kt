package com.gomizplayer.iptv.ui.search

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.kit.EmptyState
import com.gomizplayer.iptv.ui.kit.PosterCard
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/** Global search across channels/movies/series (parity with Windows search page). */
@Composable
fun SearchScreen(query: String, onPlay: (ChannelItem) -> Unit) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val libraryState by app.library.state.collectAsState()
    var selectedKind by remember { mutableStateOf<Kind?>(null) }

    val all = remember(libraryState.items) { libraryState.items.filter { !it.hidden } }
    val visible = remember(all, query, selectedKind) {
        all.asSequence()
            .filter { selectedKind == null || it.kind == selectedKind }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.group.contains(query, ignoreCase = true) }
            .toList()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.search_hint),
                color = colors.dim,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindChip("All", selectedKind == null) { selectedKind = null }
            KindChip("Live", selectedKind == Kind.LIVE) { selectedKind = if (selectedKind == Kind.LIVE) null else Kind.LIVE }
            KindChip("Movies", selectedKind == Kind.MOVIE) { selectedKind = if (selectedKind == Kind.MOVIE) null else Kind.MOVIE }
            KindChip("Series", selectedKind == Kind.SERIES) { selectedKind = if (selectedKind == Kind.SERIES) null else Kind.SERIES }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = visible.size.toString() + " results",
            color = colors.dim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))

        if (visible.isEmpty()) {
            EmptyState(title = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.no_results))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 124.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visible, key = { it.key }) { item -> PosterCard(item = item, onPlay = onPlay) }
            }
        }
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalGomizColors.current
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (selected) colors.accent.copy(alpha = 0.18f) else colors.panel2,
            labelColor = if (selected) colors.text else colors.muted
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) colors.accent.copy(alpha = 0.5f) else colors.line
        )
    )
}
