package com.gomizplayer.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.core.m3u.M3u.ChannelItem
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

@Composable
fun StatsPanel(
    title: String,
    group: String,
    epg: String?,
    aspect: String,
    speed: Float,
    latency: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xE6080A0F))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .padding(16.dp)
            .width(280.dp)
    ) {
        Text("Stats for nerds", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))
        StatRow("Channel", title)
        StatRow("Group", group)
        epg?.let { StatRow("Now", it) }
        StatRow("Aspect", aspect)
        StatRow("Speed", speed.toString() + "×")
        StatRow("Latency", if (latency > 0) "${latency}s" else "—")
    }
}

@Composable
fun InfoPanel(
    title: String,
    group: String,
    url: String,
    epg: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalGomizColors.current
    val host = remember(url) {
        runCatching { android.net.Uri.parse(url).host ?: url }.getOrDefault(url)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xE6080A0F))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .padding(16.dp)
            .width(300.dp)
    ) {
        Text("Channel info", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))
        StatRow("Name", title)
        StatRow("Group", group)
        StatRow("Host", host)
        epg?.let { StatRow("Now", it) }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = Color(0x8AFFFFFF), fontSize = 11.sp, modifier = Modifier.width(70.dp))
        Text(value, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
