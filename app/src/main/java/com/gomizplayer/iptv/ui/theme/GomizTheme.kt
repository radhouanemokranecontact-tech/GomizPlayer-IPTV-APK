package com.gomizplayer.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Exact port of the Windows app (styles.css :root) tokens.
 * Dark = default. Light = data-theme="light" values.
 */
@Immutable
data class GomizColors(
    val isDark: Boolean,
    val bg: Color,
    val bgSoft: Color,
    val panel: Color,
    val panel2: Color,
    val panel3: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val accent: Color,
    val accent2: Color,
    val accentSoft: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color,
    val sidebarStart: Color,
    val sidebarEnd: Color,
    val sidebarGlow: Color,
    val mainGlowA: Color,
    val mainGlowB: Color,
    val topbarBg: Color,
    val heroFallbackStart: Color,
    val heroFallbackEnd: Color
) {
    /** Red/orange brand gradient used by primary buttons and brand mark. */
    val accentBrush: Brush
        get() = Brush.linearGradient(listOf(accent, accent2))

    /** Radial glows behind the main area (.main in styles.css). */
    val mainBackground: Brush
        get() = Brush.radialGradient(
            0.0f to mainGlowA.copy(alpha = 0.32f),
            0.28f to Color.Transparent,
            1.0f to Color.Transparent,
            center = Offset(0.72f, -0.08f),
            radius = 0.9f
        )

    val mainBackgroundSecondary: Brush
        get() = Brush.radialGradient(
            0.0f to mainGlowB.copy(alpha = 0.28f),
            0.25f to Color.Transparent,
            1.0f to Color.Transparent,
            center = Offset(0.16f, -0.05f),
            radius = 0.75f
        )

    val sidebarBackground: Brush
        get() = Brush.linearGradient(
            0.0f to sidebarStart,
            1.0f to sidebarEnd
        )
}

private fun darkColors() = GomizColors(
    isDark = true,
    bg = Color(0xFF080A0F),
    bgSoft = Color(0xFF0D1017),
    panel = Color(0xFF12161F),
    panel2 = Color(0xFF171C27),
    panel3 = Color(0xFF1E2432),
    line = Color(0x14FFFFFF),
    lineStrong = Color(0x24FFFFFF),
    text = Color(0xFFEEF1F7),
    muted = Color(0xFF9AA3B5),
    dim = Color(0xFF6C7488),
    accent = Color(0xFFFF3D5A),
    accent2 = Color(0xFFFF7A45),
    accentSoft = Color(0x24FF3D5A),
    ok = Color(0xFF38D39F),
    warn = Color(0xFFFFC857),
    danger = Color(0xFFFF5D6C),
    sidebarStart = Color(0xFF0C0F16),
    sidebarEnd = Color(0xFF090B11),
    sidebarGlow = Color(0x21FF3D5A),
    mainGlowA = Color(0xFF5A6CFF),
    mainGlowB = Color(0xFFFF3D5A),
    topbarBg = Color(0xB8080A0F),
    heroFallbackStart = Color(0xFF243049),
    heroFallbackEnd = Color(0xFF0E1320)
)

private fun lightColors() = GomizColors(
    isDark = false,
    bg = Color(0xFFF4F6FB),
    bgSoft = Color(0xFFFFFFFF),
    panel = Color(0xFFFFFFFF),
    panel2 = Color(0xFFF1F3F9),
    panel3 = Color(0xFFE7EAF3),
    line = Color(0x17100E19),
    lineStrong = Color(0x29100E19),
    text = Color(0xFF10141D),
    muted = Color(0xFF5B6478),
    dim = Color(0xFF7B8497),
    accent = Color(0xFFFF3D5A),
    accent2 = Color(0xFFFF7A45),
    accentSoft = Color(0x1AFF3D5A),
    ok = Color(0xFF0F9F6E),
    warn = Color(0xFFC77B00),
    danger = Color(0xFFE02437),
    sidebarStart = Color(0xFFFFFFFF),
    sidebarEnd = Color(0xFFF7F8FC),
    sidebarGlow = Color(0x14FF3D5A),
    mainGlowA = Color(0xFF5A6CFF),
    mainGlowB = Color(0xFFFF3D5A),
    topbarBg = Color(0xCCFFFFFF),
    heroFallbackStart = Color(0xFF243049),
    heroFallbackEnd = Color(0xFF0E1320)
)

val LocalGomizColors = staticCompositionLocalOf { darkColors() }

// ---- Legacy aliases used by pre-UI-1 screens (kept until full migration) ----
val Accent = Color(0xFFFF3D5A)
val Accent2 = Color(0xFFFF7A45)
val Background = Color(0xFF0E1320)
val SurfaceDark = Color(0xFF161C2C)
val OnSurface = Color(0xFFF2F4F8)
val OnSurfaceMuted = Color(0xFF9AA6B8)

fun parseAccent(hex: String?): Color = try {
    Color(android.graphics.Color.parseColor(hex ?: "#FF3D5A"))
} catch (_: Exception) {
    Color(0xFFFF3D5A)
}

@Composable
fun GomizTheme(
    darkTheme: Boolean,
    accent: Color = Color(0xFFFF3D5A),
    accent2: Color = Color(0xFFFF7A45),
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) darkColors() else lightColors()
    val colors = base.copy(accent = accent, accent2 = accent2, accentSoft = accent.copy(alpha = 0.14f))
    val tvScheme = if (darkTheme) {
        androidx.tv.material3.darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            secondary = colors.accent2,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.panel,
            onSurface = colors.text,
            surfaceVariant = colors.panel3,
            onSurfaceVariant = colors.muted
        )
    } else {
        androidx.tv.material3.lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            secondary = colors.accent2,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.panel,
            onSurface = colors.text,
            surfaceVariant = colors.panel2,
            onSurfaceVariant = colors.muted
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalGomizColors provides colors) {
        androidx.tv.material3.MaterialTheme(colorScheme = tvScheme) {
            content()
        }
    }
}
