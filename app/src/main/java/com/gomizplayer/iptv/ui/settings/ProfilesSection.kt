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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.ui.theme.Accent
import com.gomizplayer.iptv.ui.theme.OnSurfaceMuted

/** Profiles management: switch / create / delete (non-default). Parity with Windows profiles. */
@Composable
internal fun ProfilesSection(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = gomiz(context)
    val data by app.store.data.collectAsState()
    val current = data.settings.profile
    val profiles = data.library.keys.toList().ifEmpty { listOf("default") }

    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp)) {
        SectionHeader(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_title), onBack)
        Text(
            androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_current, current),
            color = Accent,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(profiles.size) { index ->
                val p = profiles[index]
                val active = p == current
                SettingsRow(
                    icon = if (active) "✓" else "○",
                    title = p,
                    subtitle = if (active) androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_active) else null,
                    trailing = if (p != "default") "🗑" else null,
                    onClick = { if (!active) app.store.switchProfile(p) }
                )
                if (p != "default") {
                    Row(Modifier.fillMaxWidth().padding(start = 64.dp)) {
                        TextButton(onClick = { pendingDelete = p }) {
                            Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_delete), color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_new), color = OnSurfaceMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.gomizplayer.iptv.ui.kit.TvTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_new),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        app.store.createProfile(name)
                        app.store.switchProfile(name)
                        newName = ""
                    }
                }
            ) {
                Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_create))
            }
        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_delete)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.profiles_delete_confirm, p)) },
            confirmButton = {
                TextButton(onClick = {
                    app.store.deleteProfile(p)
                    pendingDelete = null
                }) { Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.no))
                }
            }
        )
    }
}
