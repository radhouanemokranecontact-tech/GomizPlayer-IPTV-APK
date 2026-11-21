package com.gomizplayer.iptv.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gomizplayer.iptv.R
import com.gomizplayer.iptv.gomiz
import com.gomizplayer.iptv.data.weatherEmoji
import com.gomizplayer.iptv.ui.theme.LocalGomizColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeWidgetsRow(modifier: Modifier = Modifier, compact: Boolean = false) {
    val figureSize = if (compact) 18.sp else 24.sp
    val context = LocalContext.current
    val app = gomiz(context)
    val data by app.store.data.collectAsState()
    val s = data.settings
    if (!s.widgetClock && !s.widgetWeather && !s.widgetPrayer) return

    val widgetState by app.widget.state.collectAsState()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val is24 = s.timeFormat != "12"
    val locale = Locale.getDefault()
    val timeFmt = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm:ss" else "hh:mm:ss a", locale) }
    // Dynamic date: always include the current year (from the real clock, never hardcoded).
    val dateFmt = remember(locale) { SimpleDateFormat("EEE d MMM yyyy", locale) }

    val nowMinutes = remember(nowMs) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
    val nextPrayer = remember(widgetState.prayers, nowMinutes) {
        app.widget.nextPrayer(nowMinutes)
    }
    val nextCountdown = remember(nextPrayer, nowMs) {
        nextPrayer?.let { (_, time) ->
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val nowSec = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
            val t = app.widget.toMinutes(time) ?: return@remember null
            var diff = t * 60 - nowSec
            if (diff < 0) diff += 24 * 3600
            String.format("%02d:%02d:%02d", diff / 3600, (diff % 3600) / 60, diff % 60)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (s.widgetClock) {
            WidgetCard(tag = stringResource(R.string.widget_time), dotColor = androidx.compose.ui.graphics.Color(0xFF4DA3FF), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_clock_new),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = timeFmt.format(Date(nowMs)),
                            color = LocalGomizColors.current.text,
                            fontSize = figureSize,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = dateFmt.format(Date(nowMs)),
                            color = LocalGomizColors.current.muted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        if (s.widgetWeather) {
            val tempC = widgetState.tempC
            val displayTemp = if (tempC == null) "—" else {
                if (s.tempUnit == "F") Math.round(tempC * 9 / 5 + 32).toString() + "°F"
                else Math.round(tempC).toString() + "°C"
            }
            val sub = when {
                tempC == null && widgetState.error == "no_location" -> stringResource(R.string.location_unavailable)
                tempC == null -> stringResource(R.string.loading)
                else -> listOfNotNull(
                    widgetState.city.takeIf { it.isNotBlank() },
                    widgetState.desc.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
            }
            WidgetCard(tag = stringResource(R.string.widget_weather), dotColor = androidx.compose.ui.graphics.Color(0xFFFFB020), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = weatherEmoji(widgetState.weatherCode, widgetState.isDay), fontSize = 34.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = displayTemp,
                            color = LocalGomizColors.current.text,
                            fontSize = figureSize,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = sub,
                            color = LocalGomizColors.current.muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (s.widgetPrayer) {
            val hasPrayers = widgetState.prayers.isNotEmpty()
            val nextTime = nextPrayer?.second ?: "—"
            val nextNameLabel = nextPrayer?.first?.let { prayerLabel(it) } ?: "—"
            WidgetCard(tag = stringResource(R.string.widget_prayer), dotColor = androidx.compose.ui.graphics.Color(0xFF38D39F), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_mosque),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = nextTime,
                            color = LocalGomizColors.current.text,
                            fontSize = figureSize,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (hasPrayers) {
                                nextNameLabel + (nextCountdown?.let { " · " + it } ?: "")
                            } else stringResource(R.string.loading),
                            color = LocalGomizColors.current.muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.WidgetCard(
    tag: String,
    dotColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val colors = LocalGomizColors.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.panel)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor.copy(alpha = dotAlpha))
            )
            Text(
                text = tag.uppercase(),
                color = colors.text,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp
            )
        }
        Spacer(Modifier.size(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

private fun prayerLabel(key: String): String = when (key) {
    "fajr" -> "Fajr"
    "sunrise" -> "Sunrise"
    "dhuhr" -> "Dhuhr"
    "asr" -> "Asr"
    "maghrib" -> "Maghrib"
    "isha" -> "Isha"
    else -> key
}

