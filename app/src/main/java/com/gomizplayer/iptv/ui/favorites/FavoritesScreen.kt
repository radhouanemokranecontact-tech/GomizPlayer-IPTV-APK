package com.gomizplayer.iptv.ui.favorites

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.classify.Kind
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.core.model.FavoriteItem
import com.gomizplayer.iptv.ui.kit.PosterCard
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/** My Movies & Series — literal page structure of the Windows app. */
@Composable
fun FavoritesScreen(onPlay: (ChannelItem) -> Unit, onDetail: (ChannelItem) -> Unit = {}, initialTab: Int = 0) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val data by app.store.data.collectAsState()

    var pendingClear by remember { mutableStateOf(false) }

    val favorites = data.library[data.settings.profile]?.favorites.orEmpty()
    val vodFavorites = favorites.filter { it.kind == "movie" || it.kind == "series" }

    Column(Modifier.fillMaxSize()) {
        // ---- pageHead (matches Windows renderFavorites) ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 14.dp)
        ) {
            Box(Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(colors.accentBrush))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.favorites_vod),
                    color = colors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = vodFavorites.size.toString() + " " + stringResource(R.string.items_count_plain),
                    color = colors.dim,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.weight(1f))
            if (vodFavorites.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, colors.lineStrong, RoundedCornerShape(10.dp))
                        .background(colors.panel2)
                        .clickable { pendingClear = true }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trash),
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.clear),
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (vodFavorites.isEmpty()) {
            // ---- Empty state (matches Windows .empty) ----
            Column(
                modifier = Modifier.fillMaxSize().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.panel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_heart),
                        contentDescription = null,
                        tint = colors.dim,
                        modifier = Modifier.size(46.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.favorites_empty),
                    color = colors.muted,
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        // ---- Poster grid (matches Windows cardsGrid) ----
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 124.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            items(vodFavorites, key = { it.key }) { f ->
                PosterCard(
                    item = ChannelItem(
                        key = f.key,
                        name = f.name,
                        group = f.group,
                        logo = f.logo,
                        tvgId = f.tvgId,
                        tvgName = f.name,
                        url = f.url,
                        kind = when (f.kind) {
                            "series" -> Kind.SERIES
                            "movie" -> Kind.MOVIE
                            else -> Kind.UNKNOWN
                        },
                        headers = emptyMap(),
                        hidden = false
                    ),
                    onPlay = onPlay,
                    onDetail = onDetail
                )
            }
        }
    }

    if (pendingClear) {
        AlertDialog(
            onDismissRequest = { pendingClear = false },
            title = { Text(stringResource(R.string.clear_favorites)) },
            text = { Text(stringResource(R.string.clear_favorites_q)) },
            confirmButton = {
                TextButton(onClick = {
                    app.store.clearFavorites()
                    pendingClear = false
                }) {
                    Text(stringResource(R.string.yes), color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}
