package com.gomizplayer.iptv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.ui.theme.Accent
import com.gomizplayer.iptv.ui.theme.OnSurfaceMuted
import com.gomizplayer.iptv.ui.theme.SurfaceDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sources list: refresh / delete / add / refresh all. Parity with Windows sources manager. */
@Composable
internal fun SourcesSection(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (com.gomizplayer.iptv.core.model.SourceEntry) -> Unit = {}, entryFocus: FocusRequester? = null) {
    val context = LocalContext.current
    val app = gomiz(context)
    val data by app.store.data.collectAsState()
    val libraryState by app.library.state.collectAsState()
    val scope = rememberCoroutineScope()

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // Focus the first action ("Ajouter une source") as soon as this page opens.
    val firstFocus = entryFocus ?: remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        runCatching { firstFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp)) {
        SectionHeader(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.sources_title), onBack)

        Row(verticalAlignment = Alignment.CenterVertically) {
            var addFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(if (addFocused) com.gomizplayer.iptv.ui.theme.LocalGomizColors.current.accentSoft else com.gomizplayer.iptv.ui.theme.LocalGomizColors.current.panel2)
                    .border(
                        width = if (addFocused) 2.dp else 0.dp,
                        color = if (addFocused) com.gomizplayer.iptv.ui.theme.LocalGomizColors.current.accent else androidx.compose.ui.graphics.Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .focusRequester(firstFocus)
                    .onFocusChanged { addFocused = it.isFocused }
                    .onGloballyPositioned {
                        runCatching { firstFocus.requestFocus() }
                    }
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.sources_add),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            var refreshFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(if (refreshFocused) com.gomizplayer.iptv.ui.theme.LocalGomizColors.current.accentSoft else com.gomizplayer.iptv.ui.theme.LocalGomizColors.current.panel2)
                    .border(
                        width = if (refreshFocused) 2.dp else 0.dp,
                        color = if (refreshFocused) com.gomizplayer.iptv.ui.theme.LocalGomizColors.current.accent else androidx.compose.ui.graphics.Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .onFocusChanged { refreshFocused = it.isFocused }
                    .clickable(enabled = !libraryState.loading, onClick = { scope.launch { app.library.refreshAll() } })
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    if (libraryState.loading) androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.sources_refreshing)
                    else androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.refresh_all),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        libraryState.error?.let { err ->
            Text(err, color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (data.sources.isEmpty()) {
            Spacer(Modifier.height(40.dp))
            Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.sources_empty), color = OnSurfaceMuted, fontSize = 14.sp)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(data.sources, key = { it.id }) { source ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (source.kind == "xtream")
                                androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.source_kind_xtream)
                            else androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.source_kind_m3u),
                            color = Accent,
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(androidx.compose.ui.graphics.Color(0x22FF3D5A)).padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(source.name, color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1)
                            Text(source.url, color = OnSurfaceMuted, fontSize = 10.sp, maxLines = 1)
                            Text(
                                androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.source_items, source.count) +
                                    if (source.updatedAt > 0) "  ·  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.source_updated, dateFmt.format(Date(source.updatedAt))) else "",
                                color = OnSurfaceMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        TextButton(onClick = { onEdit(source) }) {
                            Text("✎  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.edit), fontSize = 13.sp)
                        }
                        TextButton(onClick = { scope.launch { app.library.refreshOne(source) } }) {
                            Text("⟳  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.refresh), fontSize = 13.sp)
                        }
                        TextButton(onClick = { pendingDeleteId = source.id }) {
                            Text("🗑  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.delete), color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        val source = data.sources.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.delete)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.confirm_delete_source, source?.name ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    app.store.removeSource(id)
                    app.library.removeSource(id)
                    pendingDeleteId = null
                }) { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.no))
                }
            }
        )
    }
}
