package com.gomizplayer.iptv.ui.modals

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.gomizplayer.iptv.core.model.FavoriteItem
import com.gomizplayer.iptv.ui.kit.PosterCard
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/** Detail modal matching Windows: poster + facts + 4 actions + recommendations row. */
@Composable
fun DetailModal(
    item: ChannelItem,
    allItems: List<ChannelItem>,
    onPlay: (ChannelItem) -> Unit,
    onDownload: ((ChannelItem) -> Unit)? = null,
    onOpenInBrowser: ((ChannelItem) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val data by app.store.data.collectAsState()
    val isFavorite = data.library[data.settings.profile]?.favorites?.any { it.key == item.key } == true

    val recommendations = remember(item.key, allItems) {
        val kind = item.kind
        allItems.asSequence()
            .filter { it.key != item.key && it.kind == kind }
            .filter { it.group == item.group || it.group.isBlank() }
            .take(12)
            .toList()
            .ifEmpty { allItems.filter { it.kind == kind && it.key != item.key }.take(12) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onClose)
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .width(720.dp)
                .height(460.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.panel)
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.name,
                    color = colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.panel2)
                        .border(1.dp, colors.line, RoundedCornerShape(9.dp))
                        .clickable(onClick = onClose)
                        .padding(7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                // Poster
                Box(
                    Modifier
                        .width(170.dp)
                        .height(255.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x2EFF3D5A), Color(0x0016161F)),
                                center = androidx.compose.ui.geometry.Offset(120f, 80f),
                                radius = 260f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.logo.isNotBlank()) {
                        AsyncImage(
                            model = item.logo,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = item.name.trim().take(2).uppercase(),
                            color = Color(0x40FFFFFF),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.width(18.dp))

                Column(Modifier.weight(1f)) {
                    // Badges row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailBadge(text = item.kind.name, color = colors.accent)
                        if (item.group.isNotBlank()) {
                            DetailBadge(text = item.group, color = colors.muted)
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    // Facts
                    if (item.group.isNotBlank()) {
                        FactRow(label = "Group", value = item.group)
                    }
                    FactRow(label = "Source", value = "Demo Playlist")
                    if (item.tvgId.isNotBlank()) {
                        FactRow(label = "ID", value = item.tvgId)
                    }

                    Spacer(Modifier.height(14.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accentBrush)
                                .clickable { onPlay(item); onClose() }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Play", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.panel2)
                                .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                                .clickable {
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
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_heart),
                                    contentDescription = null,
                                    tint = if (isFavorite) colors.accent else colors.muted,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isFavorite) "In My List" else "Add to My List",
                                    color = if (isFavorite) colors.accent else colors.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (item.kind != Kind.LIVE) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.panel2)
                                    .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                                    .clickable { onDownload?.invoke(item) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_download),
                                        contentDescription = null,
                                        tint = colors.muted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.panel2)
                                .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                                .clickable { onOpenInBrowser?.invoke(item) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_external),
                                    contentDescription = null,
                                    tint = colors.muted,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Open", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Recommendations ----
            if (recommendations.isNotEmpty()) {
                Text(
                    text = "Recommended",
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recommendations, key = { it.key }) { rec ->
                        PosterCard(item = rec, onPlay = { onPlay(rec); onClose() }, cardWidth = 110.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBadge(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    val colors = LocalGomizColors.current
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, color = colors.muted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
        Text(value, color = colors.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
