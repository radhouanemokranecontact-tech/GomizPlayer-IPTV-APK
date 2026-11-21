package com.gomizplayer.iptv.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.kit.tvFocusRing
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/** Poster card (2:3 VOD, 16:9 wide/live) with Windows styling. */
@Composable
fun PosterCard(
    item: ChannelItem,
    onPlay: (ChannelItem) -> Unit,
    onDetail: (ChannelItem) -> Unit = {},
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    cardWidth: androidx.compose.ui.unit.Dp = 124.dp
) {
    val colors = LocalGomizColors.current
    val corner = RoundedCornerShape(13.dp)
    val width = if (wide) 232.dp else cardWidth

    Box(
        modifier = modifier
            .size(width = width, height = if (wide) 170.dp else (cardWidth * 1.5f + 44.dp))
            .clip(corner)
            .background(colors.panel)
            .border(1.dp, colors.line, corner)
            .tvFocusRing(corner)
            .clickable(onClick = { onPlay(item) })
            .padding(0.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Poster art area
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (wide) 96.dp else cardWidth * 1.5f)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x1BFF3D5A), Color(0x0016161F)),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(0f, 300f)
                        )
                    )
            ) {
                if (item.logo.isNotBlank()) {
                    AsyncImage(
                        model = item.logo,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (wide) ContentScale.Fit else ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.radialGradient(
                                listOf(Color(0x2EFF3D5A), Color(0x0016161F)),
                                center = androidx.compose.ui.geometry.Offset(120f, 60f),
                                radius = 320f
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.name.trim().take(2).uppercase(),
                            color = Color(0x40FFFFFF),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                // Info button (top-end) — opens detail modal (Windows parity)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x66000000))
                        .clickable(onClick = { onDetail(item) })
                        .focusable()
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = "Details",
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // LIVE badge
                if (item.kind == com.gomizplayer.iptv.core.classify.Kind.LIVE) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xE6FF3D5A))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            // Meta body
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = item.name,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(2.dp))
                if (item.group.isNotBlank()) {
                    Text(
                        text = item.group,
                        color = colors.dim,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Horizontal labeled row of cards with Windows section header. */
@Composable
fun SectionRow(
    title: String,
    items: List<ChannelItem>,
    onPlay: (ChannelItem) -> Unit,
    onDetail: (ChannelItem) -> Unit = {},
    rowLimit: Int = 16,
    subtitle: String? = null,
    cardWidth: androidx.compose.ui.unit.Dp? = null
) {
    if (items.isEmpty()) return
    val colors = LocalGomizColors.current
    Column(Modifier.fillMaxWidth()) {
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
            subtitle?.let {
                Spacer(Modifier.width(10.dp))
                Text(text = it, color = colors.dim, fontSize = 12.sp)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            items(items.take(rowLimit), key = { it.key }) { item ->
                PosterCard(item = item, onPlay = onPlay, onDetail = onDetail, wide = item.kind == com.gomizplayer.iptv.core.classify.Kind.LIVE, cardWidth = cardWidth ?: 124.dp)
            }
        }
    }
}

@Composable
fun LiveChannelRow(
    item: ChannelItem,
    onPlay: (ChannelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(colors.panel)
            .border(1.dp, colors.line, shape)
            .tvFocusRing(shape)
            .clickable(onClick = { onPlay(item) })
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.logo.isNotBlank()) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(colors.panel3),
                    contentAlignment = Alignment.Center
                ) { Text(item.name.trim().take(2).uppercase(), color = colors.muted, fontSize = 15.sp) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.group,
                    color = colors.dim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Continue Watching card with progress bar. */
@Composable
fun ContinueCard(
    item: ChannelItem,
    progress: Float,
    onPlay: (ChannelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    Column(
        modifier = modifier
            .width(158.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colors.panel)
            .border(1.dp, colors.line, RoundedCornerShape(13.dp))
            .clickable(onClick = { onPlay(item) })
    ) {
        Box(
            Modifier.fillMaxWidth().height(158.dp * 1.5f),
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
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            listOf(Color(0x22FF3D5A), Color(0x0016161F)),
                            center = androidx.compose.ui.geometry.Offset(100f, 80f),
                            radius = 260f
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name.trim().take(2).uppercase(),
                        color = Color(0x33FFFFFF),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(3.dp).background(Color(0x26FFFFFF))
        ) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(colors.accentBrush)
            )
        }
        Text(
            text = item.name,
            color = colors.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            lineHeight = 15.sp
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint, color = colors.dim) },
        singleLine = true,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedBorderColor = colors.accent.copy(alpha = 0.5f),
            unfocusedBorderColor = colors.line,
            focusedContainerColor = colors.panel2,
            unfocusedContainerColor = colors.panel2.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(11.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun GroupFilterRow(
    groups: List<Pair<String, Int>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    allLabel: String
) {
    val colors = LocalGomizColors.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(allLabel) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    containerColor = colors.panel2,
                    labelColor = colors.muted,
                    selectedContainerColor = colors.accentSoft,
                    selectedLabelColor = colors.text
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected == null) colors.accent.copy(alpha = 0.5f) else colors.line
                )
            )
        }
        items(groups, key = { it.first }) { (group, count) ->
            FilterChip(
                selected = selected == group,
                onClick = { onSelect(if (selected == group) null else group) },
                label = { Text("$group · $count") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    containerColor = colors.panel2,
                    labelColor = colors.muted,
                    selectedContainerColor = colors.accentSoft,
                    selectedLabelColor = colors.text
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected == group) colors.accent.copy(alpha = 0.5f) else colors.line
                )
            )
        }
    }
}

@Composable
fun EmptyState(title: String, body: String? = null, modifier: Modifier = Modifier) {
    val colors = LocalGomizColors.current
    Column(
        modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        body?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = colors.muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun HintBanner(text: String) {
    val colors = LocalGomizColors.current
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FF3D5A))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = Color(0xFFFFC9D3), fontSize = 12.sp)
    }
}
