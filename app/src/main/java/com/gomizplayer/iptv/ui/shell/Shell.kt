package com.gomizplayer.iptv.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/* =====================================================
   NovaPlay-style shell: sidebar + top bar + view area
   (Windows GomizPlayer IPTV design language, RTL-aware)
   ===================================================== */

@Composable
fun BrandMark(modifier: Modifier = Modifier, boxSize: androidx.compose.ui.unit.Dp = 38.dp, corner: androidx.compose.ui.unit.Dp = 12.dp) {
    val colors = LocalGomizColors.current
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(corner))
            .background(colors.accentBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tv),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(boxSize * 0.58f)
        )
    }
}

data class SidebarItem(val id: String, val icon: Int, val labelRes: Int)

@Composable
private fun SidebarNavItem(
    item: SidebarItem,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalGomizColors.current
    val activeBrush = remember(colors.accent) {
        Brush.linearGradient(
            0.0f to colors.accent.copy(alpha = 0.22f),
            1.0f to colors.accent.copy(alpha = 0.05f)
        )
    }
    var focused by remember { mutableStateOf(false) }
    val backgroundBrush = when {
        focused -> Brush.linearGradient(
            listOf(colors.accent.copy(alpha = 0.30f), colors.accent.copy(alpha = 0.18f))
        )
        selected -> activeBrush
        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(backgroundBrush)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) colors.accent else Color.Transparent,
                shape = RoundedCornerShape(11.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.labelRes),
            tint = if (selected) colors.text else colors.muted,
            modifier = Modifier.size(19.dp)
        )
        if (!compact) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(item.labelRes),
                color = if (selected) colors.text else colors.muted,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Sidebar(
    route: String,
    onRoute: (String) -> Unit,
    onGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val compact = LocalConfiguration.current.screenWidthDp < 640
    val colors = LocalGomizColors.current
    val items = listOf(
        SidebarItem("home", R.drawable.ic_home, R.string.nav_home),
        SidebarItem("live", R.drawable.ic_live, R.string.nav_live),
        SidebarItem("guide", R.drawable.ic_guide, R.string.tv_guide),
        SidebarItem("movies", R.drawable.ic_film, R.string.nav_movies),
        SidebarItem("series", R.drawable.ic_series, R.string.nav_series),
        SidebarItem("favorites", R.drawable.ic_heart, R.string.nav_favorites),
        SidebarItem("myvod", R.drawable.ic_film, R.string.nav_my_vod),
        SidebarItem("downloads", R.drawable.ic_download, R.string.nav_downloads),
        SidebarItem("recordings", R.drawable.ic_record, R.string.nav_recordings),
    )
    val bottomItems = listOf(
        SidebarItem("sources", R.drawable.ic_source, R.string.nav_sources),
        SidebarItem("settings", R.drawable.ic_settings, R.string.nav_settings),
    )

    Column(
        modifier = modifier
            .width(if (compact) 76.dp else 250.dp)
            .fillMaxHeight()
            .background(colors.sidebarBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Brand aligned with the top widget strip height, centered in the sidebar.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(boxSize = if (compact) 34.dp else 38.dp, corner = 12.dp)
            if (!compact) {
                Spacer(Modifier.height(6.dp))
                Text("GomizPlayer IPTV", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(stringResource(R.string.tagline), color = colors.dim, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(if (compact) 12.dp else 14.dp))

        // Center the navigation list vertically in the available space.
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(items, key = { it.id }) { item ->
                SidebarNavItem(
                    item = item,
                    selected = route == item.id,
                    compact = compact,
                    onClick = { if (item.id == "guide") onGuide() else onRoute(item.id) }
                )
            }
            items(bottomItems, key = { it.id }) { item ->
                SidebarNavItem(item = item, selected = route == item.id, compact = compact, onClick = { onRoute(item.id) })
            }
        }
        }
    }
}

@Composable
fun TopBar() {
    val colors = LocalGomizColors.current
    // Top strip now hosts the live widgets (clock, weather, prayer times) — no divider.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(colors.topbarBg, Color.Transparent)))
    ) {
        com.gomizplayer.iptv.ui.home.HomeWidgetsRow(
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 4.dp),
            compact = true
        )
    }
}

/** Full application shell: sidebar + top bar + view area with glow backgrounds. */
@Composable
fun AppShell(
    route: String,
    onRoute: (String) -> Unit,
    onGuide: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = LocalGomizColors.current
    Row(Modifier.fillMaxSize()) {
        Sidebar(route = route, onRoute = onRoute, onGuide = onGuide)
        Column(Modifier.fillMaxSize()) {
            TopBar()
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(colors.bg)
            ) {
                Box(Modifier.fillMaxSize().background(colors.mainBackgroundSecondary))
                Box(Modifier.fillMaxSize().background(colors.mainBackground))
                content()
            }
        }
    }
}
