package com.gomizplayer.iptv.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gomizplayer.iptv.ui.theme.LocalGomizColors

/**
 * Unified Android TV focus indicator: rounded accent outline + subtle tinted
 * background, animated smoothly. Used by every interactive component so the
 * look and behaviour stay consistent (sidebar, settings, cards, dialogs, player).
 */
object TvFocusStyle {
    /** Focus background alpha — tuned visually on Google TV emulator. */
    const val BACKGROUND_ALPHA = 0.30f
    const val BORDER_ALPHA = 1.0f
}

@Composable
fun Modifier.tvFocusRing(
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    enabled: Boolean = true,
    borderWidth: Dp = 2.dp,
    onFocus: ((Boolean) -> Unit)? = null
): Modifier {
    val colors = LocalGomizColors.current
    var focused by remember { mutableStateOf(false) }
    val target = if (enabled && focused) 1f else 0f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(120),
        label = "tvFocusRing"
    )
    return this
        .onFocusChanged { value ->
            focused = value.isFocused
            onFocus?.invoke(value.isFocused)
        }
        .clip(shape)
        .background(colors.accent.copy(alpha = TvFocusStyle.BACKGROUND_ALPHA * progress))
        .border(
            width = if (progress > 0f) borderWidth * progress else 0.dp,
            color = colors.accent.copy(alpha = TvFocusStyle.BORDER_ALPHA * progress),
            shape = shape
        )
}

/** In-memory focus memory — restores the last focused key per screen without touching navigation logic. */
object TvFocusMemory {
    private val lastKey = mutableMapOf<String, String>()

    fun wasLast(screen: String, key: String): Boolean = lastKey[screen] == key
    fun save(screen: String, key: String) { lastKey[screen] = key }
}

/**
 * FocusRequester that auto-restores focus on first composition when [key] was
 * the last focused element of [screen].
 */
@Composable
fun rememberFocusRequester(screen: String, key: String): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (TvFocusMemory.wasLast(screen, key)) runCatching { requester.requestFocus() }
    }
    return requester
}
