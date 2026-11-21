package com.gomizplayer.iptv.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.ui.modals.ConfirmDialog
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import java.io.File

/** Downloads screen matching Windows: pageHead + list rows + progress + actions. */
@Composable
fun DownloadsScreen(onOpenSources: () -> Unit, onPlay: (java.io.File) -> Unit = {}) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    var files by remember { mutableStateOf(app.media.listDownloads()) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 16.dp)) {
        // pageHead
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Box(Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(colors.accentBrush))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Downloads", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("${files.size} items", color = colors.dim, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
        }

        if (files.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = null,
                    tint = colors.dim,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("No downloads yet", color = colors.muted, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Download a movie or series from the detail view.", color = colors.dim, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onOpenSources) {
                    Text("Add a source first →", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(files, key = { it.absolutePath }) { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.panel)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${file.length() / 1024} KB · ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))}",
                            color = colors.dim,
                            fontSize = 10.sp
                        )
                    }
                    TextButton(onClick = { onPlay(file) }) {
                        Text("Play", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { pendingDelete = file }) {
                        Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    pendingDelete?.let { file ->
        ConfirmDialog(
            title = "Delete download",
            message = "Delete ${file.name}?",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                file.delete()
                files = app.media.listDownloads()
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
