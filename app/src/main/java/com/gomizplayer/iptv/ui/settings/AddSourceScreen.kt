package com.gomizplayer.iptv.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.core.model.SourceEntry
import com.gomizplayer.iptv.core.secrets.SecretBox
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import kotlinx.coroutines.launch
import java.security.SecureRandom

private fun randomId(): String {
    val bytes = ByteArray(8)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun parseHeaders(text: String): Map<String, String> {
    val out = mutableMapOf<String, String>()
    text.lineSequence().forEach { line ->
        val idx = line.indexOf(':')
        if (idx > 0) out[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
    }
    return out
}

/** Add/edit source — Windows v1.0.2 parity: name, type (m3u/xtream/single), url + import,
 *  Xtream credentials, optional headers. Passwords are Keystore-encrypted. */
@Composable
internal fun AddSourceScreen(onBack: () -> Unit, editSource: SourceEntry? = null) {
    val context = LocalContext.current
    val app = gomiz(context)
    val colors = LocalGomizColors.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editSource?.name ?: "") }
    var kind by remember { mutableStateOf(editSource?.kind ?: "m3u") } // m3u | xtream | single
    var url by remember { mutableStateOf(editSource?.url ?: "") }
    var username by remember { mutableStateOf(editSource?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var headersText by remember {
        mutableStateOf((editSource?.headers ?: emptyMap()).entries.joinToString("\n") { "${it.key}: ${it.value}" })
    }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                val dir = java.io.File(context.filesDir, "imported").apply { mkdirs() }
                val file = java.io.File(dir, "playlist-${System.currentTimeMillis()}.m3u")
                file.writeText(text)
                url = file.toURI().toString()
                if (name.isBlank()) name = file.nameWithoutExtension
                kind = "m3u"
            } catch (e: Exception) {
                message = false to (e.message ?: "Import failed")
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader(context.getString(if (editSource == null) R.string.sources_add else R.string.edit_source), onBack) }

        item {
            com.gomizplayer.iptv.ui.kit.TvTextField(value = name, onValueChange = { name = it }, label = context.getString(R.string.source_name))
        }

        item {
            Text(context.getString(R.string.source_type), color = colors.muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "m3u" to "M3U",
                    "xtream" to "Xtream",
                    "single" to "Single"
                ).forEach { (id, label) ->
                    TextButton(onClick = { kind = id }) {
                        Text(
                            (if (kind == id) "● " else "○ ") + label,
                            color = if (kind == id) colors.accent else colors.text,
                            fontSize = 13.sp,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item {
            com.gomizplayer.iptv.ui.kit.TvTextField(
                value = url,
                onValueChange = { url = it },
                label = context.getString(R.string.source_url),
                placeholder = when (kind) {
                    "xtream" -> "http://host:8080"
                    "single" -> "https://example.com/stream.m3u8"
                    else -> "https://example.com/playlist.m3u"
                }
            )
        }

        item {
            TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                Text("📂  " + context.getString(R.string.import_file), color = colors.accent)
            }
        }

        if (kind == "xtream") {
            item {
                com.gomizplayer.iptv.ui.kit.TvTextField(value = username, onValueChange = { username = it }, label = context.getString(R.string.source_username))
            }
            item {
                com.gomizplayer.iptv.ui.kit.TvTextField(value = password, onValueChange = { password = it }, label = context.getString(R.string.source_password), password = true)
            }
        }

        item {
            OutlinedTextField(
                value = headersText,
                onValueChange = { headersText = it },
                label = { Text(context.getString(R.string.headers) + " (" + context.getString(R.string.optional) + ")") },
                placeholder = { Text("User-Agent: VLC/3.0.21\nReferer: https://example.com") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
                )
            )
        }

        message?.let { (ok, msg) ->
            item { Text(msg, color = if (ok) Color(0xFF7CFFB2) else Color(0xFFFF8A8A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }

        item {
            Row {
                Button(enabled = !saving, onClick = {
                    val trimmedUrl = url.trim()
                    if (trimmedUrl.isBlank()) { message = false to context.getString(R.string.name_required); return@Button }
                    if (kind == "xtream" && editSource == null && (username.isBlank() || password.isBlank())) {
                        message = false to context.getString(R.string.name_required); return@Button
                    }
                    saving = true
                    val finalName = name.trim().ifBlank { trimmedUrl.substringAfter("://").substringBefore('/').ifBlank { "Source" } }
                    val entry = SourceEntry(
                        id = editSource?.id ?: randomId(),
                        name = finalName,
                        url = trimmedUrl,
                        kind = kind,
                        username = if (kind == "xtream") username.trim() else "",
                        passwordEnc = if (kind == "xtream" && password.isNotBlank()) SecretBox.encrypt(password) else (editSource?.passwordEnc ?: ""),
                        headers = parseHeaders(headersText),
                        addedAt = editSource?.addedAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    app.store.upsertSource(entry)
                    scope.launch {
                        val result = app.library.refreshOne(entry)
                        saving = false
                        result.onSuccess { count ->
                            message = true to context.getString(R.string.source_test_ok, count)
                            onBack()
                        }.onFailure { e ->
                            message = false to context.getString(R.string.source_test_fail, e.message ?: "unknown")
                        }
                    }
                }) {
                    Text(if (saving) context.getString(R.string.sources_refreshing) else context.getString(R.string.save))
                }
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onBack) { Text(context.getString(R.string.cancel)) }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}
