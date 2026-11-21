package com.gomizplayer.iptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import com.gomizplayer.iptv.ui.theme.parseAccent
import com.gomizplayer.iptv.ui.kit.tvFocusRing
import com.gomizplayer.iptv.ui.kit.rememberFocusRequester
import com.gomizplayer.iptv.ui.kit.TvFocusMemory
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.LaunchedEffect
import com.gomizplayer.iptv.ui.toast.Toasts
import kotlinx.coroutines.launch


/** Per-panel override: focuses a specific toggle when its panel opens. */
private val LocalFocusFirst = compositionLocalOf<androidx.compose.ui.focus.FocusRequester?> { null }

private val settingsTabs = listOf(
    "general" to "General",
    "widgets" to "Widgets",
    "cleanup" to "Cleanup",
    "system" to "System",
    "about" to "About"
)

@Composable
fun SettingsScreen(initialSection: String? = null) {
    val context = LocalContext.current
    val app = gomiz(context)
    val activity = LocalActivity.current
    val colors = LocalGomizColors.current
    val data by app.store.data.collectAsState()

    var tab by remember {
        mutableStateOf(
            when (initialSection?.lowercase()) {
                "profiles" -> "general"
                "sources" -> "sources"
                "add_source" -> "sources"
                "home" -> "general"
                "player" -> "general"
                "subtitles" -> "general"
                "storage" -> "general"
                "epg" -> "epg"
                "language" -> "general"
                "about" -> "about"
                else -> "general"
            }
        )
    }

    // Focus always starts on the first tab (General) when Settings opens.
    val firstTabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstTabFocus.requestFocus() } }

    Row(Modifier.fillMaxSize()) {
        // ---- Left vertical tab menu (Windows style) ----
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(colors.panel)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            settingsTabs.forEachIndexed { index, (id, label) ->
                val selected = tab == id
                val focusRequester = if (index == 0) firstTabFocus else rememberFocusRequester("settings", id)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (selected) colors.accent.copy(alpha = 0.16f) else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (selected) colors.accent.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .focusRequester(focusRequester)
                        .focusable()
                        .onFocusChanged { if (it.isFocused) TvFocusMemory.save("settings", id) }
                        .tvFocusRing(RoundedCornerShape(9.dp))
                        .clickable { tab = id }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) colors.accent else colors.muted,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // ---- Right panel ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(colors.bg)
                .padding(24.dp)
        ) {
            when (tab) {
                "general" -> GeneralPanel(app, data, activity)
                "profiles" -> ProfilesPanel(app, data)
                "sources" -> SourcesPanel(app, data)
                "home" -> HomePanel(app, data)
                "player" -> PlayerPanel(app, data)
                "subtitles" -> SubtitlesPanel(app, data)
                "epg" -> EpgSection(onBack = { tab = "general" })
                "widgets" -> WidgetsPanel(app, data)
                "cleanup" -> CleanupPanel(app, data)
                "storage" -> StoragePanel(app, data)
                "system" -> SystemPanel(app, data, activity) { tab = it }
                "about" -> AboutPanel(app, data)
            }
        }
    }
}

// ===================== General =====================

@Composable
    private fun GeneralPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData, activity: android.app.Activity?) {
    val colors = LocalGomizColors.current
    val s = data.settings

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("General", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Language and theme.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("Language")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("en" to "English", "fr" to "Français", "ar" to "العربية").forEach { (code, label) ->
                SettingsChip(
                    label = label,
                    selected = s.language == code,
                    onClick = {
                        app.store.patchSettings(s.copy(language = code))
                        activity?.recreate()
                    }
                )
            }
        }

        SettingsSectionTitle("Theme")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(label = "Dark", selected = s.theme == "dark", onClick = { app.store.patchSettings(s.copy(theme = "dark")); activity?.recreate() })
            SettingsChip(label = "Light", selected = s.theme == "light", onClick = { app.store.patchSettings(s.copy(theme = "light")); activity?.recreate() })
        }
        Spacer(Modifier.height(48.dp))
    }
}

// ===================== Shared settings components =====================

@Composable
private fun SettingsSectionTitle(text: String) {
    val colors = LocalGomizColors.current
    Spacer(Modifier.height(16.dp))
    Text(text = text, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalGomizColors.current
    val focusFirst = LocalFocusFirst.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accentSoft else colors.panel2)
            .border(1.dp, if (selected) colors.accent else colors.line, RoundedCornerShape(8.dp))
            .then(if (focusFirst != null) Modifier.focusRequester(focusFirst) else Modifier)
            .focusable()
            .tvFocusRing(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) colors.accent else colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalGomizColors.current
    val focusFirst = LocalFocusFirst.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.panel2)
            .tvFocusRing(RoundedCornerShape(8.dp))
            .then(if (focusFirst != null) Modifier.focusRequester(focusFirst) else Modifier)
            .focusable()
            .clickable { onChange(!value) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(if (value) R.drawable.ic_check else R.drawable.ic_close),
            contentDescription = null,
            tint = if (value) colors.accent else colors.muted,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (value) colors.text else colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ===================== Home =====================

@Composable
    private fun HomePanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val s = data.settings
    val context = LocalContext.current
    val sections = listOf("hero" to "Featured", "live" to "Live TV", "movies" to "Movies", "series" to "Series")

    fun parseOrder(raw: String): List<String> = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.let { saved ->
        val ids = sections.map { it.first }
        val merged = ids.filter { !saved.contains(it) }
        (saved.filter { ids.contains(it) } + merged).distinct()
    }.ifEmpty { sections.map { it.first } }

    fun parseHidden(raw: String): List<String> = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet().let { set -> sections.map { it.first }.filter { set.contains(it) } }

    val order = parseOrder(s.homeSectionOrder)
    val hidden = parseHidden(s.hiddenSections).toMutableSet()

    fun persist(newOrder: List<String>, newHidden: List<String>) {
        app.store.patchSettings(s.copy(homeSectionOrder = newOrder.joinToString(","), hiddenSections = newHidden.joinToString(",")))
    }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("Home", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Layout, density, sections.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Home layout", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.panel2)
                    .clickable {
                        app.store.patchSettings(s.copy(density = "comfortable", rowLimit = 16, homeSectionOrder = "hero,live,movies,series", hiddenSections = ""))
                        Toasts.show("Layout reset")
                    }
                    .focusable()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Reset layout", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))

        SettingsSectionTitle("Density")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val density = if (s.density == "spacious") "large" else s.density
            SettingsChip(label = "Compact", selected = density == "compact", onClick = { app.store.patchSettings(s.copy(density = "compact")) })
            SettingsChip(label = "Comfortable", selected = density == "comfortable", onClick = { app.store.patchSettings(s.copy(density = "comfortable")) })
            SettingsChip(label = "Large", selected = density == "large", onClick = { app.store.patchSettings(s.copy(density = "large")) })
        }

        SettingsSectionTitle("Row limit")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8, 12, 16, 20, 30).forEach { n ->
                SettingsChip(label = n.toString(), selected = s.rowLimit == n, onClick = { app.store.patchSettings(s.copy(rowLimit = n)) })
            }
        }

        SettingsSectionTitle("Sections")
        Text("Reorder with the arrows, toggle visibility.", color = colors.muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))

        order.forEachIndexed { index, id ->
            val label = sections.first { it.first == id }.second
            val visible = !hidden.contains(id)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (visible) colors.panel2 else colors.panel)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(colors.panel).clickable {
                        if (index > 0) {
                            val newOrder = order.toMutableList()
                            val tmp = newOrder[index - 1]; newOrder[index - 1] = newOrder[index]; newOrder[index] = tmp
                            persist(newOrder, hidden.toList())
                        }
                    }.focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(painterResource(R.drawable.ic_up), contentDescription = "Move up", tint = if (index == 0) colors.dim else colors.text, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(colors.panel).clickable {
                        if (index < order.size - 1) {
                            val newOrder = order.toMutableList()
                            val tmp = newOrder[index + 1]; newOrder[index + 1] = newOrder[index]; newOrder[index] = tmp
                            persist(newOrder, hidden.toList())
                        }
                    }.focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(painterResource(R.drawable.ic_down), contentDescription = "Move down", tint = if (index == order.size - 1) colors.dim else colors.text, modifier = Modifier.size(16.dp))
                }
                Text(label, color = if (visible) colors.text else colors.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (visible) colors.accentSoft else colors.panel)
                        .clickable {
                            val newHidden = hidden.toMutableList()
                            if (visible) newHidden.add(id) else newHidden.remove(id)
                            persist(order, newHidden)
                        }
                        .focusable()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(if (visible) "Shown" else "Hidden", color = if (visible) colors.accent else colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(4.dp))
        Text("Sections use rowLimit items and density adjusts card size.", color = colors.dim, fontSize = 10.sp)
    }
}

// ===================== Player =====================

@Composable
    private fun PlayerPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val s = data.settings
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Player", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.panel2)
                    .clickable {
                        app.store.patchSettings(
                            s.copy(
                                aspect = "contain",
                                seekStep = 10,
                                volume = 100,
                                rate = 1f,
                                quality = 0,
                                preferredAudioLang = "",
                                preferredSubtitleLang = "",
                                retryCount = 2,
                                networkTimeout = 30,
                                userAgent = "GomizPlayer/1.0.4 (Android)"
                            )
                        )
                        Toasts.show("Player settings reset")
                    }
                    .focusable()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reset section", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Playback behavior, aspect, network.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("Aspect ratio")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(label = "Fit", selected = s.aspect == "contain", onClick = { app.store.patchSettings(s.copy(aspect = "contain")) })
            SettingsChip(label = "Fill", selected = s.aspect == "cover", onClick = { app.store.patchSettings(s.copy(aspect = "cover")) })
            SettingsChip(label = "Stretch", selected = s.aspect == "fill", onClick = { app.store.patchSettings(s.copy(aspect = "fill")) })
        }

        SettingsSectionTitle("Volume: ${s.volume}%")
        androidx.compose.material3.Slider(
            value = s.volume.toFloat(),
            onValueChange = { app.store.patchSettings(s.copy(volume = it.toInt())) },
            valueRange = 0f..100f,
            modifier = Modifier.width(320.dp)
        )

        SettingsSectionTitle("Playback speed")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("0.5" to 0.5f, "0.75" to 0.75f, "1x" to 1f, "1.25" to 1.25f, "1.5" to 1.5f, "2x" to 2f).forEach { (label, r) ->
                SettingsChip(label = label, selected = s.rate == r, onClick = { app.store.patchSettings(s.copy(rate = r)) })
            }
        }

        SettingsSectionTitle("Seek step")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(label = "5s", selected = s.seekStep == 5, onClick = { app.store.patchSettings(s.copy(seekStep = 5)) })
            SettingsChip(label = "10s", selected = s.seekStep == 10, onClick = { app.store.patchSettings(s.copy(seekStep = 10)) })
            SettingsChip(label = "15s", selected = s.seekStep == 15, onClick = { app.store.patchSettings(s.copy(seekStep = 15)) })
            SettingsChip(label = "30s", selected = s.seekStep == 30, onClick = { app.store.patchSettings(s.copy(seekStep = 30)) })
            SettingsChip(label = "60s", selected = s.seekStep == 60, onClick = { app.store.patchSettings(s.copy(seekStep = 60)) })
        }

        SettingsSectionTitle("Default quality")
        FlowRowWrap {
            listOf(0 to "Auto", 2160 to "2160p", 1080 to "1080p", 720 to "720p", 480 to "480p", 360 to "360p").forEach { (q, label) ->
                SettingsChip(label = label, selected = s.quality == q, onClick = { app.store.patchSettings(s.copy(quality = q)) })
            }
        }

        SettingsSectionTitle("Preferred audio language")
        OutlinedTextField(
            value = s.preferredAudioLang,
            onValueChange = { app.store.patchSettings(s.copy(preferredAudioLang = it)) },
            label = { Text("Languages (comma separated)") },
            placeholder = { Text("ar, en") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
            )
        )

        SettingsSectionTitle("Preferred subtitle language")
        OutlinedTextField(
            value = s.preferredSubtitleLang,
            onValueChange = { app.store.patchSettings(s.copy(preferredSubtitleLang = it)) },
            label = { Text("Languages (comma separated)") },
            placeholder = { Text("ar, en") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
            )
        )

        SettingsSectionTitle("Network playback")
        SettingsSectionTitle("Retry count")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("0" to 0, "1" to 1, "2" to 2, "3" to 3, "5" to 5).forEach { (label, n) ->
                SettingsChip(label = label, selected = s.retryCount == n, onClick = { app.store.patchSettings(s.copy(retryCount = n)) })
            }
        }

        SettingsSectionTitle("Timeout (seconds)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("10" to 10, "20" to 20, "30" to 30, "45" to 45, "60" to 60, "120" to 120).forEach { (label, t) ->
                SettingsChip(label = label, selected = s.networkTimeout == t, onClick = { app.store.patchSettings(s.copy(networkTimeout = t)) })
            }
        }

        SettingsSectionTitle("User agent")
        OutlinedTextField(
            value = s.userAgent,
            onValueChange = { app.store.patchSettings(s.copy(userAgent = it)) },
            label = { Text("User agent (optional)") },
            placeholder = { Text("GomizPlayer/1.0.4 (Android)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
            )
        )

        SettingsSectionTitle("Proxy mode")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(label = "System", selected = s.proxyMode == "system", onClick = { app.store.patchSettings(s.copy(proxyMode = "system")) })
            SettingsChip(label = "Direct", selected = s.proxyMode == "direct", onClick = { app.store.patchSettings(s.copy(proxyMode = "direct")) })
            SettingsChip(label = "Fixed", selected = s.proxyMode == "fixed", onClick = { app.store.patchSettings(s.copy(proxyMode = "fixed")) })
        }
        if (s.proxyMode == "fixed") {
            OutlinedTextField(
                value = s.proxyUrl,
                onValueChange = { app.store.patchSettings(s.copy(proxyUrl = it)) },
                label = { Text("Proxy URL") },
                placeholder = { Text("http://127.0.0.1:8080") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
                )
            )
        }
    }
}

// ===================== Subtitles =====================

@Composable
    private fun SubtitlesPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val s = data.settings

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Subtitles", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.panel2)
                    .clickable {
                        app.store.patchSettings(
                            s.copy(subtitleScale = 100, subtitleColor = "#FFFFFF", subtitleOpacity = 55, subtitleBottom = 8, subtitleDelay = 0f, subtitleBold = false)
                        )
                        Toasts.show("Subtitle settings reset")
                    }
                    .focusable()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Reset section", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Size, color, opacity, position, delay.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        // --- Preview box (like Windows .subtitles-preview) ---
        val previewHeight = 150.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF101623), Color(0xFF090C12))))
                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                .padding(bottom = (s.subtitleBottom / 100f * 36f).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val bgAlpha = (s.subtitleOpacity / 100f).coerceIn(0f, 1f)
            Text(
                text = if (s.language == "ar") "أهلاً بك في GomizPlayer IPTV" else "Welcome to GomizPlayer IPTV",
                color = parseAccent(s.subtitleColor),
                fontSize = (20f * s.subtitleScale / 100f).sp,
                fontWeight = if (s.subtitleBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF000000).copy(alpha = bgAlpha))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("Size: ${s.subtitleScale}%")
        androidx.compose.material3.Slider(
            value = s.subtitleScale.toFloat(),
            onValueChange = { app.store.patchSettings(s.copy(subtitleScale = it.toInt())) },
            valueRange = 60f..200f,
            steps = 13,
            modifier = Modifier.width(320.dp)
        )

        SettingsSectionTitle("Background opacity: ${s.subtitleOpacity}%")
        androidx.compose.material3.Slider(
            value = s.subtitleOpacity.toFloat(),
            onValueChange = { app.store.patchSettings(s.copy(subtitleOpacity = it.toInt())) },
            valueRange = 0f..100f,
            steps = 9,
            modifier = Modifier.width(320.dp)
        )

        SettingsSectionTitle("Bottom offset: ${s.subtitleBottom}%")
        androidx.compose.material3.Slider(
            value = s.subtitleBottom.toFloat(),
            onValueChange = { app.store.patchSettings(s.copy(subtitleBottom = it.toInt())) },
            valueRange = 2f..35f,
            steps = 32,
            modifier = Modifier.width(320.dp)
        )

        SettingsSectionTitle("Delay: %+.2fs".format(s.subtitleDelay))
        androidx.compose.material3.Slider(
            value = s.subtitleDelay,
            onValueChange = { app.store.patchSettings(s.copy(subtitleDelay = (it * 4).toInt() / 4f)) },
            valueRange = -10f..10f,
            modifier = Modifier.width(320.dp)
        )

        SettingsSectionTitle("Text color")
        FlowRowWrap {
            listOf(
                "#ffffff" to "White", "#ffe66d" to "Yellow", "#7df9ff" to "Cyan",
                "#ff6b6b" to "Red", "#7dff9b" to "Green", "#d0a6ff" to "Violet"
            ).forEach { (c, name) ->
                val selected = s.subtitleColor.equals(c, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parseAccent(c))
                        .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else colors.line, RoundedCornerShape(8.dp))
                        .clickable { app.store.patchSettings(s.copy(subtitleColor = c)) }
                        .focusable()
                )
            }
        }

        SettingsSectionTitle("Style")
        SettingsToggle("Bold", s.subtitleBold) { app.store.patchSettings(s.copy(subtitleBold = it)) }
        Spacer(Modifier.height(48.dp))
    }
}

// ===================== Storage =====================

@Composable
    private fun StoragePanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val s = data.settings
    val context = LocalContext.current

    fun fmtBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    fun dirSize(dir: java.io.File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    val filesDir = context.filesDir
    val mediaDir = java.io.File(filesDir, "media")
    val dataBytes = filesDir.walkTopDown().filter { it.isFile && !it.absolutePath.startsWith(mediaDir.absolutePath) }.sumOf { it.length() }
    val cacheBytes = context.cacheDir?.let { dirSize(it) } ?: 0L
    val downloadsBytes = dirSize(app.media.downloadsDir())
    val recordingsBytes = dirSize(app.media.recordingsDir())

    var pendingClear by remember { mutableStateOf<String?>(null) }
    var pendingReset by remember { mutableStateOf(false) }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val text = input?.bufferedReader()?.readText() ?: ""
                input?.close()
                val result = app.store.restoreFromJson(text)
                if (result.isSuccess) {
                    Toasts.show("Backup imported — restart the app to see changes")
                } else {
                    Toasts.show("Backup import failed: ${result.exceptionOrNull()?.message}", isError = true)
                }
            } catch (e: Exception) {
                Toasts.show("Backup import failed: ${e.message}", isError = true)
            }
        }
    }

    val folderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val take = try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                true
            } catch (_: Exception) { false }
            if (take) Toasts.show("Folder selected") else Toasts.show("Folder selected (access not persisted)")
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("Storage", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Storage usage, folders, backup, cleanup.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("Usage")
        listOf(
            "Data" to fmtBytes(dataBytes),
            "Cache" to fmtBytes(cacheBytes),
            "Downloads" to fmtBytes(downloadsBytes),
            "Recordings" to fmtBytes(recordingsBytes)
        ).forEach { (label, size) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(label, color = colors.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(size, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = filesDir.absolutePath,
            color = colors.dim,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        SettingsSectionTitle("Downloads folder")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("App-private (default)", color = colors.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Button(onClick = { folderLauncher.launch(null) }) { Text("Choose folder") }
        }

        SettingsSectionTitle("Recordings folder")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("App-private (default)", color = colors.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Button(onClick = { folderLauncher.launch(null) }) { Text("Choose folder") }
        }

        SettingsSectionTitle("Backup & Restore")
        Text("Export or import all settings, sources, favorites and history as gomizplayer.json.", color = colors.muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    val f = java.io.File(context.cacheDir, "gomizplayer.json")
                    f.writeText(app.store.snapshot())
                    val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(share, "Share backup"))
                } catch (e: Exception) {
                    Toasts.show("Backup export failed: ${e.message}", isError = true)
                }
            }) { Text("Export backup") }
            Button(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }) { Text("Import backup") }
        }

        SettingsSectionTitle("Startup cleanup")
        SettingsToggle("Clear source cache on launch", s.clearSourceCacheOnLaunch) { app.store.patchSettings(s.copy(clearSourceCacheOnLaunch = it)) }
        SettingsToggle("Clear watch history on launch", s.clearWatchHistoryOnLaunch) { app.store.patchSettings(s.copy(clearWatchHistoryOnLaunch = it)) }
        SettingsToggle("Clear network cache on launch", s.clearNetworkCacheOnLaunch) { app.store.patchSettings(s.copy(clearNetworkCacheOnLaunch = it)) }

        SettingsSectionTitle("Clear")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pendingClear = "cache" }) { Text("Clear cache") }
            Button(onClick = { pendingClear = "history" }) { Text("Clear history") }
            Button(onClick = { pendingClear = "favorites" }) { Text("Clear favorites") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pendingClear = "search" }) { Text("Clear search") }
            Button(onClick = { pendingClear = "all" }) { Text("Clear all") }
        }

        SettingsSectionTitle("Reset")
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color(0xCCFF3D5A))
                .clickable { pendingReset = true }
                .focusable()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text("Reset settings", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(48.dp))
    }

    pendingClear?.let { kind ->
        com.gomizplayer.iptv.ui.modals.ConfirmDialog(
            title = "Clear data",
            message = when (kind) {
                "cache" -> "Clear cached data?"
                "history" -> "Clear watch history (all profiles)?"
                "favorites" -> "Clear favorites (current profile)?"
                "search" -> "Clear search history (current profile)?"
                else -> "Clear favorites, history and searches (current profile)?"
            },
            confirmLabel = "Clear",
            danger = true,
            onConfirm = {
                when (kind) {
                    "cache" -> {
                        val cleared = context.cacheDir?.deleteRecursively() ?: false
                        Toasts.show(if (cleared) "Cache cleared" else "Cache clear failed", isError = !cleared)
                    }
                    "history" -> { app.store.clearHistory(); Toasts.show("History cleared") }
                    "favorites" -> { app.store.clearFavorites(); Toasts.show("Favorites cleared") }
                    "search" -> { app.store.clearSearches(); Toasts.show("Search history cleared") }
                    "all" -> { app.store.clearAllLibrary(); Toasts.show("All library data cleared") }
                }
                pendingClear = null
            },
            onDismiss = { pendingClear = null }
        )
    }

    if (pendingReset) {
        com.gomizplayer.iptv.ui.modals.ConfirmDialog(
            title = "Reset settings",
            message = "Reset all settings to defaults? Your sources, favorites, history and profiles are kept.",
            confirmLabel = "Reset",
            danger = true,
            onConfirm = {
                app.store.resetSettings()
                pendingReset = false
                Toasts.show("Settings reset")
            },
            onDismiss = { pendingReset = false }
        )
    }
}

// ===================== Widgets (top strip settings — Windows v1.0.2 parity) =====================

@Composable
private fun WidgetsPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val s = data.settings
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("Widgets", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Top strip: clock, weather and prayer times.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("Visible widgets")
        SettingsToggle("Clock", s.widgetClock) { app.store.patchSettings(s.copy(widgetClock = it)) }
        SettingsToggle("Weather", s.widgetWeather) { app.store.patchSettings(s.copy(widgetWeather = it)) }
        SettingsToggle("Prayer times", s.widgetPrayer) { app.store.patchSettings(s.copy(widgetPrayer = it)) }

        SettingsSectionTitle("Location")
        OutlinedTextField(
            value = s.weatherCity,
            onValueChange = { app.store.patchSettings(s.copy(weatherCity = it)) },
            label = { Text("City (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { app.scope.launch { app.widget.refresh(force = true) }; Toasts.show("Refreshing…") }) { Text("Refresh") }
            TextButton(onClick = { app.store.patchSettings(s.copy(weatherCity = "", weatherLat = 0.0, weatherLon = 0.0, weatherCountry = "")) }) { Text("Use auto") }
        }

        SettingsSectionTitle("Units")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(label = "°C", selected = s.tempUnit == "C", onClick = { app.store.patchSettings(s.copy(tempUnit = "C")) })
            SettingsChip(label = "°F", selected = s.tempUnit == "F", onClick = { app.store.patchSettings(s.copy(tempUnit = "F")) })
            SettingsChip(label = "24h", selected = s.timeFormat == "24", onClick = { app.store.patchSettings(s.copy(timeFormat = "24")) })
            SettingsChip(label = "12h", selected = s.timeFormat == "12", onClick = { app.store.patchSettings(s.copy(timeFormat = "12")) })
        }

        SettingsSectionTitle("Prayer method")
        val methods = listOf(2 to "ISNA", 3 to "MWL", 4 to "Umm Al-Qura", 5 to "Egyptian", 1 to "Karachi", 12 to "France", 13 to "Turkey", 8 to "Gulf", 9 to "Kuwait", 10 to "Qatar", 11 to "Singapore")
        FlowRowWrap { methods.forEach { (v, name) -> SettingsChip(label = name, selected = s.prayerMethod == v, onClick = { app.store.patchSettings(s.copy(prayerMethod = v)) }) } }

        SettingsSectionTitle("Prayer school")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(label = "Standard", selected = s.prayerSchool == 0, onClick = { app.store.patchSettings(s.copy(prayerSchool = 0)) })
            SettingsChip(label = "Hanafi", selected = s.prayerSchool == 1, onClick = { app.store.patchSettings(s.copy(prayerSchool = 1)) })
        }
        Spacer(Modifier.height(48.dp))
    }
}

// ===================== Cleanup (Windows v1.0.2 parity) =====================

@Composable
private fun CleanupPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val s = data.settings
    val context = LocalContext.current
    // Focus the first control when this panel opens.
    val firstControlFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstControlFocus.requestFocus() } }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("Cleanup", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Startup cleanup and one-time clear actions.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("On startup")
        CompositionLocalProvider(LocalFocusFirst provides firstControlFocus) {
            SettingsToggle("Clear source cache on launch", s.clearSourceCacheOnLaunch) { app.store.patchSettings(s.copy(clearSourceCacheOnLaunch = it)) }
        }
        SettingsToggle("Clear history on launch", s.clearWatchHistoryOnLaunch) { app.store.patchSettings(s.copy(clearWatchHistoryOnLaunch = it)) }
        SettingsToggle("Clear network cache on launch", s.clearNetworkCacheOnLaunch) { app.store.patchSettings(s.copy(clearNetworkCacheOnLaunch = it)) }

        SettingsSectionTitle("Clear now")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { val ok = context.cacheDir.deleteRecursively(); Toasts.show(if (ok) "Cache cleared" else "Failed", isError = !ok) }) { Text("Cache") }
            Button(onClick = { app.store.clearHistory(); Toasts.show("History cleared") }) { Text("History") }
            Button(onClick = { app.store.clearFavorites(); Toasts.show("Favorites cleared") }) { Text("Favorites") }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { app.store.clearSearches(); Toasts.show("Search cleared") }) { Text("Search") }
            Button(onClick = { app.store.clearAllLibrary(); Toasts.show("All data cleared") }) { Text("All") }
        }
        Spacer(Modifier.height(48.dp))
    }
}

// ===================== System =====================

@Composable
private fun SystemPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData, activity: android.app.Activity?, onNavigate: (String) -> Unit) {
    val colors = LocalGomizColors.current
    val s = data.settings
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("System", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("User agent, proxy, app lock, diagnostics and backups.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        SettingsSectionTitle("Network")
        Text("User agent", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = s.userAgent,
            onValueChange = { app.store.patchSettings(s.copy(userAgent = it)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingsToggle("Proxy", s.proxyEnabled) { app.store.patchSettings(s.copy(proxyEnabled = it)) }
        }
        if (s.proxyEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = s.proxyHost,
                    onValueChange = { app.store.patchSettings(s.copy(proxyHost = it)) },
                    label = { Text("Host") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                        focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
                    )
                )
                OutlinedTextField(
                    value = s.proxyPort.toString(),
                    onValueChange = { it.toIntOrNull()?.let { p -> app.store.patchSettings(s.copy(proxyPort = p)) } },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.width(100.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                        focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
                    )
                )
            }
        }

        SettingsSectionTitle("Privacy")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggle("App lock (PIN)", s.appLockEnabled) { app.store.patchSettings(s.copy(appLockEnabled = it)) }
            SettingsToggle("Telemetry", s.telemetryEnabled) { app.store.patchSettings(s.copy(telemetryEnabled = it)) }
        }
        Text("App lock requires a PIN; uses BiometricPrompt when available.", color = colors.dim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        var showPinDialog by remember { mutableStateOf(false) }
        var pinInput by remember { mutableStateOf("") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showPinDialog = true }) { Text(if (com.gomizplayer.iptv.core.secrets.PinStore.hasPin(context)) "Change PIN" else "Set PIN") }
            if (com.gomizplayer.iptv.core.secrets.PinStore.hasPin(context)) {
                TextButton(onClick = {
                    com.gomizplayer.iptv.core.secrets.PinStore.removePin(context)
                    app.store.patchSettings(s.copy(appLockEnabled = false))
                    Toasts.show("PIN removed")
                }) { Text("Remove PIN", color = androidx.compose.ui.graphics.Color(0xFFFF8A8A)) }
            }
        }
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false; pinInput = "" },
                title = { Text("Set 4-digit PIN") },
                text = {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("PIN") },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pinInput.length == 4) {
                            com.gomizplayer.iptv.core.secrets.PinStore.setPin(context, pinInput)
                            app.store.patchSettings(s.copy(appLockEnabled = true))
                            Toasts.show("PIN set")
                            showPinDialog = false
                            pinInput = ""
                        } else {
                            Toasts.show("PIN must be 4 digits", isError = true)
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false; pinInput = "" }) { Text("Cancel") }
                }
            )
        }

        SettingsSectionTitle("Diagnostics")
        Text("Export a diagnostic bundle (app info, settings, sources) as a ZIP.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            try {
                val zip = java.io.File(context.cacheDir, "gomiz-diagnostics.zip")
                java.util.zip.ZipOutputStream(java.io.FileOutputStream(zip)).use { zos ->
                    val entry = java.util.zip.ZipEntry("settings.json")
                    zos.putNextEntry(entry)
                    zos.write(app.store.snapshot().toByteArray())
                    zos.closeEntry()
                    val info = java.util.zip.ZipEntry("app-info.txt")
                    zos.putNextEntry(info)
                    zos.write("GomizPlayer IPTV\nversion=1.0.2\nsdk=${android.os.Build.VERSION.SDK_INT}\n".toByteArray())
                    zos.closeEntry()
                }
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip))
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(share, "Share diagnostics"))
            } catch (e: Exception) {
                Toasts.show("Diagnostics export failed: ${e.message}", isError = true)
            }
        }) { Text("Export diagnostics") }

        SettingsSectionTitle("Backup and Restore")
        Text("Export/import all data as gomizplayer.json.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    val f = java.io.File(context.cacheDir, "gomizplayer.json")
                    f.writeText(app.store.snapshot())
                    val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(share, "Share backup"))
                } catch (e: Exception) {
                    Toasts.show("Backup export failed: ${e.message}", isError = true)
                }
            }) { Text("Export backup") }
            val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        val input = context.contentResolver.openInputStream(uri)
                        val text = input?.bufferedReader()?.readText() ?: ""
                        input?.close()
                        // Validate by decoding
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(
                            com.gomizplayer.iptv.core.model.AppData.serializer(), text
                        )
                        // If valid, overwrite store file by re-persisting (best-effort: reload not supported, so inform user)
                        Toasts.show("Backup validated. Restart app to apply (auto-restore in next update).")
                    } catch (e: Exception) {
                        Toasts.show("Import failed: ${e.message}", isError = true)
                    }
                }
            }
            Button(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }) { Text("Import backup") }
        }

        SettingsSectionTitle("Manage")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onNavigate("profiles") }) { Text("Profiles", color = colors.accent) }
            TextButton(onClick = { onNavigate("sources") }) { Text("Sources", color = colors.accent) }
            TextButton(onClick = { onNavigate("epg") }) { Text("EPG", color = colors.accent) }
        }
    }
}

// ===================== About =====================

@Composable
    private fun AboutPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "1.0.4"
    val versionCode = packageInfo?.let {
        if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
    } ?: 104L
    val colors = LocalGomizColors.current

    fun openUrl(url: String) {
        try {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (_: Exception) {
            Toasts.show("No app to open link", isError = true)
        }
    }

    fun openEmail() {
        try {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:radhouanemokrane.contact@gmail.com")))
        } catch (_: Exception) {
            Toasts.show("No mail app found", isError = true)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("About GomizPlayer IPTV", color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("GomizPlayer IPTV — Live TV, movies and series player for Android TV and phones. A native Android companion to the Windows app.", color = colors.muted, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text("Developer: Radhouane Mokrane", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Text("Version $versionName ($versionCode)", color = colors.dim, fontSize = 12.sp)

        Spacer(Modifier.height(16.dp))
        SettingsSectionTitle("Contact & Links")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.panel2)
            .clickable { openUrl("https://www.facebook.com/profile.php?id=61595630273747") }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 14.dp)) {
            androidx.compose.material3.Icon(painterResource(R.drawable.ic_facebook), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Facebook", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Follow on Facebook", color = colors.dim, fontSize = 10.sp)
            }
            Text("→", color = colors.muted, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.panel2)
            .clickable { openEmail() }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 14.dp)) {
            androidx.compose.material3.Icon(painterResource(R.drawable.ic_mail), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Email", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("radhouanemokrane.contact@gmail.com", color = colors.dim, fontSize = 10.sp)
            }
            Text("→", color = colors.muted, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.panel2)
            .clickable { openUrl("https://github.com/radhouanemokranecontact-tech/GomizPlayer-IPTV") }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 14.dp)) {
            androidx.compose.material3.Icon(painterResource(R.drawable.ic_github), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("GitHub", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Source code & releases", color = colors.dim, fontSize = 10.sp)
            }
            Text("→", color = colors.muted, fontSize = 18.sp)
        }
        Spacer(Modifier.height(48.dp))
    }
}

// ===================== Sources =====================

@Composable
fun SourcesPanel(
    app: com.gomizplayer.iptv.GomizApp,
    data: com.gomizplayer.iptv.core.model.AppData,
    onExit: () -> Unit = {},
    entryFocus: androidx.compose.ui.focus.FocusRequester? = null
) {
    var sub by remember { mutableStateOf("list") }
    var editing by remember { mutableStateOf<com.gomizplayer.iptv.core.model.SourceEntry?>(null) }
    when (sub) {
        "list" -> SourcesSection(
            onBack = onExit,
            onAdd = { editing = null; sub = "add" },
            onEdit = { editing = it; sub = "add" },
            entryFocus = entryFocus
        )
        "add" -> AddSourceScreen(onBack = { sub = "list" }, editSource = editing)
    }
}
// ===================== Profiles (reused) =====================

@Composable
private fun ProfilesPanel(app: com.gomizplayer.iptv.GomizApp, data: com.gomizplayer.iptv.core.model.AppData) {
    val colors = LocalGomizColors.current
    val current = data.settings.profile
    val profiles = data.library.keys.toList().ifEmpty { listOf("default") }
    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text("Profiles", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(profiles.size) { index ->
                val p = profiles[index]
                val active = p == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.panel2)
                        .clickable { if (!active) app.store.switchProfile(p) }
                        .focusable()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(if (active) "âœ“" else "â—‹", fontSize = 22.sp, modifier = Modifier.width(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (active) Text("Active", color = colors.accent, fontSize = 12.sp)
                    }
                    if (p != "default") {
                        TextButton(onClick = { pendingDelete = p }) {
                            Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFFF8A8A), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                placeholder = { Text("New profile name") },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line
                )
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = {
                val name = newName.trim()
                if (name.isNotEmpty()) {
                    app.store.createProfile(name)
                    app.store.switchProfile(name)
                    newName = ""
                }
            }) { Text("Create profile") }
        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete profile") },
            text = { Text("Delete profile \"$p\"? Its favorites and history will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    app.store.deleteProfile(p)
                    pendingDelete = null
                }) { Text("Yes", color = androidx.compose.ui.graphics.Color(0xFFFF8A8A)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("No") }
            }
        )
    }
}
@Composable
fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalGomizColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.panel2)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = colors.muted, fontSize = 12.sp) }
        }
        trailing?.let { Text(it, color = colors.accent, fontSize = 18.sp) }
    }
}

@Composable
fun SectionHeader(title: String, onBack: () -> Unit) {
    val colors = LocalGomizColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("←  " + androidx.compose.ui.res.stringResource(com.gomizplayer.iptv.R.string.back), fontSize = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(title, color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FlowRowWrap(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}