package com.gomizplayer.iptv.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.model.EpgSource
import com.gomizplayer.iptv.ui.theme.OnSurfaceMuted
import kotlinx.coroutines.launch
import java.security.SecureRandom

private fun randomId(): String {
    val bytes = ByteArray(8)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

/** EPG (XMLTV) source management: add / refresh / remove. Parity with Windows epg IPC. */
@Composable
internal fun EpgSection(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = gomiz(context)
    val data by app.store.data.collectAsState()
    val epgState by app.epg.state.collectAsState()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_title), onBack) }
        item { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_body), color = OnSurfaceMuted, fontSize = 13.sp) }

        item {
            Button(
                onClick = { scope.launch { app.epg.refreshAll() } },
                enabled = !epgState.loading
            ) {
                Text(if (epgState.loading) androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.sources_refreshing) else androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_refresh))
            }
        }

        epgState.error?.let { err -> item { Text(err, color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 12.sp) } }

        if (data.settings.epgUrls.isEmpty()) {
            item { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_empty), color = OnSurfaceMuted, fontSize = 14.sp) }
        } else {
            items(data.settings.epgUrls, key = { it.id }) { entry ->
                com.gomizplayer.iptv.ui.settings.SettingsRow(
                    icon = "🗓️",
                    title = entry.name.ifBlank { entry.url },
                    subtitle = entry.url + "\n" + androidx.compose.ui.res.stringResource(
                        com.gomizplayer.iptv.R.string.epg_programmes,
                        epgState.counts[entry.id] ?: 0
                    ),
                    trailing = null,
                    onClick = { /* no-op; removal button below */ }
                )
                TextButton(onClick = {
                    scope.launch {
                        app.store.patchSettings(data.settings.copy(epgUrls = data.settings.epgUrls.filterNot { it.id == entry.id }))
                        app.epg.remove(entry.id)
                    }
                }) {
                    Text("🗑  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_remove), color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 13.sp)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
        item { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_add), color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
        item {
            com.gomizplayer.iptv.ui.kit.TvTextField(
                value = name,
                onValueChange = { name = it },
                label = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_name)
            )
        }
        item {
            com.gomizplayer.iptv.ui.kit.TvTextField(
                value = url,
                onValueChange = { url = it },
                label = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_url),
                placeholder = "https://example.com/xmltv.xml"
            )
        }
        status?.let { s -> item { Text(s, color = OnSurfaceMuted, fontSize = 12.sp) } }
        item {
            Button(onClick = {
                val u = url.trim()
                if (u.isEmpty()) return@Button
                val entry = EpgSource(id = randomId(), url = u, name = name.trim().ifBlank { u })
                app.store.patchSettings(data.settings.copy(epgUrls = data.settings.epgUrls + entry))
                name = ""; url = ""
                status = null
                scope.launch {
                    val result = app.epg.refresh(entry)
                    result.onSuccess { count -> status = count.toString() + " OK" }
                        .onFailure { e -> status = e.message }
                }
            }) {
                Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.epg_add))
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}
