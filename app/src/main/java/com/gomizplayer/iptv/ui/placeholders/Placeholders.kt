package com.gomizplayer.iptv.ui.placeholders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

@Composable
fun DownloadsPlaceholder(onOpenSources: () -> Unit) {
    PlaceholderScreen(
        icon = R.drawable.ic_download,
        title = "Downloads",
        body = "Live stream and VOD downloads, pause/resume, destination folder — tracked per download with progress.",
        hint = "Downloads arrive in Phase 2 (ffmpeg-kit is already in the native build plan).",
        actionLabel = "Add a source first",
        onAction = onOpenSources
    )
}

@Composable
fun RecordingsPlaceholder(onOpenSources: () -> Unit) {
    PlaceholderScreen(
        icon = R.drawable.ic_record,
        title = "Recordings",
        body = "Record any live channel with one tap, keep track of recordings with progress and expiry, play or delete with a confirmation.",
        hint = "Recordings arrive in Phase 2 (ffmpeg-kit recording engine).",
        actionLabel = "Add a source first",
        onAction = onOpenSources
    )
}

@Composable
private fun PlaceholderScreen(
    icon: Int,
    title: String,
    body: String,
    hint: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalGomizColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(96.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(body, color = colors.muted, fontSize = 14.sp, maxLines = 3)
        Spacer(Modifier.height(8.dp))
        Text(hint, color = colors.dim, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onAction) { Text("→  " + actionLabel, color = colors.accent, fontWeight = FontWeight.Bold) }
    }
}
